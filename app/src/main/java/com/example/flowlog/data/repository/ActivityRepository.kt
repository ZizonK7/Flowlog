package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.local.RoomActivityLocalDataSource
import com.example.flowlog.data.model.ActivitySession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import java.util.Calendar
import java.util.concurrent.atomic.AtomicLong

class ActivityRepository(context: Context) {
    private val eventLogRepository = EventLogRepository(context)
    private val roomDataSource = RoomActivityLocalDataSource(context)

    // 신규 Activity ID 생성자. currentTimeMillis로 초기화 후 세션 내 atomic increment.
    // 기존 legacyId(1, 2, 3...)와 충돌 없음 (타임스탬프 영역은 ~1.7×10¹²).
    private val idCounter = AtomicLong(System.currentTimeMillis())

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    // ── 읽기 경로 (Room) ─────────────────────────────────────────────────

    // auth 상태 변화 시 userId를 재평가하고 Room Flow를 재구독.
    private fun userIdFlow(): Flow<String> = callbackFlow {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.uid ?: "anonymous")
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid ?: "anonymous")
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    fun getAllActivities(): Flow<List<ActivitySession>> =
        userIdFlow().flatMapLatest { uid -> roomDataSource.observeAllActivities(uid) }

    fun getTodayActivities(timestamp: Long): Flow<List<ActivitySession>> {
        val (startOfDay, endOfDay) = dayRange(timestamp)
        return userIdFlow().flatMapLatest { uid ->
            roomDataSource.observeTodayActivities(uid, startOfDay, endOfDay)
        }
    }

    fun getActivitiesByDateRange(startTime: Long, endTime: Long): Flow<List<ActivitySession>> =
        userIdFlow().flatMapLatest { uid ->
            roomDataSource.getActivitiesByDateRange(uid, startTime, endTime)
        }

    suspend fun getActivityById(id: Long): ActivitySession? =
        roomDataSource.getActivityByLegacyId(id)

    suspend fun searchActivities(query: String): List<ActivitySession> =
        roomDataSource.searchActivities(userId, query)

    suspend fun filterByCategory(category: String): List<ActivitySession> =
        roomDataSource.filterByCategory(userId, category)

    suspend fun filterByTag(tag: String): List<ActivitySession> =
        roomDataSource.filterByTag(userId, tag)

    // ── 쓰기 경로 (Room primary) ──────────────────────────────────────────
    //
    // Room이 유일한 로컬 저장소. per-action Firebase sync 제거됨.
    // ID 정책: AtomicLong 기반 타임스탬프 → legacyId 영역(1,2,3...)과 충돌 없음.
    //
    // syncStatus 정책:
    //   Room write 직후 → PENDING (mapper 기본값)
    //   Firebase sync → FirebaseSyncDataSource.syncAll(uid) 가 PENDING 항목 batch upload
    //   soft delete → isDeleted=1 + PENDING → batch sync에서 Firestore delete 처리

    suspend fun insertActivity(activity: ActivitySession): Long {
        val id = idCounter.incrementAndGet()
        val activityWithId = activity.copy(id = id)
        // Room primary write — observeAllActivities Flow 즉시 emit → UI 즉각 갱신
        roomDataSource.insert(activityWithId, userId)
        runCatching {
            eventLogRepository.log(
                eventType = EventType.ACTIVITY_SAVED,
                entityType = EntityType.ACTIVITY,
                entityId = id.toString()
            )
        }
        return id
    }

    suspend fun updateActivity(activity: ActivitySession) {
        runCatching { roomDataSource.update(activity, userId) }
        runCatching {
            eventLogRepository.log(
                eventType = EventType.ACTIVITY_UPDATED,
                entityType = EntityType.ACTIVITY,
                entityId = activity.id.toString()
            )
        }
    }

    suspend fun deleteActivity(activity: ActivitySession) {
        runCatching { roomDataSource.softDeleteByLegacyId(activity.id) }
        runCatching {
            eventLogRepository.log(
                eventType = EventType.ACTIVITY_DELETED,
                entityType = EntityType.ACTIVITY,
                entityId = activity.id.toString()
            )
        }
    }

    suspend fun deleteActivityById(id: Long) {
        runCatching { roomDataSource.softDeleteByLegacyId(id) }
        runCatching {
            eventLogRepository.log(
                eventType = EventType.ACTIVITY_DELETED,
                entityType = EntityType.ACTIVITY,
                entityId = id.toString()
            )
        }
    }

    suspend fun hasActivityBySourceToday(sourceType: String, sourceId: String): Boolean {
        val (startOfDay, endOfDay) = dayRange(System.currentTimeMillis())
        return roomDataSource.countBySourceForDate(
            userId = userId,
            sourceType = sourceType,
            sourceId = sourceId,
            startOfDay = startOfDay,
            endOfDay = endOfDay
        ) > 0
    }

    suspend fun deleteActivitiesBySourceToday(sourceType: String, sourceId: String) {
        val (startOfDay, endOfDay) = dayRange(System.currentTimeMillis())
        runCatching {
            roomDataSource.softDeleteBySourceForDate(
                userId = userId,
                sourceType = sourceType,
                sourceId = sourceId,
                startOfDay = startOfDay,
                endOfDay = endOfDay
            )
        }
        runCatching {
            eventLogRepository.log(
                eventType = EventType.ACTIVITY_DELETED,
                entityType = EntityType.ACTIVITY,
                entityId = "$sourceType:$sourceId"
            )
        }
    }

    // ── 유틸리티 ─────────────────────────────────────────────────────────

    private fun dayRange(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return Pair(start, cal.timeInMillis)
    }
}
