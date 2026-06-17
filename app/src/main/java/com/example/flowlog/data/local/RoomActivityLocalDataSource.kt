package com.example.flowlog.data.local

import android.content.Context
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.dao.ActivityDao
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.ActivityEntity
import com.example.flowlog.data.local.mapper.toActivityEntity
import com.example.flowlog.data.local.mapper.toActivitySession
import com.example.flowlog.data.model.ActivitySession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room 기반 활동 기록 로컬 데이터 소스. ActivityRepository의 primary local store.
 */
class RoomActivityLocalDataSource(context: Context) {

    private val dao: ActivityDao = FlowlogDatabase.getInstance(context).activityDao()

    /**
     * 활동 저장. 반환값은 Room에서 부여된 activityId (String).
     * legacyId가 없는 신규 데이터는 UUID activityId를 사용.
     */
    suspend fun insert(activity: ActivitySession, userId: String): String {
        val entity = activity.toActivityEntity(userId)
        dao.insertActivity(entity)
        return entity.activityId
    }

    /**
     * 활동 수정.
     * legacyId가 있으면 "legacy_activity_${id}"로 activityId를 유도.
     * id == 0L인 순수 Room 데이터는 아직 지원 안 함 (전환 완료 후 확장).
     */
    suspend fun update(activity: ActivitySession, userId: String) {
        val activityId = if (activity.id != 0L) "legacy_activity_${activity.id}" else return
        val existing = dao.getActivityById(activityId)
        if (existing == null) {
            // 엔티티가 Room에 없으면 (migration 미완료 등) upsert로 삽입
            dao.insertActivity(activity.toActivityEntity(userId))
            return
        }
        dao.updateActivity(
            existing.copy(
                title = activity.title,
                category = activity.category,
                note = activity.note,
                endTime = activity.endTime,
                durationMillis = activity.durationMillis,
                isFavorite = activity.isFavorite,
                tagsJson = activity.toActivityEntity(userId).tagsJson,
                exerciseSetsJson = activity.toActivityEntity(userId).exerciseSetsJson,
                sourceType = activity.sourceType,
                sourceId = activity.sourceId,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
        )
    }

    /**
     * Soft delete. activityId(String) 기반.
     */
    suspend fun softDelete(activityId: String) {
        val now = System.currentTimeMillis()
        dao.softDeleteActivity(activityId, now, now)
    }

    /**
     * legacyId(Long)로 soft delete. 마이그레이션된 데이터 삭제 시 사용.
     */
    suspend fun softDeleteByLegacyId(legacyId: Long) {
        softDelete("legacy_activity_$legacyId")
    }

    suspend fun softDeleteBySourceForDate(
        userId: String,
        sourceType: String,
        sourceId: String,
        startOfDay: Long,
        endOfDay: Long
    ) {
        val now = System.currentTimeMillis()
        dao.softDeleteActivitiesBySourceForDate(
            userId = userId,
            sourceType = sourceType,
            sourceId = sourceId,
            startOfDay = startOfDay,
            endOfDay = endOfDay,
            deletedAt = now,
            updatedAt = now
        )
    }

    suspend fun countBySourceForDate(
        userId: String,
        sourceType: String,
        sourceId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Int {
        return dao.countActivitiesBySourceForDate(
            userId = userId,
            sourceType = sourceType,
            sourceId = sourceId,
            startOfDay = startOfDay,
            endOfDay = endOfDay
        )
    }

    /**
     * 전체 활동 실시간 관찰 (isDeleted = 0). 통계/분석/getAllActivities 읽기 경로용.
     */
    fun observeAllActivities(userId: String): Flow<List<ActivitySession>> {
        return dao.observeAllActivities(userId)
            .map { entities -> entities.map { it.toActivitySession() } }
    }

    /**
     * legacyId(Long)로 단건 조회. getActivityById(Long) 읽기 경로용.
     */
    suspend fun getActivityByLegacyId(legacyId: Long): ActivitySession? {
        return dao.getActivityByLegacyId(legacyId)?.toActivitySession()
    }

    /**
     * 오늘 활동 실시간 관찰. isDeleted = 0 조건 포함.
     * startOfDay / endOfDay 는 호출부(Repository 또는 ViewModel)에서 계산.
     */
    fun observeTodayActivities(
        userId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ActivitySession>> {
        return dao.observeTodayActivities(userId, startOfDay, endOfDay)
            .map { entities -> entities.map { it.toActivitySession() } }
    }

    /**
     * 날짜 범위 활동 조회. 통계/분석 화면용.
     */
    fun getActivitiesByDateRange(
        userId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<ActivitySession>> {
        return dao.getActivitiesByDateRange(userId, startTime, endTime)
            .map { entities -> entities.map { it.toActivitySession() } }
    }

    /**
     * Firebase batch sync 대상 조회. Entity 그대로 반환 (sync 레이어에서 String activityId 필요).
     */
    suspend fun getUnsyncedActivities(userId: String): List<ActivityEntity> {
        return dao.getUnsyncedActivities(userId)
    }

    suspend fun markSynced(activityId: String) {
        dao.markActivitySynced(activityId)
    }

    // ── 검색 / 필터 ──────────────────────────────────────────────────────

    suspend fun searchActivities(userId: String, query: String): List<ActivitySession> {
        if (query.isBlank()) return emptyList()
        return dao.searchActivities(userId, query.trim()).map { it.toActivitySession() }
    }

    suspend fun filterByCategory(userId: String, category: String): List<ActivitySession> =
        dao.filterByCategory(userId, category).map { it.toActivitySession() }

    suspend fun filterByTag(userId: String, tag: String): List<ActivitySession> =
        dao.filterByTag(userId, tag).map { it.toActivitySession() }
}
