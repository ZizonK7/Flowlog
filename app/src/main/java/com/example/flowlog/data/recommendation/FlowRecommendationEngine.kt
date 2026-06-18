package com.example.flowlog.data.recommendation

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.DailyCueRecommendationTiming
import com.example.flowlog.data.model.RecommendedTodoBlock
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.repository.DailyCueRecord
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class FlowRecommendationSource {
    ROUTINE,
    TIMETABLE_TODO,
    TODAY_TODO,
    PETITE
}

data class FlowRecommendationDecision(
    val source: FlowRecommendationSource,
    val step: Int,
    val reasonCode: String,
    val routine: DailyCueRecord? = null,
    val timetableTodo: RecommendedTodoBlock? = null,
    val todayTodo: TodoItem? = null
)

data class TimetableProgress(
    val total: Int = 0,
    val completed: Int = 0
) {
    val completionRatio: Double
        get() = if (total == 0) 1.0 else completed.toDouble() / total.toDouble()
}

class FlowRecommendationEngine {
    fun recommend(
        now: Long,
        routines: List<DailyCueRecord>,
        completedRoutineIds: Set<Long>,
        timetableTodos: List<RecommendedTodoBlock>,
        timetableProgress: TimetableProgress,
        todayTodos: List<TodoItem>,
        activities: List<ActivitySession>
    ): FlowRecommendationDecision? {
        val remainingRoutines = routines.filter {
            it.archivedAt == null &&
                it.label == "Routine" &&
                it.id !in completedRoutineIds
        }
        val remainingTimetable = timetableTodos.filter {
            it.userActionStatus in ACTIVE_TIMETABLE_STATUSES
        }
        val remainingTodayTodos = todayTodos.filterNot { it.isCompleted }
        val lastSession = activities
            .asSequence()
            .filter { it.endTime in 1..now }
            .maxByOrNull { it.endTime }
        val nextLongSleepStart = estimateNextLongSleepStart(now, activities)
        val millisUntilLongSleep = nextLongSleepStart?.minus(now)
        val minutesSinceLastSession = lastSession?.let { now - it.endTime }

        routineFor(remainingRoutines, DailyCueRecommendationTiming.AFTER_WAKING)?.let { routine ->
            if (
                lastSession?.category == "SLEEP" &&
                lastSession.durationMillis >= MIN_LONG_SLEEP_MILLIS &&
                minutesSinceLastSession in 0..AFTER_WAKING_WINDOW_MILLIS
            ) {
                return FlowRecommendationDecision(
                    source = FlowRecommendationSource.ROUTINE,
                    step = 1,
                    reasonCode = "AFTER_WAKING",
                    routine = routine
                )
            }
        }

        remainingTimetable.firstOrNull {
            now in it.plannedStartMillis..it.plannedEndMillis
        }?.let {
            return FlowRecommendationDecision(
                source = FlowRecommendationSource.TIMETABLE_TODO,
                step = 2,
                reasonCode = "TIMETABLE_WINDOW",
                timetableTodo = it
            )
        }

        routineFor(remainingRoutines, DailyCueRecommendationTiming.SOFT_TRANSITION)?.let { routine ->
            val afterMeal = lastSession?.category == "MEAL" &&
                minutesSinceLastSession in 0..SOFT_BUFFER_WINDOW_MILLIS
            val beforeSleep = millisUntilLongSleep in 0..SOFT_BUFFER_WINDOW_MILLIS
            if (afterMeal || beforeSleep) {
                return FlowRecommendationDecision(
                    source = FlowRecommendationSource.ROUTINE,
                    step = 3,
                    reasonCode = if (afterMeal) "AFTER_MEAL" else "BEFORE_LONG_SLEEP",
                    routine = routine
                )
            }
        }

        routineFor(remainingRoutines, DailyCueRecommendationTiming.FLOW_RESET)?.let { routine ->
            val interrupted = when (lastSession?.category) {
                "REST" -> lastSession.durationMillis >= RESET_REST_MILLIS
                "GAME" -> lastSession.durationMillis >= RESET_GAME_MILLIS
                else -> false
            }
            val justWokeUp = lastSession?.category == "SLEEP" &&
                minutesSinceLastSession in 0..AFTER_WAKING_WINDOW_MILLIS
            val nearSleep = millisUntilLongSleep in 0..SOFT_BUFFER_WINDOW_MILLIS
            if (interrupted && !justWokeUp && !nearSleep) {
                return FlowRecommendationDecision(
                    source = FlowRecommendationSource.ROUTINE,
                    step = 4,
                    reasonCode = "FLOW_INTERRUPTED",
                    routine = routine
                )
            }
        }

        routineFor(remainingRoutines, DailyCueRecommendationTiming.AFTER_MAIN_TASKS)?.let { routine ->
            val timetableReady = timetableProgress.total == 0 ||
                timetableProgress.completionRatio >= REWARD_COMPLETION_RATIO
            val coreFocusCompleted = routines
                .filter { it.recommendationTiming == DailyCueRecommendationTiming.FOCUS_READY.name }
                .any { it.id in completedRoutineIds }
            val productiveMillis = activities
                .filter { it.startTime >= startOfDay(now) && it.category in PRODUCTIVE_CATEGORIES }
                .sumOf { it.durationMillis.coerceAtLeast(0L) }
            val earnedReward = coreFocusCompleted || productiveMillis >= REWARD_PRODUCTIVE_MILLIS
            val enoughTimeBeforeSleep = millisUntilLongSleep == null ||
                millisUntilLongSleep > SOFT_BUFFER_WINDOW_MILLIS
            if (timetableReady && earnedReward && enoughTimeBeforeSleep) {
                return FlowRecommendationDecision(
                    source = FlowRecommendationSource.ROUTINE,
                    step = 5,
                    reasonCode = "REWARD_EARNED",
                    routine = routine
                )
            }
        }

        val generalRoutines = remainingRoutines.filter {
            it.recommendationTiming == DailyCueRecommendationTiming.FOCUS_READY.name ||
                it.recommendationTiming == DailyCueRecommendationTiming.AFTER_FOCUS.name
        }
        stableRandomChoice(generalRoutines, now)?.let {
            return FlowRecommendationDecision(
                source = FlowRecommendationSource.ROUTINE,
                step = 6,
                reasonCode = "GENERAL_ROUTINE_SLOT",
                routine = it
            )
        }

        val todayStart = startOfDay(now)
        remainingTodayTodos.firstOrNull {
            it.category == TodoCategory.TODAY ||
                it.selectedDate?.let { date -> startOfDay(date) == todayStart } == true
        }?.let {
            return FlowRecommendationDecision(
                source = FlowRecommendationSource.TODAY_TODO,
                step = 7,
                reasonCode = "TODAY_TODO_REMAINS",
                todayTodo = it
            )
        }

        stableRandomChoice(remainingRoutines, now)?.let {
            return FlowRecommendationDecision(
                source = FlowRecommendationSource.ROUTINE,
                step = 8,
                reasonCode = "ROUTINE_FALLBACK",
                routine = it
            )
        }
        return null
    }

    private fun routineFor(
        routines: List<DailyCueRecord>,
        timing: DailyCueRecommendationTiming
    ): DailyCueRecord? = routines.firstOrNull { it.recommendationTiming == timing.name }

    private fun stableRandomChoice(items: List<DailyCueRecord>, now: Long): DailyCueRecord? {
        if (items.isEmpty()) return null
        val seed = startOfDay(now) xor items.joinToString("|") { it.id.toString() }.hashCode().toLong()
        return items[Random(seed).nextInt(items.size)]
    }

    private fun estimateNextLongSleepStart(now: Long, activities: List<ActivitySession>): Long? {
        val starts = activities
            .asSequence()
            .filter { it.category == "SLEEP" && it.durationMillis >= MIN_LONG_SLEEP_MILLIS }
            .filter { it.startTime >= now - SLEEP_HISTORY_MILLIS }
            .map { minuteOfDay(it.startTime) }
            .sorted()
            .toList()
        if (starts.isEmpty()) return null
        val estimatedMinute = starts[starts.size / 2]
        val candidate = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, estimatedMinute / 60)
            set(Calendar.MINUTE, estimatedMinute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        return candidate.timeInMillis
    }

    private fun minuteOfDay(millis: Long): Int = Calendar.getInstance().run {
        timeInMillis = millis
        get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        val MIN_LONG_SLEEP_MILLIS = TimeUnit.MINUTES.toMillis(90)
        val AFTER_WAKING_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(30)
        val SOFT_BUFFER_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(60)
        val RESET_REST_MILLIS = TimeUnit.MINUTES.toMillis(120)
        val RESET_GAME_MILLIS = TimeUnit.MINUTES.toMillis(90)
        val REWARD_PRODUCTIVE_MILLIS = TimeUnit.MINUTES.toMillis(75)
        val SLEEP_HISTORY_MILLIS = TimeUnit.DAYS.toMillis(14)
        const val REWARD_COMPLETION_RATIO = 0.7
        val PRODUCTIVE_CATEGORIES = setOf("TODO", "STUDY", "DEVELOPMENT", "WORK", "COMPANY", "SCHOOL")
        val ACTIVE_TIMETABLE_STATUSES = setOf("PLANNED", "RESCHEDULED", "STARTED")
    }
}
