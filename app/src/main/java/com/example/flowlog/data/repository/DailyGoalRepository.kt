package com.example.flowlog.data.repository

import android.content.Context
import android.util.Log
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.example.flowlog.data.local.entity.DailyGoalRecommendationEntity
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.TodoItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class GoalItem(val todo: TodoItem, val reason: String)

class DailyGoalRepository(context: Context) {

    private val dao = FlowlogDatabase.getInstance(context).dailyGoalDao()
    private val eventLogRepository = EventLogRepository(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    fun todayDateKey(): String = dateFormat.format(Date())

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
                    todoId = "legacy_todo_${goalItem.todo.id}",
                    rank = index + 1,
                    reason = goalItem.reason,
                    todoSnapshotJson = runCatching {
                        json.encodeToString(goalItem.todo)
                    }.getOrNull(),
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

    suspend fun markItemClicked(dateKey: String, todoLegacyId: Long) {
        val recommendation = dao.getRecommendationByDate(userId, dateKey) ?: return
        dao.markItemClicked(
            recommendationId = recommendation.recommendationId,
            todoId = "legacy_todo_$todoLegacyId",
            updatedAt = System.currentTimeMillis()
        )
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

    companion object {
        private const val TAG = "DailyGoalRepository"
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
