package com.example.flowlog.data.sync

import android.content.Context
import android.util.Log
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.ActivityEntity
import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.example.flowlog.data.local.entity.DailyGoalRecommendationEntity
import com.example.flowlog.data.local.entity.EventLogEntity
import com.example.flowlog.data.local.entity.TodoEntity
import com.example.flowlog.data.remote.awaitResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class RestoreSection(
    val fetched: Int = 0,
    val inserted: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0
)

data class RestoreOutcome(
    val activities: RestoreSection = RestoreSection(),
    val todos: RestoreSection = RestoreSection(),
    val events: RestoreSection = RestoreSection(),
    val recommendations: RestoreSection = RestoreSection(),
    val items: RestoreSection = RestoreSection()
)

/**
 * Firestore → Room 복원.
 *
 * 호출 조건: 로그인 직후, 로컬 activities + todos count 가 모두 0인 경우(신규 설치/재설치).
 * 복원된 데이터는 syncStatus = SYNCED 로 삽입 → 중복 업로드 방지.
 * 로그 태그: "FlowlogRestore"
 */
class FirebaseRestoreDataSource(context: Context) {

    private val db = FlowlogDatabase.getInstance(context)
    private val activityDao = db.activityDao()
    private val todoDao = db.todoDao()
    private val eventLogDao = db.eventLogDao()
    private val dailyGoalDao = db.dailyGoalDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val json = Json { ignoreUnknownKeys = true }

    private fun activityCollection(userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("activitySessions")

    private fun todoCollection(userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("todos")

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

    suspend fun restoreFromFirestore(userId: String): RestoreOutcome {
        return withContext(Dispatchers.IO) {
            Log.i(TAG, "restore start — userId=$userId")

            val activityResult = runCatching { restoreActivities(userId) }
                .onFailure { e -> Log.w(TAG, "Activity restore section failed: ${e.message}", e) }
                .getOrDefault(RestoreSection())

            val todoResult = runCatching { restoreTodos(userId) }
                .onFailure { e -> Log.w(TAG, "Todo restore section failed: ${e.message}", e) }
                .getOrDefault(RestoreSection())

            val eventResult = runCatching { restoreEventLogs(userId) }
                .onFailure { e -> Log.w(TAG, "EventLog restore section failed: ${e.message}", e) }
                .getOrDefault(RestoreSection())

            val recommendationResult = runCatching { restoreRecommendations(userId) }
                .onFailure { e -> Log.w(TAG, "Recommendation restore section failed: ${e.message}", e) }
                .getOrDefault(RestoreSection())

            val itemResult = runCatching { restoreGoalItems(userId) }
                .onFailure { e -> Log.w(TAG, "GoalItem restore section failed: ${e.message}", e) }
                .getOrDefault(RestoreSection())

            Log.i(
                TAG,
                "restore complete — " +
                    "activities(fetched=${activityResult.fetched} inserted=${activityResult.inserted} skipped=${activityResult.skipped} failed=${activityResult.failed}) " +
                    "todos(fetched=${todoResult.fetched} inserted=${todoResult.inserted} skipped=${todoResult.skipped} failed=${todoResult.failed}) " +
                    "events(fetched=${eventResult.fetched} inserted=${eventResult.inserted} failed=${eventResult.failed}) " +
                    "recommendations(fetched=${recommendationResult.fetched} inserted=${recommendationResult.inserted} failed=${recommendationResult.failed}) " +
                    "items(fetched=${itemResult.fetched} inserted=${itemResult.inserted} failed=${itemResult.failed})"
            )

            RestoreOutcome(activityResult, todoResult, eventResult, recommendationResult, itemResult)
        }
    }

    private suspend fun restoreActivities(userId: String): RestoreSection {
        val docs = activityCollection(userId).get().awaitResult().documents
        Log.i(TAG, "Activities fetched: ${docs.size}")
        if (docs.isEmpty()) return RestoreSection()

        var inserted = 0
        var skipped = 0
        var failed = 0

        docs.forEach { doc ->
            runCatching {
                // legacyId: 0L은 Room-native 데이터를 의미하므로 null 처리
                val legacyId = doc.getLong("id")?.takeIf { it != 0L }
                val activityId = if (legacyId != null) "legacy_activity_$legacyId" else doc.id

                if (activityDao.getActivityById(activityId) != null) {
                    skipped++
                    return@runCatching
                }

                val tags = (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val tagsJson = if (tags.isNotEmpty()) json.encodeToString(tags) else null
                val linkedTodoLegacyId = doc.getLong("linkedTodoId")

                activityDao.insertActivity(
                    ActivityEntity(
                        activityId = activityId,
                        userId = userId,
                        title = doc.getString("title") ?: "",
                        category = doc.getString("category") ?: "",
                        note = doc.getString("note"),
                        startTime = doc.getLong("startTime") ?: 0L,
                        endTime = doc.getLong("endTime"),
                        durationMillis = doc.getLong("durationMillis") ?: 0L,
                        isFavorite = doc.getBoolean("isFavorite") ?: false,
                        linkedTodoId = linkedTodoLegacyId?.let { "legacy_todo_$it" },
                        legacyId = legacyId,
                        legacyLinkedTodoId = linkedTodoLegacyId,
                        tagsJson = tagsJson,
                        sourceType = doc.getString("sourceType") ?: "MANUAL",
                        sourceId = doc.getString("sourceId"),
                        createdAt = doc.getLong("startTime") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("modifiedTime") ?: System.currentTimeMillis(),
                        isDeleted = false,
                        deletedAt = null,
                        syncStatus = SyncStatus.SYNCED
                    )
                )
                inserted++
            }.onFailure { e ->
                failed++
                Log.w(TAG, "Activity restore item failed: ${doc.id} — ${e.message}")
            }
        }

        Log.i(TAG, "Activities — fetched=${docs.size} inserted=$inserted skipped=$skipped failed=$failed")
        return RestoreSection(fetched = docs.size, inserted = inserted, skipped = skipped, failed = failed)
    }

    private suspend fun restoreTodos(userId: String): RestoreSection {
        val docs = todoCollection(userId).get().awaitResult().documents
        Log.i(TAG, "Todos fetched: ${docs.size}")
        if (docs.isEmpty()) return RestoreSection()

        var inserted = 0
        var skipped = 0
        var failed = 0

        docs.forEach { doc ->
            runCatching {
                val legacyId = doc.getLong("id")?.takeIf { it != 0L }
                val todoId = if (legacyId != null) "legacy_todo_$legacyId" else doc.id

                if (todoDao.getTodoById(todoId) != null) {
                    skipped++
                    return@runCatching
                }

                todoDao.insertTodo(
                    TodoEntity(
                        todoId = todoId,
                        userId = userId,
                        title = doc.getString("title") ?: "",
                        category = doc.getString("category") ?: "NORMAL",
                        selectedDate = doc.getLong("selectedDate"),
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        completedAt = doc.getLong("completedAt"),
                        isDeleted = false,
                        deletedAt = null,
                        // remote map은 accumulatedSeconds(초 단위) 저장 → 밀리초로 변환
                        accumulatedWorkMillis = (doc.getLong("accumulatedSeconds") ?: 0L) * 1000L,
                        burdenLevel = doc.getString("burdenLevel"),
                        burdenGroupKey = doc.getString("burdenGroupKey"),
                        burdenScore = (doc.getLong("burdenScore") ?: 0L).toInt(),
                        burdenReasonJson = doc.getString("burdenReasonJson"),
                        legacyId = legacyId,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED
                    )
                )
                inserted++
            }.onFailure { e ->
                failed++
                Log.w(TAG, "Todo restore item failed: ${doc.id} — ${e.message}")
            }
        }

        Log.i(TAG, "Todos — fetched=${docs.size} inserted=$inserted skipped=$skipped failed=$failed")
        return RestoreSection(fetched = docs.size, inserted = inserted, skipped = skipped, failed = failed)
    }

    private suspend fun restoreEventLogs(userId: String): RestoreSection {
        val docs = eventLogCollection(userId).get().awaitResult().documents
        Log.i(TAG, "EventLogs fetched: ${docs.size}")
        if (docs.isEmpty()) return RestoreSection()

        var failed = 0
        val entities = mutableListOf<EventLogEntity>()

        docs.forEach { doc ->
            runCatching {
                entities.add(
                    EventLogEntity(
                        eventId = doc.getString("eventId") ?: doc.id,
                        userId = userId,
                        installationId = doc.getString("installationId"),
                        eventType = doc.getString("eventType") ?: "",
                        entityType = doc.getString("entityType"),
                        entityId = doc.getString("entityId"),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        source = doc.getString("source") ?: "APP",
                        metadataJson = doc.getString("metadataJson"),
                        appVersion = doc.getString("appVersion"),
                        algorithmVersion = doc.getString("algorithmVersion"),
                        syncStatus = SyncStatus.SYNCED,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                )
            }.onFailure { e ->
                failed++
                Log.w(TAG, "EventLog restore item failed: ${doc.id} — ${e.message}")
            }
        }

        // IGNORE 전략 — 이미 존재하는 eventId는 조용히 무시됨
        eventLogDao.insertEvents(entities)
        Log.i(TAG, "EventLogs — fetched=${docs.size} inserted=${entities.size} failed=$failed")
        return RestoreSection(fetched = docs.size, inserted = entities.size, skipped = 0, failed = failed)
    }

    private suspend fun restoreRecommendations(userId: String): RestoreSection {
        val docs = dailyGoalRecommendationCollection(userId).get().awaitResult().documents
        Log.i(TAG, "Recommendations fetched: ${docs.size}")
        if (docs.isEmpty()) return RestoreSection()

        var inserted = 0
        var failed = 0

        docs.forEach { doc ->
            runCatching {
                dailyGoalDao.insertRecommendation(
                    DailyGoalRecommendationEntity(
                        recommendationId = doc.getString("recommendationId") ?: doc.id,
                        userId = userId,
                        dateKey = doc.getString("dateKey") ?: "",
                        algorithmVersion = doc.getString("algorithmVersion") ?: "",
                        generatedAt = doc.getLong("generatedAt") ?: 0L,
                        reasonSummary = doc.getString("reasonSummary"),
                        candidateSnapshotJson = doc.getString("candidateSnapshotJson"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        recommendationMode = doc.getString("recommendationMode"),
                        workplaceDetected = doc.getBoolean("workplaceDetected") ?: false,
                        workplaceBlocksJson = doc.getString("workplaceBlocksJson"),
                        selectedTodoIdsJson = doc.getString("selectedTodoIdsJson"),
                        heavyTodoId = doc.getString("heavyTodoId"),
                        heavyBurdenLevel = doc.getString("heavyBurdenLevel"),
                        heavyReason = doc.getString("heavyReason"),
                        heavyDistributionSnapshotJson = doc.getString("heavyDistributionSnapshotJson"),
                        lightTodoId = doc.getString("lightTodoId"),
                        lightBurdenLevel = doc.getString("lightBurdenLevel"),
                        lightReason = doc.getString("lightReason"),
                        lightDistributionSnapshotJson = doc.getString("lightDistributionSnapshotJson"),
                        plannedItemsJson = doc.getString("plannedItemsJson"),
                        syncStatus = SyncStatus.SYNCED
                    )
                )
                inserted++
            }.onFailure { e ->
                failed++
                Log.w(TAG, "Recommendation restore item failed: ${doc.id} — ${e.message}")
            }
        }

        Log.i(TAG, "Recommendations — fetched=${docs.size} inserted=$inserted failed=$failed")
        return RestoreSection(fetched = docs.size, inserted = inserted, skipped = 0, failed = failed)
    }

    private suspend fun restoreGoalItems(userId: String): RestoreSection {
        val docs = dailyGoalItemCollection(userId).get().awaitResult().documents
        Log.i(TAG, "GoalItems fetched: ${docs.size}")
        if (docs.isEmpty()) return RestoreSection()

        var failed = 0
        val entities = mutableListOf<DailyGoalItemEntity>()

        docs.forEach { doc ->
            runCatching {
                entities.add(
                    DailyGoalItemEntity(
                        itemId = doc.getString("itemId") ?: doc.id,
                        recommendationId = doc.getString("recommendationId") ?: "",
                        userId = userId,
                        todoId = doc.getString("todoId") ?: "",
                        rank = (doc.getLong("rank") ?: 0L).toInt(),
                        reason = doc.getString("reason"),
                        todoSnapshotJson = doc.getString("todoSnapshotJson"),
                        burdenLevel = doc.getString("burdenLevel"),
                        burdenReasonJson = doc.getString("burdenReasonJson"),
                        plannedStartMillis = doc.getLong("plannedStartMillis"),
                        plannedEndMillis = doc.getLong("plannedEndMillis"),
                        recommendedDurationMinutes = doc.getLong("recommendedDurationMinutes")?.toInt(),
                        notificationScheduledAtMillis = doc.getLong("notificationScheduledAtMillis"),
                        userActionStatus = doc.getString("userActionStatus") ?: "PLANNED",
                        actualStartedAt = doc.getLong("actualStartedAt"),
                        actualCompletedAt = doc.getLong("actualCompletedAt"),
                        linkedActivityId = doc.getString("linkedActivityId"),
                        completedTodoId = doc.getString("completedTodoId"),
                        notificationDeliveredAt = doc.getLong("notificationDeliveredAt"),
                        notificationClickedAt = doc.getLong("notificationClickedAt"),
                        wasClicked = doc.getBoolean("wasClicked") ?: false,
                        wasCompleted = doc.getBoolean("wasCompleted") ?: false,
                        wasSkipped = doc.getBoolean("wasSkipped") ?: false,
                        wasDeleted = doc.getBoolean("wasDeleted") ?: false,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            }.onFailure { e ->
                failed++
                Log.w(TAG, "GoalItem restore item failed: ${doc.id} — ${e.message}")
            }
        }

        // REPLACE 전략 — 복원 시점에 로컬이 비어 있으므로 충돌 없음
        dailyGoalDao.insertGoalItems(entities)
        Log.i(TAG, "GoalItems — fetched=${docs.size} inserted=${entities.size} failed=$failed")
        return RestoreSection(fetched = docs.size, inserted = entities.size, skipped = 0, failed = failed)
    }

    companion object {
        private const val TAG = "FlowlogRestore"
    }
}
