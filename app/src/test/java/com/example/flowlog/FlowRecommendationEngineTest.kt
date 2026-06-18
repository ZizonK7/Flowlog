package com.example.flowlog

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.DailyCueRecommendationTiming
import com.example.flowlog.data.model.RecommendedTodoBlock
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.FlowRecommendationEngine
import com.example.flowlog.data.recommendation.FlowRecommendationSource
import com.example.flowlog.data.recommendation.TimetableProgress
import com.example.flowlog.data.repository.DailyCueRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class FlowRecommendationEngineTest {
    private val engine = FlowRecommendationEngine()
    private val now = Calendar.getInstance().apply {
        set(2026, Calendar.JUNE, 18, 9, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun wakingRoutineWinsBeforeTimetableTodo() {
        val result = recommend(
            routines = listOf(routine(1, DailyCueRecommendationTiming.AFTER_WAKING)),
            timetableTodos = listOf(timetable(now - 1_000, now + 60_000)),
            activities = listOf(session("SLEEP", now - minutes(120), now - minutes(10)))
        )

        assertEquals(1, result?.step)
        assertEquals(FlowRecommendationSource.ROUTINE, result?.source)
    }

    @Test
    fun timetableTodoWinsWhenCurrentTimeIsInsideWindow() {
        val result = recommend(
            timetableTodos = listOf(timetable(now - 1_000, now + 60_000))
        )

        assertEquals(2, result?.step)
        assertEquals(FlowRecommendationSource.TIMETABLE_TODO, result?.source)
    }

    @Test
    fun mealTriggersSoftBuffer() {
        val result = recommend(
            routines = listOf(routine(2, DailyCueRecommendationTiming.SOFT_TRANSITION)),
            activities = listOf(session("MEAL", now - minutes(40), now - minutes(20)))
        )

        assertEquals(3, result?.step)
        assertEquals("AFTER_MEAL", result?.reasonCode)
    }

    @Test
    fun longRestTriggersResetAction() {
        val result = recommend(
            routines = listOf(routine(3, DailyCueRecommendationTiming.FLOW_RESET)),
            activities = listOf(session("REST", now - minutes(150), now - minutes(5)))
        )

        assertEquals(4, result?.step)
    }

    @Test
    fun productiveDayAndCompletedTimetableTriggerReward() {
        val result = recommend(
            routines = listOf(routine(4, DailyCueRecommendationTiming.AFTER_MAIN_TASKS)),
            timetableProgress = TimetableProgress(total = 10, completed = 7),
            activities = listOf(session("STUDY", now - minutes(80), now))
        )

        assertEquals(5, result?.step)
    }

    @Test
    fun generalSlotChoosesCoreOrExtension() {
        val result = recommend(
            routines = listOf(
                routine(5, DailyCueRecommendationTiming.FOCUS_READY),
                routine(6, DailyCueRecommendationTiming.AFTER_FOCUS)
            )
        )

        assertEquals(6, result?.step)
        assertEquals(FlowRecommendationSource.ROUTINE, result?.source)
    }

    @Test
    fun completedRoutineIsExcludedAndTodayTodoIsUsed() {
        val result = recommend(
            routines = listOf(routine(7, DailyCueRecommendationTiming.FOCUS_READY)),
            completedRoutineIds = setOf(7),
            todayTodos = listOf(TodoItem(id = 8, title = "오늘 할 일", category = TodoCategory.TODAY))
        )

        assertEquals(7, result?.step)
        assertEquals(FlowRecommendationSource.TODAY_TODO, result?.source)
    }

    @Test
    fun noCandidatesReturnsNoRecommendation() {
        assertNull(recommend())
    }

    private fun recommend(
        routines: List<DailyCueRecord> = emptyList(),
        completedRoutineIds: Set<Long> = emptySet(),
        timetableTodos: List<RecommendedTodoBlock> = emptyList(),
        timetableProgress: TimetableProgress = TimetableProgress(),
        todayTodos: List<TodoItem> = emptyList(),
        activities: List<ActivitySession> = emptyList()
    ) = engine.recommend(
        now = now,
        routines = routines,
        completedRoutineIds = completedRoutineIds,
        timetableTodos = timetableTodos,
        timetableProgress = timetableProgress,
        todayTodos = todayTodos,
        activities = activities
    )

    private fun routine(id: Long, timing: DailyCueRecommendationTiming) = DailyCueRecord(
        id = id,
        label = "Routine",
        title = "Routine $id",
        timerDurationMillis = null,
        timerCategory = "TODO",
        recommendationTiming = timing.name,
        note = "",
        order = id.toInt(),
        createdAt = now,
        updatedAt = now
    )

    private fun timetable(start: Long, end: Long) = RecommendedTodoBlock(
        itemId = "item",
        recommendationId = "daily",
        todoId = 10,
        petiteId = null,
        title = "시간표 할 일",
        burdenLevel = "MEDIUM",
        reason = null,
        plannedStartMillis = start,
        plannedEndMillis = end,
        recommendedDurationMinutes = 60,
        userActionStatus = "PLANNED"
    )

    private fun session(category: String, start: Long, end: Long) = ActivitySession(
        category = category,
        title = category,
        startTime = start,
        endTime = end,
        durationMillis = end - start
    )

    private fun minutes(value: Long): Long = TimeUnit.MINUTES.toMillis(value)
}
