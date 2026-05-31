package com.example.flowlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowlog.data.constants.RecommendationReason
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.DailyGoalRepository
import com.example.flowlog.data.repository.GoalItem
import com.example.flowlog.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class YesterdayFlowSuggestion(
    val message: String,
    val actionLabel: String,
    val actionCategory: String
)

class TodoViewModel(
    private val repository: TodoRepository,
    context: Context
) : ViewModel() {

    private val focusPrefs = context.getSharedPreferences("todo_focus", Context.MODE_PRIVATE)
    private val dailyGoalRepository = DailyGoalRepository(context.applicationContext)
    private val activityRepository = ActivityRepository(context.applicationContext)

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    private val _focusIds = MutableStateFlow<List<Long>>(emptyList())
    private val _yesterdaySuggestion = MutableStateFlow<YesterdayFlowSuggestion?>(null)
    val yesterdaySuggestion: StateFlow<YesterdayFlowSuggestion?> = _yesterdaySuggestion.asStateFlow()
    private var latestActivities: List<ActivitySession> = emptyList()

    val todayFocusItems: StateFlow<List<TodoItem>> = combine(_todos, _focusIds) { todos, ids ->
        val idToIndex = ids.mapIndexed { i, id -> id to i }.toMap()
        todos.filter { it.id in idToIndex }
            .sortedWith(compareBy(
                { if (it.isCompleted) 1 else 0 },
                { idToIndex[it.id] ?: Int.MAX_VALUE }
            ))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.getAllTodos().collect { todos ->
                _todos.value = todos
                _yesterdaySuggestion.value = buildYesterdaySuggestion(todos, latestActivities)
                if (_focusIds.value.isEmpty() && todos.isNotEmpty()) {
                    initFocusIds(todos)
                }
            }
        }
        viewModelScope.launch {
            activityRepository.getAllActivities().collect { activities ->
                latestActivities = activities
                _yesterdaySuggestion.value = buildYesterdaySuggestion(_todos.value, activities)
            }
        }
    }

    private suspend fun initFocusIds(allTodos: List<TodoItem>) {
        val todayKey = startOfDay(System.currentTimeMillis())
        val storedKey = focusPrefs.getLong("date_key", 0L)
        val storedOrdered = focusPrefs.getString("focus_ids_ordered", null)
            ?.split(",")?.mapNotNull { it.toLongOrNull() }
            ?: focusPrefs.getStringSet("focus_ids", emptySet())
                ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()

        if (storedKey == todayKey && storedOrdered.isNotEmpty()) {
            _focusIds.value = storedOrdered
            return
        }

        dailyGoalRepository.reconcilePastRecommendations(allTodos, latestActivities)
        val selectionResult = selectTodayFocus(allTodos)
        val newIds = selectionResult.map { it.todo.id }
        _focusIds.value = newIds

        focusPrefs.edit()
            .putLong("date_key", todayKey)
            .putString("focus_ids_ordered", newIds.joinToString(","))
            .apply()

        // Room에 오늘의 목표 추천 저장 (async, 실패해도 기존 동작 유지)
        val dateKey = dailyGoalRepository.todayDateKey()
        viewModelScope.launch {
            runCatching {
                dailyGoalRepository.saveRecommendation(
                    dateKey = dateKey,
                    selectedItems = selectionResult,
                    candidateTodos = allTodos.filter { !it.isCompleted },
                    isRefresh = false
                )
            }
        }

        selectionResult
            .filter { it.todo.category == TodoCategory.REVIEW && it.todo.reviewStage == 1 && it.todo.isCompleted }
            .forEach { goalItem ->
                repository.updateTodo(goalItem.todo.copy(isCompleted = false, updatedAt = System.currentTimeMillis()))
            }
    }

    fun addTodo(title: String, category: TodoCategory = TodoCategory.NORMAL, selectedDate: Long? = null) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.insertTodo(
                TodoItem(
                    title = clean,
                    category = category,
                    selectedDate = selectedDate,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun completeTodo(todo: TodoItem) {
        viewModelScope.launch {
            if (todo.category == TodoCategory.REVIEW && todo.reviewStage < 2) {
                repository.completeReviewTodo(todo)
            } else {
                repository.updateCompleted(todo.id, true, System.currentTimeMillis())
            }
        }
    }

    fun completeFocusTodo(todo: TodoItem) {
        viewModelScope.launch {
            if (todo.category == TodoCategory.REVIEW && todo.reviewStage < 2) {
                repository.completeReviewTodo(todo)
            } else {
                repository.updateCompleted(todo.id, true, System.currentTimeMillis())
            }
            // 오늘의 목표에서 완료된 항목으로 Room에 기록
            runCatching {
                dailyGoalRepository.markItemCompleted(
                    dateKey = dailyGoalRepository.todayDateKey(),
                    todoLegacyId = todo.id
                )
            }
        }
    }

    fun startFocusTodo(todo: TodoItem) {
        viewModelScope.launch {
            runCatching {
                dailyGoalRepository.markItemClicked(
                    dateKey = dailyGoalRepository.todayDateKey(),
                    todoLegacyId = todo.id
                )
            }
        }
    }

    fun uncompleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            if (todo.category == TodoCategory.REVIEW) {
                val now = System.currentTimeMillis()
                when (todo.reviewStage) {
                    1 -> repository.updateTodo(todo.copy(
                        isCompleted = false, reviewStage = 0,
                        reviewStage1CompletedAt = null, updatedAt = now
                    ))
                    2 -> repository.updateTodo(todo.copy(
                        isCompleted = true, reviewStage = 1,
                        completedAt = null, updatedAt = now
                    ))
                    else -> repository.updateCompleted(todo.id, false, null)
                }
            } else {
                repository.updateCompleted(todo.id, false, null)
            }
        }
    }

    fun updateTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.updateTodo(todo.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }

    fun refreshSort() {
        val currentTodos = _todos.value
        if (currentTodos.isEmpty()) return

        val selectionResult = selectTodayFocus(currentTodos)
        val newIds = selectionResult.map { it.todo.id }
        _focusIds.value = newIds

        val todayKey = startOfDay(System.currentTimeMillis())
        focusPrefs.edit()
            .putLong("date_key", todayKey)
            .putString("focus_ids_ordered", newIds.joinToString(","))
            .apply()

        viewModelScope.launch {
            // Room에 새로고침 추천 저장
            runCatching {
                dailyGoalRepository.reconcilePastRecommendations(currentTodos, latestActivities)
                dailyGoalRepository.saveRecommendation(
                    dateKey = dailyGoalRepository.todayDateKey(),
                    selectedItems = selectionResult,
                    candidateTodos = currentTodos.filter { !it.isCompleted },
                    isRefresh = true
                )
            }

            selectionResult
                .filter { it.todo.category == TodoCategory.REVIEW && it.todo.reviewStage == 1 && it.todo.isCompleted }
                .forEach { goalItem ->
                    repository.updateTodo(goalItem.todo.copy(isCompleted = false, updatedAt = System.currentTimeMillis()))
                }
        }
    }

    fun addWorkTime(todoId: Long, durationMillis: Long) {
        val seconds = durationMillis / 1000L
        if (seconds <= 0L) return
        viewModelScope.launch {
            repository.addAccumulatedSeconds(todoId, seconds)
        }
    }

    suspend fun restoreTodosFromBackup(jsonText: String): Int {
        return repository.restoreTodosFromBackup(jsonText)
    }

    // ── 오늘의 목표 선정 ──────────────────────────────────────────────────
    // 우선순위: D-0 과제는 최우선. 일반 추천은 Todo 2개를 목표로 하되,
    // D-0 과제는 개수 제한 없이 모두 포함하고, D-1 등 긴급 Todo가 많으면 최대 3개까지 허용한다.
    private fun selectTodayFocus(allTodos: List<TodoItem>): List<GoalItem> {
        val todayStart = startOfDay(System.currentTimeMillis())
        val active = allTodos.filter {
            !it.isCompleted || (it.category == TodoCategory.REVIEW && it.reviewStage == 1)
        }

        data class Candidate(val todo: TodoItem, val priority: Int, val reason: String) {
            val isUrgent: Boolean = priority <= 1
        }

        fun burdenRank(todo: TodoItem): Int {
            val ageDays = ((todayStart - startOfDay(todo.createdAt)) / DAY_MILLIS).coerceAtLeast(0L)
            val workHours = todo.accumulatedSeconds / 3600L
            return when {
                ageDays <= 1L && workHours == 0L -> 0
                ageDays >= 7L || workHours >= 3L -> 2
                else -> 1
            }
        }

        val priorityCandidates = active.mapNotNull { todo ->
            val (priority, reason) = when (todo.category) {
                TodoCategory.ASSIGNMENT -> {
                    val deadline = todo.selectedDate ?: return@mapNotNull null
                    val daysUntil = daysDiff(todayStart, startOfDay(deadline))
                    when (daysUntil) {
                        0L -> 0 to RecommendationReason.ASSIGNMENT_TODAY
                        1L -> 1 to RecommendationReason.ASSIGNMENT_D_MINUS_1
                        7L -> 4 to RecommendationReason.ASSIGNMENT_D_MINUS_7
                        else -> return@mapNotNull null
                    }
                }
                TodoCategory.REVIEW -> {
                    val base = todo.selectedDate ?: todo.createdAt
                    val daysSince = daysDiff(startOfDay(base), todayStart)
                    when {
                        todo.reviewStage == 0 -> when (daysSince) {
                            1L       -> 2 to RecommendationReason.REVIEW_D_PLUS_1
                            2L, 3L   -> 3 to RecommendationReason.REVIEW_D_PLUS_1_LATE
                            else     -> return@mapNotNull null
                        }
                        todo.reviewStage == 1 -> when (daysSince) {
                            7L              -> 5 to RecommendationReason.REVIEW_D_PLUS_7
                            8L, 9L, 10L     -> 6 to RecommendationReason.REVIEW_D_PLUS_7_LATE
                            else            -> return@mapNotNull null
                        }
                        else -> return@mapNotNull null
                    }
                }
                TodoCategory.NORMAL -> return@mapNotNull null
            }
            Candidate(todo, priority, reason)
        }

        val d0Candidates = priorityCandidates
            .filter { it.priority == 0 }
            .sortedWith(compareBy<Candidate> { burdenRank(it.todo) }.thenBy { it.todo.selectedDate ?: Long.MAX_VALUE }.thenByDescending { it.todo.createdAt })

        val result = d0Candidates
            .map { GoalItem(it.todo, it.reason) }
            .toMutableList()

        val urgentCount = priorityCandidates.count { it.isUrgent }
        val targetCount = if (result.isNotEmpty()) {
            result.size.coerceAtLeast(if (urgentCount >= 3) 3 else 2)
        } else if (urgentCount >= 3) {
            3
        } else {
            2
        }

        val takenIds = result.map { it.todo.id }.toSet()
        val remainingPriority = priorityCandidates
            .filter { it.todo.id !in takenIds }
            .sortedWith(compareBy<Candidate> { it.priority }.thenBy { burdenRank(it.todo) }.thenByDescending { it.todo.accumulatedSeconds })
            .take((targetCount - result.size).coerceAtLeast(0))
            .map { GoalItem(it.todo, it.reason) }
        result.addAll(remainingPriority)

        if (result.size < targetCount) {
            val filledIds = result.map { it.todo.id }.toSet()
            val fills = active
                .filter { it.id !in filledIds && !it.isCompleted }
                .sortedWith(compareByDescending<TodoItem> { it.accumulatedSeconds == 0L }.thenByDescending { it.createdAt })
                .take(targetCount - result.size)
                .map { GoalItem(it, RecommendationReason.EMPTY_GOAL_FILL) }
            result.addAll(fills)
        }

        return result
    }

    private fun buildYesterdaySuggestion(
        todos: List<TodoItem>,
        activities: List<ActivitySession>
    ): YesterdayFlowSuggestion? {
        val yesterdayStart = startOfDay(System.currentTimeMillis()) - DAY_MILLIS
        val yesterdayEnd = yesterdayStart + DAY_MILLIS
        val yesterday = activities.filter { it.startTime in yesterdayStart until yesterdayEnd }
        val totals = yesterday.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.durationMillis } }
        val productive = listOf("STUDY", "WORK", "DEVELOPMENT").sumOf { totals[it] ?: 0L }
        val rest = totals["REST"] ?: 0L
        val sleep = totals["SLEEP"] ?: 0L
        val hasOpenTodo = todos.any { !it.isCompleted }

        return when {
            yesterday.isEmpty() -> YesterdayFlowSuggestion(
                message = "어제 기록이 비어 있어요. 오늘은 가벼운 시작 기록부터 남겨볼까요?",
                actionLabel = "휴식으로 시작",
                actionCategory = "REST"
            )
            sleep > 0L && sleep < 5.hours -> YesterdayFlowSuggestion(
                message = "어제 수면이 짧았어요. 오늘은 회복 리듬을 먼저 챙겨도 좋아요.",
                actionLabel = "수면 기록",
                actionCategory = "SLEEP"
            )
            rest < 30.minutes -> YesterdayFlowSuggestion(
                message = "어제 휴식 기록이 거의 없었어요. 짧게 쉬고 다시 들어가도 괜찮아요.",
                actionLabel = "휴식 시작",
                actionCategory = "REST"
            )
            hasOpenTodo && productive < 30.minutes -> YesterdayFlowSuggestion(
                message = "어제 Todo로 이어진 집중 시간이 적었어요. 오늘은 10분만 먼저 열어볼까요?",
                actionLabel = "공부 시작",
                actionCategory = "STUDY"
            )
            productive < 60.minutes -> YesterdayFlowSuggestion(
                message = "어제 공부/업무/개발 흐름이 낮았어요. 부담 없는 한 세션을 추천해요.",
                actionLabel = "집중 시작",
                actionCategory = "STUDY"
            )
            else -> YesterdayFlowSuggestion(
                message = "어제 흐름은 안정적이었어요. 오늘은 목표 두 개만 또렷하게 가져가요.",
                actionLabel = "휴식 시작",
                actionCategory = "REST"
            )
        }
    }

    private fun startOfDay(millis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun daysDiff(fromMs: Long, toMs: Long): Long =
        (toMs - fromMs) / (24L * 60 * 60 * 1000)

    private val Int.minutes: Long get() = this * 60L * 1000L
    private val Int.hours: Long get() = this * 60L * 60L * 1000L

    companion object {
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
