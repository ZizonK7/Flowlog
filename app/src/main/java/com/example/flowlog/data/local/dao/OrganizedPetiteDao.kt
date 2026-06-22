package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizedPetiteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OrganizedPetiteEntity>)

    @Query("DELETE FROM organized_petites WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("""
        SELECT * FROM organized_petites
        WHERE userId = :userId
          AND isDismissed = 0
          AND isCompleted = 0
        ORDER BY rank ASC, priorityScore ASC, title ASC
    """)
    fun observeActive(userId: String): Flow<List<OrganizedPetiteEntity>>

    @Query("""
        SELECT * FROM organized_petites
        WHERE userId = :userId
          AND isDismissed = 1
        ORDER BY updatedAt DESC
    """)
    suspend fun getDismissed(userId: String): List<OrganizedPetiteEntity>

    @Query("""
        UPDATE organized_petites
        SET isDismissed = 1,
            updatedAt = :updatedAt
        WHERE userId = :userId
          AND sourceType = :sourceType
          AND IFNULL(sourceId, '') = IFNULL(:sourceId, '')
    """)
    suspend fun dismissBySource(userId: String, sourceType: String, sourceId: String?, updatedAt: Long)

    // TODO·PETITE 두 sourceType이 동일 todoId로 공존할 수 있으므로 한 번에 모두 dismiss.
    @Query("""
        UPDATE organized_petites
        SET isDismissed = 1,
            updatedAt = :updatedAt
        WHERE userId = :userId
          AND sourceType IN ('TODO', 'PETITE')
          AND IFNULL(sourceId, '') = :sourceId
    """)
    suspend fun dismissTodoPetitesBySourceId(userId: String, sourceId: String, updatedAt: Long)

    @Query("""
        UPDATE organized_petites
        SET isDismissed = 0,
            updatedAt = :updatedAt
        WHERE userId = :userId
          AND sourceType = :sourceType
          AND IFNULL(sourceId, '') = IFNULL(:sourceId, '')
    """)
    suspend fun restoreBySource(userId: String, sourceType: String, sourceId: String?, updatedAt: Long)

    @Transaction
    suspend fun replaceAllForUser(userId: String, items: List<OrganizedPetiteEntity>) {
        deleteAllForUser(userId)
        if (items.isNotEmpty()) insertAll(items)
    }

    // CALENDAR sourceType은 calendar pull이 독립적으로 관리하므로 삭제 대상에서 제외한다.
    @Query("DELETE FROM organized_petites WHERE userId = :userId AND sourceType != 'CALENDAR'")
    suspend fun deleteNonCalendarForUser(userId: String)

    @Transaction
    suspend fun replaceNonCalendarForUser(userId: String, items: List<OrganizedPetiteEntity>) {
        deleteNonCalendarForUser(userId)
        if (items.isNotEmpty()) insertAll(items)
    }

    @Query("""
        SELECT * FROM organized_petites
        WHERE userId = :userId
          AND sourceType = :sourceType
          AND IFNULL(sourceId, '') = IFNULL(:sourceId, '')
        LIMIT 1
    """)
    suspend fun getBySource(userId: String, sourceType: String, sourceId: String?): OrganizedPetiteEntity?

    @Query("""
        UPDATE organized_petites SET
            title = :title,
            category = :category,
            dateMillis = :dateMillis,
            estimatedMinutes = :estimatedMinutes,
            aiComment = :aiComment,
            rank = :rank,
            priorityScore = :priorityScore,
            activityCategory = :activityCategory,
            calendarTaskType = :calendarTaskType,
            autoStartEnabled = :autoStartEnabled,
            autoStartTime24 = :autoStartTime24,
            autoStartEndTime24 = :autoStartEndTime24,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateCalendarPetiteContent(
        id: String,
        title: String,
        category: String?,
        dateMillis: Long?,
        estimatedMinutes: Int?,
        aiComment: String?,
        rank: Int,
        priorityScore: Int,
        activityCategory: String?,
        calendarTaskType: String?,
        autoStartEnabled: Boolean,
        autoStartTime24: String,
        autoStartEndTime24: String,
        updatedAt: Long
    )

    @Query("""
        SELECT * FROM organized_petites
        WHERE userId = :userId
          AND sourceType = 'CALENDAR'
          AND autoStartEnabled = 1
          AND autoStartTime24 != ''
          AND autoStartEndTime24 != ''
          AND isDismissed = 0
          AND isCompleted = 0
          AND dateMillis = :todayDateKey
    """)
    fun observeTodayCalendarAutoStartPetites(userId: String, todayDateKey: Long): Flow<List<OrganizedPetiteEntity>>

    @Query("SELECT * FROM organized_petites WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OrganizedPetiteEntity?

    @Query("""
        UPDATE organized_petites SET
            autoStartTime24 = :startTime24,
            autoStartEndTime24 = :endTime24,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateCalendarPetiteAutoStartTimes(
        id: String,
        startTime24: String,
        endTime24: String,
        updatedAt: Long
    )

    @Query("UPDATE organized_petites SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, updatedAt: Long)

    @Query("UPDATE organized_petites SET isCompleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markCompletedById(id: String, updatedAt: Long)

    @Query("""
        UPDATE organized_petites
        SET isCompleted = 0,
            isDismissed = 0,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun reopenById(id: String, updatedAt: Long)

    /**
     * sourceType/sourceId 조합이 없을 때만 삽입.
     * 기존 row가 있으면 (dismissed/completed 포함) 아무것도 하지 않는다 — 사용자 상태 보존.
     */
    @Transaction
    suspend fun insertPetiteIfAbsent(entity: OrganizedPetiteEntity) {
        val existing = getBySource(entity.userId, entity.sourceType, entity.sourceId)
        if (existing == null) {
            insertAll(listOf(entity))
        }
        // existing row (dismissed/completed 포함)가 있으면 아무것도 하지 않음 — 사용자 상태 보존
    }

    /**
     * CALENDAR sourceType 전용 upsert.
     * 기존 row가 있으면 content 필드만 덮어쓰고, isDismissed·isCompleted는 보존한다.
     * 기존 row가 없으면 새로 삽입한다.
     */
    @Transaction
    suspend fun upsertCalendarPetitePreservingUserState(entity: OrganizedPetiteEntity) {
        val existing = getBySource(entity.userId, entity.sourceType, entity.sourceId)
        if (existing == null) {
            insertAll(listOf(entity))
        } else {
            updateCalendarPetiteContent(
                id = existing.id,
                title = entity.title,
                category = entity.category,
                dateMillis = entity.dateMillis,
                estimatedMinutes = entity.estimatedMinutes,
                aiComment = entity.aiComment,
                rank = entity.rank,
                priorityScore = entity.priorityScore,
                activityCategory = entity.activityCategory,
                calendarTaskType = entity.calendarTaskType,
                autoStartEnabled = entity.autoStartEnabled,
                autoStartTime24 = entity.autoStartTime24,
                autoStartEndTime24 = entity.autoStartEndTime24,
                updatedAt = entity.updatedAt
            )
        }
    }
}
