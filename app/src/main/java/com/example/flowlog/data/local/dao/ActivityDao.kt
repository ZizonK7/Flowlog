package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

// OnConflictStrategy.REPLACE: 동기화 시 서버 데이터로 덮어쓸 수 있고,
// legacyId 기반 마이그레이션 재실행 시에도 안전하게 upsert 처리됨
@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("SELECT * FROM activities WHERE activityId = :activityId LIMIT 1")
    suspend fun getActivityById(activityId: String): ActivityEntity?

    // legacyId(Long) 기반 조회 — SharedPrefs 시절 id로 Room 데이터 접근
    @Query("SELECT * FROM activities WHERE legacyId = :legacyId AND isDeleted = 0 LIMIT 1")
    suspend fun getActivityByLegacyId(legacyId: Long): ActivityEntity?

    // 전체 활동 조회 (통계/분석, getAllActivities 읽기 경로용)
    @Query("""
        SELECT * FROM activities
        WHERE userId = :userId
          AND isDeleted = 0
        ORDER BY startTime DESC
    """)
    fun observeAllActivities(userId: String): Flow<List<ActivityEntity>>

    // 오늘 활동 목록 (삭제된 항목 제외, startTime 기준)
    @Query("""
        SELECT * FROM activities
        WHERE userId = :userId
          AND startTime >= :startOfDay
          AND startTime < :endOfDay
          AND isDeleted = 0
        ORDER BY startTime DESC
    """)
    fun observeTodayActivities(
        userId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ActivityEntity>>

    // 날짜 범위 조회 (통계, 분석용)
    @Query("""
        SELECT * FROM activities
        WHERE userId = :userId
          AND startTime >= :startTime
          AND startTime < :endTime
          AND isDeleted = 0
        ORDER BY startTime DESC
    """)
    fun getActivitiesByDateRange(
        userId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<ActivityEntity>>

    @Query("""
        SELECT * FROM activities
        WHERE userId = :userId
          AND category = :category
          AND isDeleted = 0
        ORDER BY startTime DESC
    """)
    fun getActivitiesByCategory(
        userId: String,
        category: String
    ): Flow<List<ActivityEntity>>

    // Firebase batch sync 대상 조회
    // isDeleted = 1 항목도 포함: 오프라인 삭제가 Firestore에 반영되지 않는 문제 방지.
    // FirebaseSyncDataSource에서 isDeleted 여부를 보고 set/delete 분기 처리.
    @Query("""
        SELECT * FROM activities
        WHERE userId = :userId
          AND syncStatus = '${SyncStatus.PENDING}'
    """)
    suspend fun getUnsyncedActivities(userId: String): List<ActivityEntity>

    @Query("""
        SELECT * FROM activities
        WHERE userId = :userId
          AND syncStatus = '${SyncStatus.PENDING}'
          AND startTime < :cutoffMillis
    """)
    suspend fun getEligibleUnsyncedActivities(userId: String, cutoffMillis: Long): List<ActivityEntity>

    @Query("""
        UPDATE activities
        SET syncStatus = '${SyncStatus.SYNCED}'
        WHERE activityId = :activityId
    """)
    suspend fun markActivitySynced(activityId: String)

    // Soft delete: UI와 통계에서 제외되고, isDeleted=1로만 표시됨
    @Query("""
        UPDATE activities
        SET isDeleted = 1,
            deletedAt = :deletedAt,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE activityId = :activityId
    """)
    suspend fun softDeleteActivity(activityId: String, deletedAt: Long, updatedAt: Long)

    @Query("""
        SELECT COUNT(*) FROM activities
        WHERE userId = :userId
          AND isDeleted = 0
    """)
    suspend fun getActiveActivitiesCount(userId: String): Int

    @Query("UPDATE activities SET userId = :newUserId, syncStatus = 'PENDING' WHERE userId = 'anonymous'")
    suspend fun reassignAnonymousUser(newUserId: String): Int

    // ── 검색 / 필터 (ActivityRepository 읽기 경로) ────────────────────────

    // title, category, note, tagsJson에 대해 대소문자 무관 부분 일치 검색.
    // tagsJson은 JSON 배열 문자열이므로 LIKE 검색 → 태그명 일부 문자열도 매칭될 수 있음.
    // :query가 '%' 또는 '_'를 포함하면 LIKE 와일드카드로 동작하나 개인 앱 수준에서 허용.
    @Query("""
        SELECT * FROM activities
        WHERE userId = :userId
          AND isDeleted = 0
          AND (
            LOWER(title)                        LIKE '%' || LOWER(:query) || '%'
            OR LOWER(category)                  LIKE '%' || LOWER(:query) || '%'
            OR LOWER(COALESCE(note, ''))        LIKE '%' || LOWER(:query) || '%'
            OR LOWER(COALESCE(tagsJson, ''))    LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY startTime DESC
    """)
    suspend fun searchActivities(userId: String, query: String): List<ActivityEntity>

    // category 완전 일치. 기존 getActivitiesByCategory(Flow)와 달리 단건 조회(suspend).
    @Query("""
        SELECT * FROM activities
        WHERE userId = :userId
          AND category = :category
          AND isDeleted = 0
        ORDER BY startTime DESC
    """)
    suspend fun filterByCategory(userId: String, category: String): List<ActivityEntity>

    // tagsJson 내 정확한 tag 검색 (따옴표로 감싸 JSON 원소 경계 확보, 대소문자 무관).
    // 예: tagsJson = '["sport","coding"]', tag = "sport" → '%"sport"%' 매칭 ✓
    // 한계: 태그명 자체에 '"' 문자가 포함되면 오탐 가능 (실제 사용 시 해당 없음).
    @Query("""
        SELECT * FROM activities
        WHERE userId = :userId
          AND isDeleted = 0
          AND LOWER(COALESCE(tagsJson, '')) LIKE '%"' || LOWER(:tag) || '"%'
        ORDER BY startTime DESC
    """)
    suspend fun filterByTag(userId: String, tag: String): List<ActivityEntity>
}
