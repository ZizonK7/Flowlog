package com.example.flowlog.data.remote

import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.example.flowlog.data.local.entity.DailyGoalRecommendationEntity
import com.example.flowlog.data.local.entity.EventLogEntity
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.MainButtonConfig
import com.example.flowlog.data.model.MainButtonItem
import com.example.flowlog.data.model.TodoItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Firestore document ID 정책
 *
 * legacyId != null (SharedPrefs 시절 마이그레이션 데이터):
 *   → docId = legacyId.toString()  (예: "1234")
 *   → 기존 Firestore 문서와 호환 유지
 *
 * legacyId == null (Room 전용 신규 데이터):
 *   → docId = activityId / todoId  (Room String UUID)
 *   → "0" 충돌 방지. 절대 0L.toString() = "0"을 사용하지 않는다.
 *
 * per-action sync: [syncActivity]/[syncTodo] — ActivitySession.id/TodoItem.id 사용.
 *   신규 데이터는 timestamp ID가 id 필드에 들어오므로 "0" 충돌 없음.
 * batch sync: [FirebaseSyncDataSource] — 아래 [syncActivityByDocId]/[syncTodoByDocId] 사용.
 *   entity의 legacyId 유무를 보고 docId를 결정.
 */
class FirestoreSyncRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val uid: String?
        get() = auth.currentUser?.uid

    suspend fun syncActivities(activities: List<ActivitySession>) {
        val userId = uid ?: return
        activities.forEach { activity ->
            activityCollection(userId).document(activity.id.toString())
                .set(activity.toRemoteMap(), SetOptions.merge())
                .awaitResult()
        }
        markSynced(userId)
    }

    suspend fun syncActivity(activity: ActivitySession) {
        val userId = uid ?: return
        activityCollection(userId).document(activity.id.toString())
            .set(activity.toRemoteMap(), SetOptions.merge())
            .awaitResult()
        markSynced(userId)
    }

    suspend fun deleteActivity(id: Long) {
        val userId = uid ?: return
        activityCollection(userId).document(id.toString()).delete().awaitResult()
        markSynced(userId)
    }

    suspend fun syncTodos(todos: List<TodoItem>) {
        val userId = uid ?: return
        todos.forEach { todo ->
            todoCollection(userId).document(todo.id.toString())
                .set(todo.toRemoteMap(), SetOptions.merge())
                .awaitResult()
        }
        markSynced(userId)
    }

    suspend fun syncTodo(todo: TodoItem) {
        val userId = uid ?: return
        todoCollection(userId).document(todo.id.toString())
            .set(todo.toRemoteMap(), SetOptions.merge())
            .awaitResult()
        markSynced(userId)
    }

    suspend fun deleteTodo(id: Long) {
        val userId = uid ?: return
        todoCollection(userId).document(id.toString()).delete().awaitResult()
        markSynced(userId)
    }

    suspend fun syncDailyCue(
        cueId: Long,
        label: String,
        title: String,
        timerDurationMillis: Long?,
        timerCategory: String,
        recommendationTiming: String,
        note: String,
        sortOrder: Int,
        createdAt: Long,
        updatedAt: Long,
        archivedAt: Long?,
        isCompletedToday: Boolean,
        completionDateKey: Long
    ) {
        val userId = uid ?: return
        dailyCueCollection(userId).document(cueId.toString())
            .set(
                mapOf(
                    "cueId" to cueId,
                    "label" to label,
                    "title" to title,
                    "timerDurationMillis" to timerDurationMillis,
                    "timerCategory" to timerCategory,
                    "recommendationTiming" to recommendationTiming,
                    "note" to note,
                    "sortOrder" to sortOrder,
                    "createdAt" to createdAt,
                    "updatedAt" to updatedAt,
                    "archivedAt" to archivedAt,
                    "isCompletedToday" to isCompletedToday,
                    "completionDateKey" to completionDateKey
                ),
                SetOptions.merge()
            )
            .awaitResult()
        markSynced(userId)
    }

    // ── batch sync용 명시적 docId 메서드 ─────────────────────────────────
    // legacyId != null → legacyId.toString(), legacyId == null → Room UUID 사용.

    suspend fun syncActivityByDocId(docId: String, activity: ActivitySession) {
        val userId = uid ?: return
        activityCollection(userId).document(docId)
            .set(activity.toRemoteMap(), SetOptions.merge())
            .awaitResult()
        markSynced(userId)
    }

    suspend fun deleteActivityByDocId(docId: String) {
        val userId = uid ?: return
        activityCollection(userId).document(docId).delete().awaitResult()
        markSynced(userId)
    }

    suspend fun syncTodoByDocId(docId: String, todo: TodoItem) {
        val userId = uid ?: return
        todoCollection(userId).document(docId)
            .set(todo.toRemoteMap(), SetOptions.merge())
            .awaitResult()
        markSynced(userId)
    }

    suspend fun deleteTodoByDocId(docId: String) {
        val userId = uid ?: return
        todoCollection(userId).document(docId).delete().awaitResult()
        markSynced(userId)
    }

    // ── EventLog batch sync ──────────────────────────────────────────────
    // 저장 경로: users/{uid}/flowlog/data/eventLogs/{eventId}
    // syncStatus는 서버에 저장하지 않음 (로컬 Room에서만 관리).
    // markSynced(metadata) 호출 없음 — 이벤트 로그는 audit trail이므로 metadata 갱신 불필요.

    suspend fun syncEventLog(eventId: String, event: EventLogEntity) {
        val userId = uid ?: return
        eventLogCollection(userId).document(eventId)
            .set(event.toRemoteMap(), SetOptions.merge())
            .awaitResult()
    }

    // ── DailyGoal batch sync ─────────────────────────────────────────────
    // 저장 경로:
    // users/{uid}/flowlog/data/dailyGoalRecommendations/{recommendationId}
    // users/{uid}/flowlog/data/dailyGoalItems/{itemId}
    // 동일 문서에 대해 merge upsert 하므로 여러 번 sync되어도 중복 생성되지 않음.

    suspend fun syncDailyGoalRecommendation(
        recommendationId: String,
        recommendation: DailyGoalRecommendationEntity
    ) {
        val userId = uid ?: return
        dailyGoalRecommendationCollection(userId).document(recommendationId)
            .set(recommendation.toRemoteMap(), SetOptions.merge())
            .awaitResult()
    }

    suspend fun syncDailyGoalItem(itemId: String, item: DailyGoalItemEntity) {
        val userId = uid ?: return
        dailyGoalItemCollection(userId).document(itemId)
            .set(item.toRemoteMap(), SetOptions.merge())
            .awaitResult()
    }

    private fun eventLogCollection(userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("eventLogs")

    private fun dailyGoalRecommendationCollection(userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("dailyGoalRecommendations")

    private fun dailyGoalItemCollection(userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("dailyGoalItems")

    private fun EventLogEntity.toRemoteMap(): Map<String, Any?> = mapOf(
        "eventId" to eventId,
        "userId" to userId,
        "installationId" to installationId,
        "eventType" to eventType,
        "entityType" to entityType,
        "entityId" to entityId,
        "timestamp" to timestamp,
        "source" to source,
        "metadataJson" to metadataJson,
        "appVersion" to appVersion,
        "algorithmVersion" to algorithmVersion,
        "createdAt" to createdAt
    )

    private fun DailyGoalRecommendationEntity.toRemoteMap(): Map<String, Any?> = mapOf(
        "recommendationId" to recommendationId,
        "userId" to userId,
        "dateKey" to dateKey,
        "algorithmVersion" to algorithmVersion,
        "generatedAt" to generatedAt,
        "reasonSummary" to reasonSummary,
        "candidateSnapshotJson" to candidateSnapshotJson,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "recommendationMode" to recommendationMode,
        "workplaceDetected" to workplaceDetected,
        "workplaceBlocksJson" to workplaceBlocksJson,
        "selectedTodoIdsJson" to selectedTodoIdsJson,
        "heavyTodoId" to heavyTodoId,
        "heavyBurdenLevel" to heavyBurdenLevel,
        "heavyReason" to heavyReason,
        "heavyDistributionSnapshotJson" to heavyDistributionSnapshotJson,
        "lightTodoId" to lightTodoId,
        "lightBurdenLevel" to lightBurdenLevel,
        "lightReason" to lightReason,
        "lightDistributionSnapshotJson" to lightDistributionSnapshotJson,
        "plannedItemsJson" to plannedItemsJson
    )

    private fun DailyGoalItemEntity.toRemoteMap(): Map<String, Any?> = mapOf(
        "itemId" to itemId,
        "recommendationId" to recommendationId,
        "userId" to userId,
        "todoId" to todoId,
        "rank" to rank,
        "reason" to reason,
        "todoSnapshotJson" to todoSnapshotJson,
        "burdenLevel" to burdenLevel,
        "burdenReasonJson" to burdenReasonJson,
        "plannedStartMillis" to plannedStartMillis,
        "plannedEndMillis" to plannedEndMillis,
        "recommendedDurationMinutes" to recommendedDurationMinutes,
        "notificationScheduledAtMillis" to notificationScheduledAtMillis,
        "userActionStatus" to userActionStatus,
        "actualStartedAt" to actualStartedAt,
        "actualCompletedAt" to actualCompletedAt,
        "linkedActivityId" to linkedActivityId,
        "completedTodoId" to completedTodoId,
        "notificationDeliveredAt" to notificationDeliveredAt,
        "notificationClickedAt" to notificationClickedAt,
        "wasClicked" to wasClicked,
        "wasCompleted" to wasCompleted,
        "wasSkipped" to wasSkipped,
        "wasDeleted" to wasDeleted,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    // ── MainButtonConfig sync ─────────────────────────────────────────────
    // 저장 경로: users/{uid}/flowlog/config  (document)
    // 비교 기준 필드만 저장: category, order, isPinned. source/updatedAt 등 제외.

    suspend fun uploadMainButtonConfig(config: MainButtonConfig) {
        val userId = uid ?: return
        val buttonsData = config.buttons.map { btn ->
            mapOf("category" to btn.category, "order" to btn.order, "isPinned" to btn.isPinned)
        }
        firestore.collection("users").document(userId)
            .collection("flowlog").document("config")
            .set(
                mapOf(
                    "mainButton" to mapOf(
                        "buttons" to buttonsData,
                        "configured" to config.configured,
                        "version" to config.version
                    )
                ),
                SetOptions.merge()
            )
            .awaitResult()
    }

    suspend fun fetchMainButtonConfig(): MainButtonConfig? {
        val userId = uid ?: return null
        val snapshot = firestore.collection("users").document(userId)
            .collection("flowlog").document("config")
            .get()
            .awaitResult()
        @Suppress("UNCHECKED_CAST")
        val mainButton = snapshot?.get("mainButton") as? Map<String, Any> ?: return null
        val configured = mainButton["configured"] as? Boolean ?: return null
        if (!configured) return null
        val version = (mainButton["version"] as? Long)?.toInt() ?: 0
        @Suppress("UNCHECKED_CAST")
        val buttonsList = mainButton["buttons"] as? List<Map<String, Any>> ?: return null
        val buttons = buttonsList.mapNotNull { btnMap ->
            val category = btnMap["category"] as? String ?: return@mapNotNull null
            val order = (btnMap["order"] as? Long)?.toInt() ?: 0
            val isPinned = btnMap["isPinned"] as? Boolean ?: false
            MainButtonItem(category = category, order = order, isPinned = isPinned)
        }
        if (buttons.isEmpty()) return null
        return MainButtonConfig(buttons = buttons, configured = true, version = version)
    }

    private fun activityCollection(userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("activitySessions")

    private fun todoCollection(userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("todos")

    private fun dailyCueCollection(userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("dailyCues")

    private suspend fun markSynced(userId: String) {
        firestore.collection("users").document(userId)
            .collection("flowlog").document("metadata")
            .set(mapOf("updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .awaitResult()
    }

    private fun ActivitySession.toRemoteMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "category" to category,
        "title" to title,
        "startTime" to startTime,
        "endTime" to endTime,
        "durationMillis" to durationMillis,
        "note" to note,
        "tags" to tags,
        "exerciseSets" to exerciseSets.map {
            mapOf(
                "name" to it.name,
                "reps" to it.reps,
                "intensity" to it.intensity,
                "mode" to it.mode,
                "durationMillis" to it.durationMillis
            )
        },
        "isFavorite" to isFavorite,
        "linkedTodoId" to linkedTodoId,
        "sourceType" to sourceType,
        "sourceId" to sourceId,
        "modifiedTime" to modifiedTime
    )

    private fun TodoItem.toRemoteMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "category" to category.name,
        "isCompleted" to isCompleted,
        "createdAt" to createdAt,
        "completedAt" to completedAt,
        "selectedDate" to selectedDate,
        "accumulatedSeconds" to accumulatedSeconds,
        "burdenLevel" to burdenLevel,
        "burdenGroupKey" to burdenGroupKey,
        "burdenScore" to burdenScore,
        "burdenReasonJson" to burdenReasonJson,
        "updatedAt" to updatedAt
    )
}
