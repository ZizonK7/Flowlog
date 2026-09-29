package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.entity.ActivityEntity
import com.example.flowlog.data.local.entity.ActivityConflictEntity
import kotlinx.coroutines.flow.Flow

/**
 * Activity revision 동기화 전용 DAO (v24).
 * remoteRevision = 마지막으로 확인된 원격 revision (문서/필드 없음 = 0).
 * updatedAt 은 로컬 수정마다 반드시 증가해야 조건부 ACK 가 편집 유실을 막는다.
 */
@Dao
abstract class ActivityRevisionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveConflict(conflict: ActivityConflictEntity)

    @Query("SELECT * FROM activity_conflicts WHERE userId = :userId")
    abstract fun observeConflicts(userId: String): Flow<List<ActivityConflictEntity>>

    @Query("DELETE FROM activity_conflicts WHERE userId = :userId AND activityId = :activityId")
    abstract suspend fun clearConflict(userId: String, activityId: String)

    @Query("UPDATE activities SET remoteRevision = :revision WHERE userId = :userId AND activityId = :activityId AND updatedAt = :expectedUpdatedAt AND syncStatus = 'PENDING'")
    abstract suspend fun rebaseLocal(userId: String, activityId: String, expectedUpdatedAt: Long, revision: Long): Int

    @Query("SELECT * FROM activities WHERE userId = :userId AND activityId = :activityId LIMIT 1")
    abstract suspend fun getOwnedActivity(userId: String, activityId: String): ActivityEntity?

    /** 회고 분류: 첫 변경 시 originalCategory 에 이전 category 보존. */
    @Query("UPDATE activities SET originalCategory = COALESCE(originalCategory, category), category = :newCategory, updatedAt = MAX(:now, updatedAt + 1), syncStatus = 'PENDING' WHERE userId = :userId AND activityId = :activityId AND isDeleted = 0 AND category != :newCategory")
    abstract suspend fun reclassifyActivity(userId: String, activityId: String, newCategory: String, now: Long): Int

    /** hard delete 대신 tombstone — 다음 sync 에서 원격에도 isDeleted/deletedAt 로 기록. */
    @Query("UPDATE activities SET isDeleted = 1, deletedAt = :now, updatedAt = MAX(:now, updatedAt + 1), syncStatus = 'PENDING' WHERE userId = :userId AND activityId = :activityId AND isDeleted = 0")
    abstract suspend fun markActivityDeleted(userId: String, activityId: String, now: Long): Int

    @Query("UPDATE activities SET syncStatus = 'SYNCED', remoteRevision = :revision WHERE userId = :userId AND activityId = :activityId AND updatedAt = :uploadedUpdatedAt AND syncStatus = 'PENDING'")
    abstract suspend fun markSyncedIfUnchanged(userId: String, activityId: String, uploadedUpdatedAt: Long, revision: Long): Int

    @Query("UPDATE activities SET remoteRevision = :revision WHERE userId = :userId AND activityId = :activityId AND remoteRevision < :revision")
    abstract suspend fun advanceRemoteRevision(userId: String, activityId: String, revision: Long): Int

    /**
     * 업로드 ACK. 업로드 중 로컬 수정(updatedAt 변경)이 있었다면 PENDING 유지하고 remoteRevision 만 전진 —
     * 다음 업로드가 자기 자신의 커밋과 충돌하지 않게 한다.
     */
    @Transaction
    open suspend fun ackActivityUpload(userId: String, activityId: String, uploadedUpdatedAt: Long, revision: Long): Boolean {
        if (markSyncedIfUnchanged(userId, activityId, uploadedUpdatedAt, revision) > 0) return true
        advanceRemoteRevision(userId, activityId, revision)
        return false
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(entity: ActivityEntity): Long

    @Update
    abstract suspend fun updateActivityRow(entity: ActivityEntity): Int

    /**
     * 복원 적용: 신규 행 삽입, 또는 SYNCED 이고 원격 revision 이 더 큰 기존 행만 교체.
     * PENDING(미업로드 로컬 수정) 행이나 다른 사용자 행은 건드리지 않고 false.
     */
    @Transaction
    open suspend fun applyRestoredActivity(entity: ActivityEntity, expectedLocalUpdatedAt: Long? = null): Boolean {
        val current = getOwnedActivity(entity.userId, entity.activityId)
            ?: return insertIfAbsent(entity) != -1L
        if (expectedLocalUpdatedAt != null) {
            if (current.updatedAt != expectedLocalUpdatedAt) return false
        } else if (current.syncStatus != SyncStatus.SYNCED || current.remoteRevision >= entity.remoteRevision) return false
        return updateActivityRow(entity) > 0
    }
}
