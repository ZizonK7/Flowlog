package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.entity.EventLogEntity
import kotlinx.coroutines.flow.Flow

// OnConflictStrategy.IGNORE: EventLog는 append-only.
// 동일한 eventId로 재삽입 시도 시 조용히 무시 (중복 방지).
// delete 및 update(syncStatus 제외)는 의도적으로 제공하지 않음.
@Dao
interface EventLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: EventLogEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<EventLogEntity>)

    @Query("SELECT * FROM event_logs WHERE eventId = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): EventLogEntity?

    // 최근 이벤트 실시간 관찰 (홈 화면 디버그 패널, 개발자 모드용)
    @Query("""
        SELECT * FROM event_logs
        WHERE userId = :userId
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun observeRecentEvents(userId: String, limit: Int): Flow<List<EventLogEntity>>

    // 날짜 범위 조회 (분석, export용)
    @Query("""
        SELECT * FROM event_logs
        WHERE userId = :userId
          AND timestamp >= :startTime
          AND timestamp < :endTime
        ORDER BY timestamp ASC
    """)
    suspend fun getEventsByDateRange(
        userId: String,
        startTime: Long,
        endTime: Long
    ): List<EventLogEntity>

    @Query("""
        SELECT * FROM event_logs
        WHERE userId = :userId
          AND eventType = :eventType
        ORDER BY timestamp DESC
    """)
    suspend fun getEventsByType(userId: String, eventType: String): List<EventLogEntity>

    // 특정 Entity와 연관된 이벤트 조회 (예: activityId의 전체 이력)
    @Query("""
        SELECT * FROM event_logs
        WHERE userId = :userId
          AND entityType = :entityType
          AND entityId = :entityId
        ORDER BY timestamp ASC
    """)
    suspend fun getEventsForEntity(
        userId: String,
        entityType: String,
        entityId: String
    ): List<EventLogEntity>

    @Query("""
        SELECT * FROM event_logs
        WHERE userId = :userId
          AND syncStatus = '${SyncStatus.PENDING}'
        ORDER BY timestamp ASC
    """)
    suspend fun getUnsyncedEvents(userId: String): List<EventLogEntity>

        @Query("""
                SELECT * FROM event_logs
                WHERE userId = :userId
                    AND syncStatus = '${SyncStatus.PENDING}'
                    AND createdAt < :cutoffMillis
                ORDER BY timestamp ASC
        """)
        suspend fun getEligibleUnsyncedEvents(userId: String, cutoffMillis: Long): List<EventLogEntity>

    @Query("""
        UPDATE event_logs
        SET syncStatus = '${SyncStatus.SYNCED}'
        WHERE eventId = :eventId
    """)
    suspend fun markEventSynced(eventId: String)

    // 여러 이벤트 일괄 sync 완료 처리 (batch sync 후 호출)
    @Query("""
        UPDATE event_logs
        SET syncStatus = '${SyncStatus.SYNCED}'
        WHERE eventId IN (:eventIds)
    """)
    suspend fun markEventsSynced(eventIds: List<String>)

    @Query("""
        SELECT COUNT(*) FROM event_logs
        WHERE userId = :userId
          AND syncStatus = '${SyncStatus.PENDING}'
    """)
    suspend fun getUnsyncedEventCount(userId: String): Int

    // 로그인 전 'anonymous' userId로 저장된 이벤트를 실제 uid로 재할당
    @Query("UPDATE event_logs SET userId = :newUserId, syncStatus = 'PENDING' WHERE userId = 'anonymous'")
    suspend fun reassignAnonymousUser(newUserId: String): Int
}
