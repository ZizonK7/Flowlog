package com.example.flowlog.data.sync

import android.content.Context
import android.util.Log
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.example.flowlog.data.local.entity.DailyGoalRecommendationEntity
import com.example.flowlog.data.local.entity.SyncBatchEntity
import com.example.flowlog.data.local.mapper.toActivitySession
import com.example.flowlog.data.local.mapper.toTodoItem
import com.example.flowlog.data.remote.FirestoreSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Room의 syncStatus = PENDING 항목을 읽어 Firestore에 batch 업로드하는 구현체.
 *
 * 호출 위치: MainActivity (로그인 직후, 네트워크 복구 시)
 * 로그 태그: "FlowlogSync"
 */
class FirebaseSyncDataSource(context: Context) : SyncRepository {

    private val db = FlowlogDatabase.getInstance(context)
    private val activityDao = db.activityDao()
    private val todoDao = db.todoDao()
    private val eventLogDao = db.eventLogDao()
    private val dailyGoalDao = db.dailyGoalDao()
    private val syncBatchDao = db.syncBatchDao()
    private val examStrategyCheckDao = db.examStrategyCheckDao()
    private val firestoreSync = FirestoreSyncRepository()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun syncAll(userId: String): SyncOutcome {
        return withContext(Dispatchers.IO) {
            val activityCount = runCatching { activityDao.getUnsyncedActivities(userId).size }.getOrDefault(0)
            val todoCount = runCatching { todoDao.getUnsyncedTodos(userId).size }.getOrDefault(0)
            val eventCount = runCatching { eventLogDao.getUnsyncedEvents(userId).size }.getOrDefault(0)
            val dailyGoalRecommendationCount = runCatching { dailyGoalDao.getUnsyncedRecommendations(userId).size }.getOrDefault(0)
            val dailyGoalItemCount = runCatching { dailyGoalDao.getUnsyncedItems(userId).size }.getOrDefault(0)

            Log.i(TAG, "syncAll start — userId=$userId | activities=$activityCount todos=$todoCount events=$eventCount dailyGoalRecommendations=$dailyGoalRecommendationCount dailyGoalItems=$dailyGoalItemCount")

            val activityResult = runCatching { syncPendingActivities(userId) }
                .onFailure { e -> Log.w(TAG, "Activity sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())
            val todoResult = runCatching { syncPendingTodos(userId) }
                .onFailure { e -> Log.w(TAG, "Todo sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())
            val eventResult = runCatching { syncPendingEvents(userId) }
                .onFailure { e -> Log.w(TAG, "Event sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())
            val dailyGoalRecommendationResult = runCatching { syncPendingDailyGoalRecommendations(userId) }
                .onFailure { e -> Log.w(TAG, "DailyGoalRecommendation sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())
            val dailyGoalItemResult = runCatching { syncPendingDailyGoalItems(userId) }
                .onFailure { e -> Log.w(TAG, "DailyGoalItem sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())
            val examCheckResult = runCatching { syncPendingExamChecks(userId) }
                .onFailure { e -> Log.w(TAG, "ExamCheck sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())

            Log.i(TAG, "syncAll complete — userId=$userId")
            return@withContext activityResult + todoResult + eventResult + dailyGoalRecommendationResult + dailyGoalItemResult + examCheckResult
        }
    }

    suspend fun syncEligible(userId: String): SyncOutcome {
        return withContext(Dispatchers.IO) {
            val cutoffMillis = todayStartMillis()
            val activityCount = runCatching { activityDao.getEligibleUnsyncedActivities(userId, cutoffMillis).size }.getOrDefault(0)
            val todoCount = runCatching { todoDao.getEligibleUnsyncedTodos(userId, cutoffMillis).size }.getOrDefault(0)
            val eventCount = runCatching { eventLogDao.getEligibleUnsyncedEvents(userId, cutoffMillis).size }.getOrDefault(0)
            val dailyGoalRecommendationCount = runCatching { dailyGoalDao.getEligibleUnsyncedRecommendations(userId, cutoffMillis).size }.getOrDefault(0)
            val dailyGoalItemCount = runCatching { dailyGoalDao.getEligibleUnsyncedItems(userId, cutoffMillis).size }.getOrDefault(0)

            Log.i(TAG, "syncEligible start — userId=$userId | activities=$activityCount todos=$todoCount events=$eventCount dailyGoalRecommendations=$dailyGoalRecommendationCount dailyGoalItems=$dailyGoalItemCount")

            val activityResult = runCatching { syncPendingActivities(userId, eligibleOnly = true) }
                .onFailure { e -> Log.w(TAG, "Activity sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())
            val todoResult = runCatching { syncPendingTodos(userId, eligibleOnly = true) }
                .onFailure { e -> Log.w(TAG, "Todo sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())
            val eventResult = runCatching { syncPendingEvents(userId, eligibleOnly = true) }
                .onFailure { e -> Log.w(TAG, "Event sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())
            val dailyGoalRecommendationResult = runCatching { syncPendingDailyGoalRecommendations(userId, eligibleOnly = true) }
                .onFailure { e -> Log.w(TAG, "DailyGoalRecommendation sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())
            val dailyGoalItemResult = runCatching { syncPendingDailyGoalItems(userId, eligibleOnly = true) }
                .onFailure { e -> Log.w(TAG, "DailyGoalItem sync section failed: ${e.message}", e) }
                .getOrDefault(SyncOutcome())

            Log.i(TAG, "syncEligible complete — userId=$userId")
            return@withContext activityResult + todoResult + eventResult + dailyGoalRecommendationResult + dailyGoalItemResult
        }
    }

    override suspend fun syncPendingActivities(userId: String): SyncOutcome {
        return syncPendingActivities(userId, eligibleOnly = false)
    }

    private suspend fun syncPendingActivities(userId: String, eligibleOnly: Boolean): SyncOutcome {
        return withContext(Dispatchers.IO) {
            val pending = runCatching {
                if (eligibleOnly) {
                    activityDao.getEligibleUnsyncedActivities(userId, todayStartMillis())
                } else {
                    activityDao.getUnsyncedActivities(userId)
                }
            }.getOrDefault(emptyList())
            Log.i(TAG, "Activities pending: ${pending.size}")
            if (pending.isEmpty()) return@withContext SyncOutcome()

            val batchId = UUID.randomUUID().toString()
            val dateKey = dateFormat.format(Date())
            syncBatchDao.insertBatch(
                SyncBatchEntity(
                    batchId = batchId,
                    userId = userId,
                    dateKey = dateKey,
                    eventCount = pending.size,
                    status = "PENDING"
                )
            )

            var successCount = 0
            val errors = mutableListOf<String>()

            pending.forEach { entity ->
                runCatching {
                    val docId = entity.legacyId?.toString() ?: entity.activityId
                    if (entity.isDeleted) {
                        firestoreSync.deleteActivityByDocId(docId)
                    } else {
                        firestoreSync.syncActivityByDocId(docId, entity.toActivitySession())
                    }
                    activityDao.markActivitySynced(entity.activityId)
                    successCount++
                }.onFailure { e ->
                    errors.add("${entity.activityId}: ${e.message}")
                    Log.w(TAG, "Activity sync failed: ${entity.activityId} — ${e.message}")
                }
            }

            val now = System.currentTimeMillis()
            if (errors.isEmpty()) {
                syncBatchDao.markBatchSuccess(batchId, "SUCCESS", now)
                Log.i(TAG, "Activities synced: $successCount/${pending.size}")
            } else {
                syncBatchDao.markBatchFailed(batchId, "FAILED", errors.joinToString("; "))
                Log.w(TAG, "Activities sync partial: $successCount ok, ${errors.size} failed | ${errors.joinToString("; ")}")
            }

            return@withContext SyncOutcome(
                attemptedCount = pending.size,
                successCount = successCount,
                failureCount = errors.size
            )
        }
    }

    override suspend fun syncPendingTodos(userId: String): SyncOutcome {
        return syncPendingTodos(userId, eligibleOnly = false)
    }

    private suspend fun syncPendingTodos(userId: String, eligibleOnly: Boolean): SyncOutcome {
        return withContext(Dispatchers.IO) {
            val pending = runCatching {
                if (eligibleOnly) {
                    todoDao.getEligibleUnsyncedTodos(userId, todayStartMillis())
                } else {
                    todoDao.getUnsyncedTodos(userId)
                }
            }.getOrDefault(emptyList())
            Log.i(TAG, "Todos pending: ${pending.size}")
            if (pending.isEmpty()) return@withContext SyncOutcome()

            val batchId = UUID.randomUUID().toString()
            val dateKey = dateFormat.format(Date())
            syncBatchDao.insertBatch(
                SyncBatchEntity(
                    batchId = batchId,
                    userId = userId,
                    dateKey = dateKey,
                    eventCount = pending.size,
                    status = "PENDING"
                )
            )

            var successCount = 0
            val errors = mutableListOf<String>()

            pending.forEach { entity ->
                runCatching {
                    val docId = entity.legacyId?.toString() ?: entity.todoId
                    if (entity.isDeleted) {
                        firestoreSync.deleteTodoByDocId(docId)
                    } else {
                        firestoreSync.syncTodoByDocId(docId, entity.toTodoItem())
                    }
                    todoDao.markTodoSynced(entity.todoId)
                    successCount++
                }.onFailure { e ->
                    errors.add("${entity.todoId}: ${e.message}")
                    Log.w(TAG, "Todo sync failed: ${entity.todoId} — ${e.message}")
                }
            }

            val now = System.currentTimeMillis()
            if (errors.isEmpty()) {
                syncBatchDao.markBatchSuccess(batchId, "SUCCESS", now)
                Log.i(TAG, "Todos synced: $successCount/${pending.size}")
            } else {
                syncBatchDao.markBatchFailed(batchId, "FAILED", errors.joinToString("; "))
                Log.w(TAG, "Todos sync partial: $successCount ok, ${errors.size} failed | ${errors.joinToString("; ")}")
            }

            return@withContext SyncOutcome(
                attemptedCount = pending.size,
                successCount = successCount,
                failureCount = errors.size
            )
        }
    }

    override suspend fun syncPendingEvents(userId: String): SyncOutcome {
        return syncPendingEvents(userId, eligibleOnly = false)
    }

    private suspend fun syncPendingEvents(userId: String, eligibleOnly: Boolean): SyncOutcome {
        return withContext(Dispatchers.IO) {
            val pending = runCatching {
                if (eligibleOnly) {
                    eventLogDao.getEligibleUnsyncedEvents(userId, todayStartMillis())
                } else {
                    eventLogDao.getUnsyncedEvents(userId)
                }
            }.getOrDefault(emptyList())
            Log.i(TAG, "EventLogs pending: ${pending.size}")
            if (pending.isEmpty()) return@withContext SyncOutcome()

            val batchId = UUID.randomUUID().toString()
            val dateKey = dateFormat.format(Date())
            syncBatchDao.insertBatch(
                SyncBatchEntity(
                    batchId = batchId,
                    userId = userId,
                    dateKey = dateKey,
                    eventCount = pending.size,
                    status = "PENDING"
                )
            )
            Log.d(TAG, "EventLog batch created: batchId=$batchId total=${pending.size}")

            val syncedIds = mutableListOf<String>()
            val errors = mutableListOf<String>()

            pending.forEach { entity ->
                runCatching {
                    firestoreSync.syncEventLog(entity.eventId, entity)
                    syncedIds.add(entity.eventId)
                }.onFailure { e ->
                    errors.add("${entity.eventId}: ${e.message}")
                    Log.w(TAG, "EventLog sync failed: ${entity.eventId} — ${e.message}")
                }
            }

            val now = System.currentTimeMillis()
            if (syncedIds.isNotEmpty()) {
                runCatching { eventLogDao.markEventsSynced(syncedIds) }
            }

            if (errors.isEmpty()) {
                syncBatchDao.markBatchSuccess(batchId, "SUCCESS", now)
                Log.i(TAG, "EventLogs synced: ${syncedIds.size}/${pending.size}")
            } else {
                syncBatchDao.markBatchFailed(batchId, "FAILED", errors.joinToString("; "))
                Log.w(TAG, "EventLogs sync partial: ${syncedIds.size} ok, ${errors.size} failed | ${errors.joinToString("; ")}")
            }

            return@withContext SyncOutcome(
                attemptedCount = pending.size,
                successCount = syncedIds.size,
                failureCount = errors.size
            )
        }
    }

    override suspend fun syncPendingDailyGoalRecommendations(userId: String): SyncOutcome {
        return syncPendingDailyGoalRecommendations(userId, eligibleOnly = false)
    }

    private suspend fun syncPendingDailyGoalRecommendations(userId: String, eligibleOnly: Boolean): SyncOutcome {
        return withContext(Dispatchers.IO) {
            val pending = runCatching {
                if (eligibleOnly) {
                    dailyGoalDao.getEligibleUnsyncedRecommendations(userId, todayStartMillis())
                } else {
                    dailyGoalDao.getUnsyncedRecommendations(userId)
                }
            }.getOrDefault(emptyList())
            Log.i(TAG, "DailyGoalRecommendations pending: ${pending.size}")
            if (pending.isEmpty()) return@withContext SyncOutcome()

            val batchId = UUID.randomUUID().toString()
            val dateKey = dateFormat.format(Date())
            syncBatchDao.insertBatch(
                SyncBatchEntity(
                    batchId = batchId,
                    userId = userId,
                    dateKey = dateKey,
                    eventCount = pending.size,
                    status = "PENDING"
                )
            )

            val syncedIds = mutableListOf<String>()
            val errors = mutableListOf<String>()

            pending.forEach { entity ->
                runCatching {
                    firestoreSync.syncDailyGoalRecommendation(entity.recommendationId, entity)
                    syncedIds.add(entity.recommendationId)
                }.onFailure { e ->
                    errors.add("${entity.recommendationId}: ${e.message}")
                    Log.w(TAG, "DailyGoalRecommendation sync failed: ${entity.recommendationId} — ${e.message}")
                }
            }

            if (syncedIds.isNotEmpty()) {
                runCatching { dailyGoalDao.markRecommendationsSynced(userId, syncedIds) }
            }

            if (errors.isEmpty()) {
                syncBatchDao.markBatchSuccess(batchId, "SUCCESS", System.currentTimeMillis())
                Log.i(TAG, "DailyGoalRecommendations synced: ${syncedIds.size}/${pending.size}")
            } else {
                syncBatchDao.markBatchFailed(batchId, "FAILED", errors.joinToString("; "))
                Log.w(TAG, "DailyGoalRecommendations sync partial: ${syncedIds.size} ok, ${errors.size} failed | ${errors.joinToString("; ")}")
            }

            return@withContext SyncOutcome(
                attemptedCount = pending.size,
                successCount = syncedIds.size,
                failureCount = errors.size
            )
        }
    }

    override suspend fun syncPendingDailyGoalItems(userId: String): SyncOutcome {
        return syncPendingDailyGoalItems(userId, eligibleOnly = false)
    }

    private suspend fun syncPendingDailyGoalItems(userId: String, eligibleOnly: Boolean): SyncOutcome {
        return withContext(Dispatchers.IO) {
            val pending = runCatching {
                if (eligibleOnly) {
                    dailyGoalDao.getEligibleUnsyncedItems(userId, todayStartMillis())
                } else {
                    dailyGoalDao.getUnsyncedItems(userId)
                }
            }.getOrDefault(emptyList())
            Log.i(TAG, "DailyGoalItems pending: ${pending.size}")
            if (pending.isEmpty()) return@withContext SyncOutcome()

            val batchId = UUID.randomUUID().toString()
            val dateKey = dateFormat.format(Date())
            syncBatchDao.insertBatch(
                SyncBatchEntity(
                    batchId = batchId,
                    userId = userId,
                    dateKey = dateKey,
                    eventCount = pending.size,
                    status = "PENDING"
                )
            )

            val syncedIds = mutableListOf<String>()
            val errors = mutableListOf<String>()

            pending.forEach { entity ->
                runCatching {
                    firestoreSync.syncDailyGoalItem(entity.itemId, entity)
                    syncedIds.add(entity.itemId)
                }.onFailure { e ->
                    errors.add("${entity.itemId}: ${e.message}")
                    Log.w(TAG, "DailyGoalItem sync failed: ${entity.itemId} — ${e.message}")
                }
            }

            if (syncedIds.isNotEmpty()) {
                runCatching { dailyGoalDao.markItemsSynced(userId, syncedIds) }
            }

            if (errors.isEmpty()) {
                syncBatchDao.markBatchSuccess(batchId, "SUCCESS", System.currentTimeMillis())
                Log.i(TAG, "DailyGoalItems synced: ${syncedIds.size}/${pending.size}")
            } else {
                syncBatchDao.markBatchFailed(batchId, "FAILED", errors.joinToString("; "))
                Log.w(TAG, "DailyGoalItems sync partial: ${syncedIds.size} ok, ${errors.size} failed | ${errors.joinToString("; ")}")
            }

            return@withContext SyncOutcome(
                attemptedCount = pending.size,
                successCount = syncedIds.size,
                failureCount = errors.size
            )
        }
    }

    private suspend fun syncPendingExamChecks(userId: String): SyncOutcome {
        return withContext(Dispatchers.IO) {
            val pending = runCatching { examStrategyCheckDao.getUnsyncedChecks(userId) }.getOrDefault(emptyList())
            if (pending.isEmpty()) return@withContext SyncOutcome()

            val syncedIds = mutableListOf<String>()
            val errors = mutableListOf<String>()

            pending.forEach { check ->
                runCatching {
                    val data = buildMap<String, Any?> {
                        put("checkId", check.checkId)
                        put("userId", check.userId)
                        put("examTodoLegacyId", check.examTodoLegacyId)
                        put("subjectTitleSnapshot", check.subjectTitleSnapshot)
                        put("examDateMillis", check.examDateMillis)
                        put("strategyDValue", check.strategyDValue)
                        put("strategyLabelSnapshot", check.strategyLabelSnapshot)
                        put("checkedAtMillis", check.checkedAtMillis)
                        put("checkedOnDateKey", check.checkedOnDateKey)
                        put("checkedOnDaysUntilExam", check.checkedOnDaysUntilExam)
                        put("undoneAtMillis", check.undoneAtMillis) // null이면 정상 체크, non-null이면 undo됨
                    }
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(userId)
                        .collection("exam_strategy_checks").document(check.checkId)
                        .set(data)
                        .addOnSuccessListener {}
                        .addOnFailureListener { throw it }
                    examStrategyCheckDao.markCheckSynced(check.checkId)
                    syncedIds.add(check.checkId)
                }.onFailure { e -> errors.add("${check.checkId}: ${e.message}") }
            }

            Log.i(TAG, "ExamChecks synced: ${syncedIds.size}/${pending.size}")
            SyncOutcome(
                attemptedCount = pending.size,
                successCount = syncedIds.size,
                failureCount = errors.size
            )
        }
    }

    override suspend fun hasPendingSync(userId: String): Boolean {
        val cutoffMillis = todayStartMillis()
        val activityCount = runCatching { activityDao.getEligibleUnsyncedActivities(userId, cutoffMillis).size }.getOrDefault(0)
        val todoCount = runCatching { todoDao.getEligibleUnsyncedTodos(userId, cutoffMillis).size }.getOrDefault(0)
        val eventCount = runCatching { eventLogDao.getEligibleUnsyncedEvents(userId, cutoffMillis).size }.getOrDefault(0)
        val dailyGoalRecommendationCount = runCatching { dailyGoalDao.getEligibleUnsyncedRecommendations(userId, cutoffMillis).size }.getOrDefault(0)
        val dailyGoalItemCount = runCatching { dailyGoalDao.getEligibleUnsyncedItems(userId, cutoffMillis).size }.getOrDefault(0)
        return activityCount + todoCount + eventCount + dailyGoalRecommendationCount + dailyGoalItemCount > 0
    }

    private fun todayStartMillis(): Long {
        return java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private operator fun SyncOutcome.plus(other: SyncOutcome): SyncOutcome {
        return SyncOutcome(
            attemptedCount = attemptedCount + other.attemptedCount,
            successCount = successCount + other.successCount,
            failureCount = failureCount + other.failureCount,
            deferred = deferred || other.deferred
        )
    }

    companion object {
        private const val TAG = "FlowlogSync"
    }
}
