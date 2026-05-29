package com.example.flowlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowlog.data.constants.RecommendationReason
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
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

class TodoViewModel(
    private val repository: TodoRepository,
    context: Context
) : ViewModel() {

    private val focusPrefs = context.getSharedPreferences("todo_focus", Context.MODE_PRIVATE)
    private val dailyGoalRepository = DailyGoalRepository(context.applicationContext)

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    private val _focusIds = MutableStateFlow<List<Long>>(emptyList())

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
                if (_focusIds.value.isEmpty() && todos.isNotEmpty()) {
                    initFocusIds(todos)
                }
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
    // 우선순위: 과제 당일(0) > 과제 D-1(1) > 복습 D+1(2) > 복습 D+1 밀림(3)
    //          > 과제 D-7(4) > 복습 D+7(5) > 복습 D+7 밀림(6)
    // 3개 미달 시 전체 할 일 최신 순으로 채움 (EMPTY_GOAL_FILL)
    private fun selectTodayFocus(allTodos: List<TodoItem>): List<GoalItem> {
        val todayStart = startOfDay(System.currentTimeMillis())
        val active = allTodos.filter {
            !it.isCompleted || (it.category == TodoCategory.REVIEW && it.reviewStage == 1)
        }

        data class Candidate(val todo: TodoItem, val priority: Int, val reason: String)

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

        val result = priorityCandidates
            .sortedBy { it.priority }
            .take(3)
            .map { GoalItem(it.todo, it.reason) }
            .toMutableList()

        if (result.size < 3) {
            val takenIds = result.map { it.todo.id }.toSet()
            val fills = active
                .filter { it.id !in takenIds && !it.isCompleted }
                .sortedByDescending { it.createdAt }
                .take(3 - result.size)
                .map { GoalItem(it, RecommendationReason.EMPTY_GOAL_FILL) }
            result.addAll(fills)
        }

        return result
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
}
