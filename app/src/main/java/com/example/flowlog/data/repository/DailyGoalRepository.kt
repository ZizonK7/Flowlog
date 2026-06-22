package com.example.flowlog.data.repository

import android.content.Context
import android.util.Log
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.constants.RecommendationReason
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.agent.OrganizedPetite
import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.example.flowlog.data.local.entity.DailyGoalRecommendationEntity
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.RecommendedTodoBlock
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.TimetableProgress
import com.example.flowlog.notification.PlannedTodoReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.random.Random

data class GoalItem(
    val todo: TodoItem? = null,
    val petite: OrganizedPetite? = null,  // 캘린더 항목 전용; todo와 둘 중 하나만 non-null
    val reason: String,
    val burdenLevel: String? = null,
    val burdenGroupKey: String? = null,
    val burdenScore: Int? = null,
    val burdenReasonJson: String? = null
) {
    // DailyGoalItemEntity.todoId에 저장되는 키
    val entityTodoId: String get() = when {
        petite != null -> "calendar_petite_${petite.id}"
        todo != null -> "legacy_todo_${todo.id}"
        else -> error("GoalItem: todo와 petite 모두 null")
    }
}

@Serializable
private data class TimeBlockSnapshot(
    val category: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long
)

@Serializable
private data class DistributionSnapshot(
    val rawScoreByHour: List<Double>,
    val smoothedScoreByHour: List<Double>? = null,
    val workplaceMaskedScoreByHour: List<Double>,
    val restExcludedHours: List<Int> = emptyList(),
    val normalizedProbabilityByHour: List<Double>,
    val sampledHour: Int,
    val mode: String
)

@Serializable
private data class PlannedItemSnapshot(
    val itemId: String,
    val todoId: String,
    val title: String,
    val burdenLevel: String,
    val plannedStartMillis: Long,
    val plannedEndMillis: Long,
    val recommendedDurationMinutes: Int,
    val notificationScheduledAtMillis: Long?,
    val reason: String?
)

@Serializable
private data class ItemReplacedMetadata(
    val recommendationId: String,
    val oldItemId: String,
    val newItemId: String,
    val oldTodoId: String,
    val newTodoId: String,
    val oldTitle: String,
    val newTitle: String,
    val oldRank: Int?,
    val newRank: Int?,
    val oldReason: String?,
    val newReason: String?,
    val oldBurdenLevel: String?,
    val newBurdenLevel: String?,
    val oldPlannedStartMillis: Long,
    val newPlannedStartMillis: Long?,
    val oldPlannedEndMillis: Long,
    val newPlannedEndMillis: Long?,
    val replacedAtMillis: Long
)

class DailyGoalRepository(context: Context) {

    private val db = FlowlogDatabase.getInstance(context)
    private val dao = db.dailyGoalDao()
    private val autoButtonDao = db.autoButtonScheduleDao()
    private val todoDao = db.todoDao()
    private val petiteDao = db.organizedPetiteDao()
    private val eventLogRepository = EventLogRepository(context)
    private val plannedTodoReminderScheduler = PlannedTodoReminderScheduler(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    fun todayDateKey(): String = dateFormat.format(Date())

    fun observeTodayActiveTodoIds(dateKey: String = todayDateKey()): Flow<List<Long>> =
        dao.observeActiveItemsForDate(userId, dateKey)
            .map { items -> items.mapNotNull { it.todoId.removePrefix("legacy_todo_").toLongOrNull() } }

    fun observeTodayRecommendedBlocks(dateKey: String = todayDateKey()): Flow<List<RecommendedTodoBlock>> {
        return combine(
            dao.observePlannedItemsForDate(userId, dateKey),
            todoDao.observeAllTodos(userId),
            petiteDao.observeCalendarForUser(userId).catch { emit(emptyList()) },
            minuteTicker()
        ) { items, todos, calendarPetites, now ->
            val completedTodoIds = todos
                .filter { it.isCompleted }
                .map { it.todoId }
                .toSet()
            val calendarPetitesById = calendarPetites.associateBy { "calendar_petite_${it.id}" }
            items.mapNotNull { item ->
                val block = if (item.todoId.startsWith("calendar_petite_")) {
                    item.toRecommendedBlockFromPetite(calendarPetitesById[item.todoId])
                } else {
                    item.toRecommendedBlock()
                }
                if (block == null) return@mapNotNull null
                if (block.userActionStatus !in VISIBLE_RECOMMENDED_BLOCK_STATUSES ||
                    item.wasCompleted ||
                    item.wasSkipped ||
                    item.wasDeleted ||
                    item.todoId in completedTodoIds
                ) {
                    return@mapNotNull null
                }
                val expired = block.plannedStartMillis + HOUR_MILLIS <= now
                block.copy(isBubbleOnly = expired)
            }
        }
    }

    fun observeTodayTimetableProgress(dateKey: String = todayDateKey()): Flow<TimetableProgress> =
        dao.observePlannedItemsForDate(userId, dateKey).map { items ->
            TimetableProgress(
                total = items.count { !it.wasDeleted && !it.wasSkipped },
                completed = items.count {
                    !it.wasDeleted && !it.wasSkipped &&
                        (it.wasCompleted || it.userActionStatus == "COMPLETED")
                }
            )
        }

    suspend fun getTodayItems(dateKey: String = todayDateKey()): List<DailyGoalItemEntity> {
        return dao.getItemsForDate(userId, dateKey)
    }

    /**
     * 오늘의 목표 추천 결과를 저장.
     * refresh 시에도 새 row로 누적 저장 (audit trail).
     * [isRefresh] true이면 reason에 MANUAL_REFRESH 표기 포함.
     */
    suspend fun saveRecommendation(
        dateKey: String,
        selectedItems: List<GoalItem>,
        candidateTodos: List<TodoItem>,
        isRefresh: Boolean = false,
        algorithmVersion: String = "v1"
    ) {
        if (selectedItems.isEmpty()) return
        val now = System.currentTimeMillis()
        val currentUserId = userId

        val oldItems = dao.getItemsForDate(currentUserId, dateKey)
            .filter { it.userActionStatus in setOf("PLANNED", "RESCHEDULED") }
        oldItems.forEach { oldItem ->
            dao.updateItemActionStatus(currentUserId, oldItem.itemId, "DISMISSED", now)
            plannedTodoReminderScheduler.cancel(oldItem.itemId)
        }

        val recommendationId = UUID.randomUUID().toString()

        val reasonSummary = selectedItems.joinToString(",") { it.reason }
        val candidateSnapshotJson = runCatching {
            json.encodeToString(candidateTodos.map { it.id })
        }.getOrNull()

        dao.insertRecommendation(
            DailyGoalRecommendationEntity(
                recommendationId = recommendationId,
                userId = currentUserId,
                dateKey = dateKey,
                algorithmVersion = algorithmVersion,
                generatedAt = now,
                reasonSummary = if (isRefresh) "MANUAL_REFRESH,$reasonSummary" else reasonSummary,
                candidateSnapshotJson = candidateSnapshotJson,
                createdAt = now,
                syncStatus = SyncStatus.PENDING
            )
        )

        dao.insertGoalItems(
            selectedItems.mapIndexed { index, goalItem ->
                DailyGoalItemEntity(
                    itemId = UUID.randomUUID().toString(),
                    recommendationId = recommendationId,
                    userId = currentUserId,
                    todoId = goalItem.entityTodoId,
                    rank = index + 1,
                    reason = goalItem.reason,
                    todoSnapshotJson = goalItem.todo?.let { runCatching { json.encodeToString(it) }.getOrNull() },
                    burdenLevel = goalItem.burdenLevel,
                    burdenReasonJson = goalItem.burdenReasonJson,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            }
        )

        runCatching {
            eventLogRepository.log(
                eventType = if (isRefresh) EventType.DAILY_GOAL_REFRESHED else EventType.DAILY_GOAL_GENERATED,
                entityType = EntityType.DAILY_GOAL,
                entityId = recommendationId
            )
        }

        Log.d(TAG, "Saved recommendation $recommendationId (refresh=$isRefresh, items=${selectedItems.size})")
    }

    suspend fun ensureTodayTimePlan(
        dateKey: String = todayDateKey(),
        activities: List<ActivitySession>,
        forceRefresh: Boolean = false,
        recommendationModeOverride: String? = null
    ): Boolean {
        val currentUserId = userId
        val recommendation = dao.getRecommendationByDate(currentUserId, dateKey) ?: return false
        val items = dao.getGoalItems(recommendation.recommendationId)
        if (items.isEmpty()) return false
        if (!forceRefresh && items.any { it.plannedStartMillis != null && it.plannedEndMillis != null }) {
            return false
        }
        val plannableItems = items.filter { it.userActionStatus in REPLANNABLE_ACTION_STATUSES }
        if (plannableItems.isEmpty()) return false

        val dayStart = dateFormat.parse(dateKey)?.time ?: startOfDay(System.currentTimeMillis())
        val workplaceBlocks = todayWorkplaceBlocks(dayStart)
        val workplaceMaskedHours = maskedHours(dayStart, workplaceBlocks)
        val currentHour = ((System.currentTimeMillis() - dayStart) / HOUR_MILLIS).toInt().coerceIn(0, 23)
        val effectiveMaskedHours = workplaceMaskedHours + (0..currentHour).toSet()
        val pastWorkdayActivities = activitiesBeforeTodayOnWorkdays(activities, dayStart)
        val workdayCount = pastWorkdayActivities.keys.size
        val heavyMode = recommendationModeOverride ?: if (workdayCount < 5) {
            MODE_COLD_START_RANDOM
        } else {
            MODE_PERSONALIZED_DISTRIBUTION
        }

        val planned = mutableListOf<PlannedComputation>()
        val roles = selectTimePlanRoles(plannableItems)
        val firstPlan = roles.firstOrNull()?.let { role ->
            buildPlannedComputation(
                role = role,
                dayStart = dayStart,
                workplaceMaskedHours = effectiveMaskedHours,
                pastWorkdayActivities = pastWorkdayActivities.values.flatten(),
                historicalActivities = activities,
                workplaceBlocks = workplaceBlocks,
                heavyMode = heavyMode,
                recommendationModeOverride = recommendationModeOverride,
                excludedHours = emptySet()
            )
        }
        if (firstPlan != null) planned += firstPlan

        val secondPlan = roles.getOrNull(1)?.let { role ->
            val excluded = firstPlan?.let { first ->
                val firstHour = ((first.startMillis - dayStart) / HOUR_MILLIS).toInt()
                (0..(firstHour + 2).coerceAtMost(23)).toSet()
            } ?: emptySet()
            buildPlannedComputation(
                role = role,
                dayStart = dayStart,
                workplaceMaskedHours = effectiveMaskedHours,
                pastWorkdayActivities = pastWorkdayActivities.values.flatten(),
                historicalActivities = activities,
                workplaceBlocks = workplaceBlocks,
                heavyMode = heavyMode,
                recommendationModeOverride = recommendationModeOverride,
                excludedHours = excluded
            )
        }
        if (secondPlan != null) planned += secondPlan
        if (planned.isEmpty()) return false

        val now = System.currentTimeMillis()
        planned.forEach { plan ->
            dao.updateItemTimePlan(
                userId = currentUserId,
                itemId = plan.item.itemId,
                burdenLevel = plan.burdenLevel,
                plannedStartMillis = plan.startMillis,
                plannedEndMillis = plan.endMillis,
                recommendedDurationMinutes = plan.durationMinutes,
                notificationScheduledAtMillis = plan.notificationScheduledAtMillis,
                updatedAt = now
            )
            plannedTodoReminderScheduler.reschedule(plan.item.itemId)
        }

        val plannedSnapshots = planned.map { it.toSnapshot() }
        dao.updateRecommendationTimePlan(
            userId = currentUserId,
            recommendationId = recommendation.recommendationId,
            updatedAt = now,
            recommendationMode = recommendationModeOverride ?: heavyMode,
            workplaceDetected = workplaceBlocks.isNotEmpty(),
            workplaceBlocksJson = json.encodeToString(workplaceBlocks),
            selectedTodoIdsJson = json.encodeToString(items.map { it.todoId }),
            heavyTodoId = firstPlan?.item?.todoId,
            heavyBurdenLevel = firstPlan?.burdenLevel,
            heavyReason = firstPlan?.item?.reason,
            heavyDistributionSnapshotJson = firstPlan?.let { json.encodeToString(it.distribution) },
            lightTodoId = secondPlan?.item?.todoId,
            lightBurdenLevel = secondPlan?.burdenLevel,
            lightReason = secondPlan?.item?.reason,
            lightDistributionSnapshotJson = secondPlan?.let { json.encodeToString(it.distribution) },
            plannedItemsJson = json.encodeToString(plannedSnapshots)
        )

        Log.d(TAG, "Time plan saved recommendation=${recommendation.recommendationId}, mode=${recommendationModeOverride ?: heavyMode}, workplace=${workplaceBlocks.size}, planned=${planned.size}")
        return true
    }

    suspend fun dismissPlannedItem(itemId: String) {
        dao.updateItemActionStatus(userId, itemId, "DISMISSED", System.currentTimeMillis())
        plannedTodoReminderScheduler.cancel(itemId)
    }

    suspend fun reschedulePlannedItem(itemId: String) {
        dao.updateItemActionStatus(userId, itemId, "RESCHEDULED", System.currentTimeMillis())
        plannedTodoReminderScheduler.reschedule(itemId)
    }

    suspend fun setPlannedItemTime(itemId: String, startHourOfDay: Int) {
        val now = System.currentTimeMillis()
        val dayStart = startOfDay(now)
        val startMillis = dayStart + startHourOfDay * HOUR_MILLIS
        val endMillis = startMillis + HOUR_MILLIS
        val notificationScheduledAtMillis = startMillis.takeIf { it > now }
        Log.d(TAG, "setPlannedItemTime: itemId=$itemId startHour=$startHourOfDay plannedStart=${fmtTime(startMillis)} notifAt=${fmtTime(notificationScheduledAtMillis)}")
        dao.updateItemTimeManually(
            userId = userId,
            itemId = itemId,
            plannedStartMillis = startMillis,
            plannedEndMillis = endMillis,
            durationMinutes = 60,
            notificationScheduledAtMillis = notificationScheduledAtMillis,
            updatedAt = now
        )
        plannedTodoReminderScheduler.reschedule(itemId)
    }

    suspend fun replacePlannedItemTodo(
        block: RecommendedTodoBlock,
        newTodo: TodoItem,
        activities: List<ActivitySession>
    ): Boolean {
        val now = System.currentTimeMillis()

        // Fetch old item before dismissal to capture rank for inheritance
        val oldItem = dao.getGoalItem(block.itemId)
        val inheritedRank = oldItem?.rank ?: 1

        val newItemId = UUID.randomUUID().toString()

        // Dismiss old item — sets syncStatus = PENDING automatically
        dao.updateItemActionStatus(userId, block.itemId, "DISMISSED", now)
        plannedTodoReminderScheduler.cancel(block.itemId)

        // Insert new item: inherit rank from old, mark reason as USER_REPLACED
        dao.insertGoalItems(
            listOf(
                DailyGoalItemEntity(
                    itemId = newItemId,
                    recommendationId = block.recommendationId,
                    userId = userId,
                    todoId = "legacy_todo_${newTodo.id}",
                    rank = inheritedRank,
                    reason = RecommendationReason.USER_REPLACED,
                    todoSnapshotJson = runCatching { json.encodeToString(newTodo) }.getOrNull(),
                    burdenLevel = null,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )
        )

        Log.d(TAG, "replacePlannedItemTodo begin: oldItemId=${block.itemId} newItemId=$newItemId inheritedRank=$inheritedRank oldPlannedStart=${fmtTime(oldItem?.plannedStartMillis)} newTodoId=${newTodo.id} newPlannedStartBeforeRecompute=null")

        // Assign time slots — burdenLevel is computed here
        val result = ensureTodayTimePlan(activities = activities, forceRefresh = true)

        // Log EventLog after time plan so final plannedStart/EndMillis are captured
        runCatching {
            val newItem = dao.getGoalItem(newItemId)
            val rec = dao.getRecommendationByDate(userId, todayDateKey())
            Log.d(TAG, "replacePlannedItemTodo done: newItemId=$newItemId newPlannedStart=${fmtTime(newItem?.plannedStartMillis)} newNotifAt=${fmtTime(newItem?.notificationScheduledAtMillis)} result=$result mode=${rec?.recommendationMode} heavy=${rec?.heavyTodoId} light=${rec?.lightTodoId}")
            eventLogRepository.log(
                eventType = EventType.DAILY_GOAL_ITEM_REPLACED,
                entityType = EntityType.DAILY_GOAL_ITEM,
                entityId = block.itemId,
                metadataJson = json.encodeToString(
                    ItemReplacedMetadata(
                        recommendationId = block.recommendationId,
                        oldItemId = block.itemId,
                        newItemId = newItemId,
                        oldTodoId = "legacy_todo_${block.todoId}",
                        newTodoId = "legacy_todo_${newTodo.id}",
                        oldTitle = block.title,
                        newTitle = newTodo.title,
                        oldRank = oldItem?.rank,
                        newRank = newItem?.rank,
                        oldReason = block.reason,
                        newReason = newItem?.reason,
                        oldBurdenLevel = block.burdenLevel,
                        newBurdenLevel = newItem?.burdenLevel,
                        oldPlannedStartMillis = block.plannedStartMillis,
                        newPlannedStartMillis = newItem?.plannedStartMillis,
                        oldPlannedEndMillis = block.plannedEndMillis,
                        newPlannedEndMillis = newItem?.plannedEndMillis,
                        replacedAtMillis = now
                    )
                )
            )
        }

        return result
    }

    suspend fun markPlannedItemStarted(itemId: String) {
        dao.markPlannedItemStarted(userId, itemId, System.currentTimeMillis())
        plannedTodoReminderScheduler.cancel(itemId)
    }

    suspend fun revertStartedItem(itemId: String) {
        dao.revertStartedItem(userId, itemId, System.currentTimeMillis())
    }

    suspend fun markOpenPlannedItemCompleted(todoLegacyId: Long, activityLegacyId: Long) {
        dao.markOpenPlannedItemCompleted(
            userId = userId,
            todoId = "legacy_todo_$todoLegacyId",
            actualCompletedAt = System.currentTimeMillis(),
            linkedActivityId = activityLegacyId.toString(),
            completedTodoId = "legacy_todo_$todoLegacyId"
        )
    }

    suspend fun markPlannedItemCompleted(itemId: String, todoLegacyId: Long, activityLegacyId: Long?) {
        dao.markPlannedItemCompleted(
            userId = userId,
            itemId = itemId,
            actualCompletedAt = System.currentTimeMillis(),
            linkedActivityId = activityLegacyId?.toString(),
            completedTodoId = "legacy_todo_$todoLegacyId"
        )
        plannedTodoReminderScheduler.cancel(itemId)
    }

    suspend fun revertPlannedItemCompleted(itemId: String, restoredStatus: String) {
        dao.revertPlannedItemCompleted(
            userId = userId,
            itemId = itemId,
            restoredStatus = restoredStatus,
            updatedAt = System.currentTimeMillis()
        )
        plannedTodoReminderScheduler.reschedule(itemId)
    }

    /**
     * 오늘의 목표 항목 완료 처리. 가장 최근 추천에서 해당 todo를 완료로 표시.
     */
    suspend fun markItemCompleted(dateKey: String, todoLegacyId: Long) {
        val recommendation = dao.getRecommendationByDate(userId, dateKey) ?: return
        dao.markItemCompleted(
            recommendationId = recommendation.recommendationId,
            todoId = "legacy_todo_$todoLegacyId",
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 캘린더 petite가 완료/dismiss될 때, 그 petite를 가리키는 Anchors 추천 시간 블록도 함께 정리.
     * petiteId만으로 userId 전체 범위에서 매칭해 refresh로 누적된 row까지 모두 처리.
     */
    suspend fun markCalendarPetiteCompleted(petiteId: String) {
        dao.markCalendarPetiteItemsCompleted(
            userId = userId,
            todoId = "calendar_petite_$petiteId",
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun markItemClicked(dateKey: String, todoLegacyId: Long) {
        val recommendation = dao.getRecommendationByDate(userId, dateKey) ?: return
        dao.markItemClicked(
            recommendationId = recommendation.recommendationId,
            todoId = "legacy_todo_$todoLegacyId",
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun dismissItemsByTodoId(todoLegacyId: Long) {
        dao.dismissItemsByTodoId("legacy_todo_$todoLegacyId", System.currentTimeMillis())
    }

    suspend fun reconcilePastRecommendations(
        currentTodos: List<TodoItem>,
        activities: List<ActivitySession> = emptyList(),
        beforeDateKey: String = todayDateKey()
    ) {
        val currentByLegacyId = currentTodos.associateBy { "legacy_todo_${it.id}" }
        val now = System.currentTimeMillis()
        dao.getRecommendationsBeforeDate(userId, beforeDateKey).forEach { recommendation ->
            val dayStart = dateFormat.parse(recommendation.dateKey)?.time ?: return@forEach
            val dayEnd = dayStart + DAY_MILLIS
            dao.getGoalItems(recommendation.recommendationId).forEach { item ->
                if (item.wasCompleted || item.wasSkipped || item.wasDeleted) return@forEach
                val current = currentByLegacyId[item.todoId] ?: return@forEach
                val shownSnapshot = runCatching {
                    item.todoSnapshotJson?.let { json.decodeFromString<TodoItem>(it) }
                }.getOrNull()
                val startedByActivity = activities.any { activity ->
                    activity.linkedTodoId == current.id &&
                        activity.durationMillis > 0L &&
                        activity.startTime in dayStart until dayEnd
                }
                val startedByAccumulatedDelta = current.accumulatedSeconds > (shownSnapshot?.accumulatedSeconds ?: 0L)
                val startedOnShownDate = startedByActivity || (activities.isEmpty() && startedByAccumulatedDelta)
                val completedOnShownDate = current.completedAt?.let { it in dayStart until dayEnd } == true

                when {
                    completedOnShownDate -> dao.markItemCompletedPending(
                        recommendationId = recommendation.recommendationId,
                        todoId = item.todoId,
                        updatedAt = now
                    )
                    !startedOnShownDate -> dao.markItemSkippedPending(
                        recommendationId = recommendation.recommendationId,
                        todoId = item.todoId,
                        updatedAt = now
                    )
                }
            }
        }
    }

    suspend fun getTodayRecommendation(dateKey: String): DailyGoalRecommendationEntity? {
        return dao.getRecommendationByDate(userId, dateKey)
    }

    private data class PlannedComputation(
        val item: DailyGoalItemEntity,
        val burdenLevel: String,
        val startMillis: Long,
        val durationMinutes: Int,
        val distribution: DistributionSnapshot,
        val mode: String,
        val notificationScheduledAtMillis: Long?
    ) {
        val endMillis: Long = minOf(
            startMillis + TimeUnit.MINUTES.toMillis(durationMinutes.toLong()),
            Calendar.getInstance().apply {
                timeInMillis = startMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis - 1L
        )
    }

    private data class TimePlanRole(
        val item: DailyGoalItemEntity,
        val slot: TimePlanSlot
    )

    private enum class TimePlanSlot {
        HEAVY_LIKE,
        LIGHT_LIKE
    }

    private fun selectTimePlanRoles(items: List<DailyGoalItemEntity>): List<TimePlanRole> {
        val ranked = items.sortedBy { it.rank }
        if (ranked.size <= 1) {
            return ranked.map { item -> TimePlanRole(item, slotForSingleItem(burdenLevel(item))) }
        }

        val light = ranked.filter { burdenLevel(it) == "LIGHT" }
        val medium = ranked.filter { burdenLevel(it) == "MEDIUM" }
        val heavy = ranked.filter { burdenLevel(it) == "HEAVY" }

        return when {
            light.isNotEmpty() && heavy.isNotEmpty() -> listOf(
                TimePlanRole(heavy.first(), TimePlanSlot.HEAVY_LIKE),
                TimePlanRole(light.first(), TimePlanSlot.LIGHT_LIKE)
            )
            medium.size >= 2 -> listOf(
                TimePlanRole(medium[0], TimePlanSlot.HEAVY_LIKE),
                TimePlanRole(medium[1], TimePlanSlot.HEAVY_LIKE)
            )
            light.isNotEmpty() && medium.isNotEmpty() -> listOf(
                TimePlanRole(medium.first(), TimePlanSlot.HEAVY_LIKE),
                TimePlanRole(light.first(), TimePlanSlot.LIGHT_LIKE)
            )
            light.size >= 2 -> listOf(
                TimePlanRole(light[0], TimePlanSlot.LIGHT_LIKE),
                TimePlanRole(light[1], TimePlanSlot.LIGHT_LIKE)
            )
            else -> ranked.take(2).map { item ->
                TimePlanRole(item, slotForSingleItem(burdenLevel(item)))
            }
        }
    }

    private fun slotForSingleItem(level: String): TimePlanSlot {
        return if (level == "LIGHT") TimePlanSlot.LIGHT_LIKE else TimePlanSlot.HEAVY_LIKE
    }

    private fun buildPlannedComputation(
        role: TimePlanRole,
        dayStart: Long,
        workplaceMaskedHours: Set<Int>,
        pastWorkdayActivities: List<ActivitySession>,
        historicalActivities: List<ActivitySession>,
        workplaceBlocks: List<TimeBlockSnapshot>,
        heavyMode: String,
        recommendationModeOverride: String?,
        excludedHours: Set<Int>
    ): PlannedComputation {
        val level = burdenLevel(role.item)
        return when (role.slot) {
            TimePlanSlot.HEAVY_LIKE -> {
                val base = if (heavyMode == MODE_COLD_START_RANDOM) {
                    coldStartHeavyDistribution(workplaceMaskedHours)
                } else {
                    heavyDistribution(pastWorkdayActivities, workplaceMaskedHours)
                }
                val probabilities = probabilitiesWithExclusion(
                    scores = base.workplaceMaskedScoreByHour,
                    excludedHours = excludedHours,
                    maskedHours = workplaceMaskedHours,
                    preferredRange = 9..20
                )
                val hour = sampleHour(probabilities)
                val startMillis = dayStart + TimeUnit.HOURS.toMillis(hour.toLong())
                PlannedComputation(
                    item = role.item,
                    burdenLevel = level,
                    startMillis = startMillis,
                    durationMinutes = durationMinutesForBurden(level),
                    distribution = base.copy(
                        restExcludedHours = excludedHours.sorted(),
                        normalizedProbabilityByHour = probabilities,
                        sampledHour = hour,
                        mode = heavyMode
                    ),
                    mode = heavyMode,
                    notificationScheduledAtMillis = startMillis.takeIf { it > System.currentTimeMillis() }
                )
            }
            TimePlanSlot.LIGHT_LIKE -> {
                val base = lightDistribution(historicalActivities.filter { it.startTime < dayStart }, workplaceMaskedHours)
                val probabilities = probabilitiesWithExclusion(
                    scores = base.workplaceMaskedScoreByHour,
                    excludedHours = excludedHours,
                    maskedHours = workplaceMaskedHours,
                    preferredRange = 0..23
                )
                val hour = sampleHour(probabilities)
                val startMillis = dayStart + TimeUnit.HOURS.toMillis(hour.toLong())
                PlannedComputation(
                    item = role.item,
                    burdenLevel = level,
                    startMillis = startMillis,
                    durationMinutes = durationMinutesForBurden(level),
                    distribution = base.copy(
                        restExcludedHours = excludedHours.sorted(),
                        normalizedProbabilityByHour = probabilities,
                        sampledHour = hour,
                        mode = recommendationModeOverride ?: MODE_PERSONALIZED_DISTRIBUTION
                    ),
                    mode = recommendationModeOverride ?: MODE_PERSONALIZED_DISTRIBUTION,
                    notificationScheduledAtMillis = startMillis.takeIf { it > System.currentTimeMillis() }
                )
            }
        }
    }

    private fun PlannedComputation.toSnapshot(): PlannedItemSnapshot {
        return PlannedItemSnapshot(
            itemId = item.itemId,
            todoId = item.todoId,
            title = item.displayTitle(),
            burdenLevel = burdenLevel,
            plannedStartMillis = startMillis,
            plannedEndMillis = endMillis,
            recommendedDurationMinutes = durationMinutes,
            notificationScheduledAtMillis = notificationScheduledAtMillis,
            reason = item.reason
        )
    }

    private fun DailyGoalItemEntity.toRecommendedBlock(): RecommendedTodoBlock? {
        val start = plannedStartMillis ?: return null
        val end = plannedEndMillis ?: return null
        val legacyId = todoId.removePrefix("legacy_todo_").toLongOrNull() ?: return null
        val todoSnapshot = decodedTodoSnapshot()
        return RecommendedTodoBlock(
            itemId = itemId,
            recommendationId = recommendationId,
            todoId = legacyId,
            petiteId = null,
            title = todoSnapshot?.title ?: displayTitle(),
            category = todoSnapshot?.category,
            selectedDate = todoSnapshot?.selectedDate,
            burdenLevel = burdenLevel ?: burdenLevel(this),
            reason = reason,
            plannedStartMillis = start,
            plannedEndMillis = end,
            recommendedDurationMinutes = recommendedDurationMinutes ?: ((end - start) / 60_000L).toInt().coerceAtLeast(1),
            userActionStatus = userActionStatus,
            notificationScheduledAtMillis = notificationScheduledAtMillis
        )
    }

    private fun DailyGoalItemEntity.toRecommendedBlockFromPetite(
        petiteEntity: OrganizedPetiteEntity?
    ): RecommendedTodoBlock? {
        val start = plannedStartMillis ?: return null
        val end = plannedEndMillis ?: return null
        val petiteId = todoId.removePrefix("calendar_petite_").takeIf { it != todoId } ?: return null
        val parsedCategory = petiteEntity?.category?.let {
            runCatching { TodoCategory.valueOf(it) }.getOrNull()
        }
        return RecommendedTodoBlock(
            itemId = itemId,
            recommendationId = recommendationId,
            todoId = 0L,
            petiteId = petiteId,
            title = petiteEntity?.title ?: "캘린더 항목",
            category = parsedCategory,
            selectedDate = petiteEntity?.dateMillis,
            burdenLevel = burdenLevel ?: "MEDIUM",
            reason = reason,
            plannedStartMillis = start,
            plannedEndMillis = end,
            recommendedDurationMinutes = recommendedDurationMinutes ?: ((end - start) / 60_000L).toInt().coerceAtLeast(1),
            userActionStatus = userActionStatus,
            notificationScheduledAtMillis = notificationScheduledAtMillis
        )
    }

    private fun DailyGoalItemEntity.displayTitle(): String {
        return decodedTodoSnapshot()?.title ?: todoId.removePrefix("legacy_todo_")
    }

    private fun DailyGoalItemEntity.decodedTodoSnapshot(): TodoItem? {
        return runCatching {
            todoSnapshotJson?.let { json.decodeFromString<TodoItem>(it) }
        }.getOrNull()
    }

    private suspend fun todayWorkplaceBlocks(dayStart: Long): List<TimeBlockSnapshot> {
        val dayOfWeek = Calendar.getInstance().apply { timeInMillis = dayStart }.get(Calendar.DAY_OF_WEEK)
        val blocks = mutableListOf<TimeBlockSnapshot>()
        autoButtonDao.getActiveSchedules(userId).forEach { schedule ->
            val isWorkplaceToday = schedule.category in WORKPLACE_CATEGORIES &&
                dayOfWeek in repeatDays(schedule.repeatDaysMask) &&
                autoButtonDao.getSkipDate(schedule.scheduleId, dayStart) == null
            if (isWorkplaceToday) {
                blocks += TimeBlockSnapshot(
                    category = schedule.category,
                    title = schedule.title,
                    startMillis = dayStart + schedule.startMinuteOfDay * 60_000L,
                    endMillis = dayStart + schedule.endMinuteOfDay * 60_000L
                )
            }
        }
        return blocks
    }

    private fun repeatDays(mask: Int): Set<Int> {
        return (1..7).filter { day -> mask and (1 shl day) != 0 }.toSet()
    }

    private fun maskedHours(dayStart: Long, blocks: List<TimeBlockSnapshot>): Set<Int> {
        return (0..23).filter { hour ->
            val hourStart = dayStart + hour * HOUR_MILLIS
            val hourEnd = hourStart + HOUR_MILLIS
            blocks.any { block -> hourStart < block.endMillis && hourEnd > block.startMillis }
        }.toSet()
    }

    private fun activitiesBeforeTodayOnWorkdays(
        activities: List<ActivitySession>,
        todayStart: Long
    ): Map<Long, List<ActivitySession>> {
        return activities
            .filter { it.startTime < todayStart && it.durationMillis > 0L }
            .groupBy { startOfDay(it.startTime) }
            .filterValues { dayActivities ->
                dayActivities.any { it.category in WORKPLACE_CATEGORIES }
            }
    }

    private fun coldStartHeavyDistribution(maskedHours: Set<Int>): DistributionSnapshot {
        val raw = (0..23).map { if (it in 9..20) 1.0 else 0.0 }
        val masked = raw.mapIndexed { hour, score -> if (hour in maskedHours) 0.0 else score }
        return DistributionSnapshot(
            rawScoreByHour = raw,
            smoothedScoreByHour = raw,
            workplaceMaskedScoreByHour = masked,
            normalizedProbabilityByHour = normalizeOrFallback(masked, maskedHours, preferredRange = 9..20),
            sampledHour = 9,
            mode = MODE_COLD_START_RANDOM
        )
    }

    private fun heavyDistribution(
        activities: List<ActivitySession>,
        maskedHours: Set<Int>
    ): DistributionSnapshot {
        val productive = hourlyCategoryMillis(activities, HEAVY_PRODUCTIVE_CATEGORIES)
        val denominator = hourlyCategoryMillis(activities, HEAVY_DENOMINATOR_CATEGORIES)
        val raw = (0..23).map { hour ->
            val total = denominator[hour]
            if (total <= 0L) 0.0 else productive[hour].toDouble() / total.toDouble()
        }
        val smoothed = smooth(raw)
        val masked = smoothed.mapIndexed { hour, score -> if (hour in maskedHours) 0.0 else score }
        return DistributionSnapshot(
            rawScoreByHour = raw,
            smoothedScoreByHour = smoothed,
            workplaceMaskedScoreByHour = masked,
            normalizedProbabilityByHour = normalizeOrFallback(masked, maskedHours, preferredRange = 9..20),
            sampledHour = 9,
            mode = MODE_PERSONALIZED_DISTRIBUTION
        )
    }

    private fun lightDistribution(
        activities: List<ActivitySession>,
        maskedHours: Set<Int>
    ): DistributionSnapshot {
        val rest = hourlyCategoryMillis(activities, setOf("REST"))
        val denominator = hourlyCategoryMillis(activities, LIGHT_DENOMINATOR_CATEGORIES)
        val raw = (0..23).map { hour ->
            val total = denominator[hour]
            if (total <= 0L) 0.0 else rest[hour].toDouble() / total.toDouble()
        }
        val masked = raw.mapIndexed { hour, score -> if (hour in maskedHours) 0.0 else score }
        return DistributionSnapshot(
            rawScoreByHour = raw,
            workplaceMaskedScoreByHour = masked,
            normalizedProbabilityByHour = normalizeOrFallback(masked, maskedHours, preferredRange = 0..23),
            sampledHour = 12,
            mode = MODE_PERSONALIZED_DISTRIBUTION
        )
    }

    private fun hourlyCategoryMillis(activities: List<ActivitySession>, categories: Set<String>): LongArray {
        val result = LongArray(24)
        activities.filter { it.category in categories && it.durationMillis > 0L }.forEach { activity ->
            var cursor = activity.startTime
            val end = max(activity.endTime, activity.startTime + activity.durationMillis)
            while (cursor < end) {
                val hourStart = startOfHour(cursor)
                val hourEnd = hourStart + HOUR_MILLIS
                val overlapEnd = minOf(end, hourEnd)
                val hour = Calendar.getInstance().apply { timeInMillis = cursor }.get(Calendar.HOUR_OF_DAY)
                result[hour] += (overlapEnd - cursor).coerceAtLeast(0L)
                cursor = overlapEnd
            }
        }
        return result
    }

    private fun smooth(values: List<Double>): List<Double> {
        return values.mapIndexed { index, value ->
            val previous = values.getOrElse(index - 1) { value }
            val next = values.getOrElse(index + 1) { value }
            previous * 0.25 + value * 0.5 + next * 0.25
        }
    }

    private fun applyLightExclusion(scores: List<Double>, excludedHours: Set<Int>): List<Double> {
        return scores.mapIndexed { hour, score -> if (hour in excludedHours) 0.0 else score }
    }

    private fun probabilitiesWithExclusion(
        scores: List<Double>,
        excludedHours: Set<Int>,
        maskedHours: Set<Int>,
        preferredRange: IntRange
    ): List<Double> {
        val relaxed = applyLightExclusion(scores, excludedHours)
        val usableScores = if (relaxed.sum() <= 0.0) scores else relaxed
        return normalizeOrFallback(usableScores, maskedHours, preferredRange)
    }

    private fun normalizeOrFallback(
        scores: List<Double>,
        maskedHours: Set<Int>,
        preferredRange: IntRange
    ): List<Double> {
        val sum = scores.sum()
        if (sum > 0.0) return scores.map { it / sum }

        val fallback = (0..23).map { hour ->
            if (hour in preferredRange && hour !in maskedHours) 1.0 else 0.0
        }
        val fallbackSum = fallback.sum()
        if (fallbackSum > 0.0) return fallback.map { it / fallbackSum }

        val relaxed = (0..23).map { hour -> if (hour !in maskedHours) 1.0 else 0.0 }
        val relaxedSum = relaxed.sum().takeIf { it > 0.0 } ?: 1.0
        return relaxed.map { it / relaxedSum }
    }

    private fun sampleHour(probabilities: List<Double>): Int {
        val target = Random(System.currentTimeMillis()).nextDouble()
        var cursor = 0.0
        probabilities.forEachIndexed { hour, probability ->
            cursor += probability
            if (target <= cursor) return hour
        }
        return probabilities.indexOfLast { it > 0.0 }.takeIf { it >= 0 } ?: 9
    }

    private fun burdenLevel(item: DailyGoalItemEntity): String {
        item.burdenLevel?.let { return it }
        val todo = runCatching {
            item.todoSnapshotJson?.let { json.decodeFromString<TodoItem>(it) }
        }.getOrNull()
        val ageDays = todo?.let {
            ((startOfDay(System.currentTimeMillis()) - startOfDay(it.createdAt)) / DAY_MILLIS).coerceAtLeast(0L)
        } ?: 0L
        val workHours = todo?.accumulatedSeconds?.div(3600L) ?: 0L
        return when {
            ageDays >= 7L || workHours >= 3L -> "HEAVY"
            ageDays <= 1L && workHours == 0L -> "LIGHT"
            else -> "MEDIUM"
        }
    }

    private fun burdenWeight(level: String): Int {
        return when (level) {
            "HEAVY" -> 2
            "MEDIUM" -> 1
            else -> 0
        }
    }

    private fun durationMinutesForBurden(level: String): Int {
        return when (level) {
            "HEAVY" -> HEAVY_DURATION_MINUTES
            "MEDIUM" -> MEDIUM_DURATION_MINUTES
            else -> LIGHT_DURATION_MINUTES
        }
    }

    private fun startOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfHour(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        private const val TAG = "DailyGoalRepository"
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
        private const val MINUTE_MILLIS = 60L * 1000L
        private const val HOUR_MILLIS = 60L * 60L * 1000L
        private const val HEAVY_DURATION_MINUTES = 90
        private const val MEDIUM_DURATION_MINUTES = 60
        private const val LIGHT_DURATION_MINUTES = 45
        private const val MODE_COLD_START_RANDOM = "COLD_START_RANDOM"
        private const val MODE_PERSONALIZED_DISTRIBUTION = "PERSONALIZED_DISTRIBUTION"
        const val MODE_MANUAL_REFRESH = "MANUAL_REFRESH"
        private val WORKPLACE_CATEGORIES = setOf("SCHOOL", "WORK", "COMPANY")
        private val HEAVY_PRODUCTIVE_CATEGORIES = setOf("TODO", "STUDY", "DEVELOPMENT", "WORK", "COMPANY")
        private val HEAVY_DENOMINATOR_CATEGORIES = setOf("TODO", "STUDY", "DEVELOPMENT", "WORK", "COMPANY", "REST", "SLEEP", "SCHOOL")
        private val LIGHT_DENOMINATOR_CATEGORIES = setOf("TODO", "STUDY", "DEVELOPMENT", "WORK", "COMPANY", "REST", "SCHOOL")
        private val REPLANNABLE_ACTION_STATUSES = setOf("PLANNED", "RESCHEDULED")
        private val VISIBLE_RECOMMENDED_BLOCK_STATUSES = setOf("PLANNED", "RESCHEDULED", "STARTED")

        private val logFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        private fun fmtTime(ms: Long?): String = ms?.let { logFmt.format(Date(it)) } ?: "null"

        private fun minuteTicker(): Flow<Long> = flow {
            while (true) {
                val now = System.currentTimeMillis()
                emit(now)
                delay(MINUTE_MILLIS - (now % MINUTE_MILLIS))
            }
        }

    }
}
