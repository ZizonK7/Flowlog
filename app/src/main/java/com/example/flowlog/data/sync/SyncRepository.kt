package com.example.flowlog.data.sync

/**
 * Firebase 동기화 인터페이스.
 *
 * 현재 구현체: [FirebaseSyncDataSource]
 *   - Room의 syncStatus = PENDING 항목 중 업로드 가능 시점이 된 것만 batch 업로드
 *   - 동일 문서는 안정적인 entity id를 docId로 사용해 중복 업로드를 방지
 *   - MainActivity, 알람 리시버, 활동 종료 경로는 모두 이 인터페이스만 사용
 */
interface SyncRepository {
    suspend fun syncAll(userId: String): SyncOutcome
    suspend fun syncPendingActivities(userId: String): SyncOutcome
    suspend fun syncPendingTodos(userId: String): SyncOutcome
    suspend fun syncPendingEvents(userId: String): SyncOutcome
    suspend fun syncPendingDailyGoalRecommendations(userId: String): SyncOutcome
    suspend fun syncPendingDailyGoalItems(userId: String): SyncOutcome
    suspend fun hasPendingSync(userId: String): Boolean
}
