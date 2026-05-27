package com.example.flowlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
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

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    // 하루 동안 고정된 오늘의 목표 ID 목록 — 우선순위 순서 보존 (List)
    private val _focusIds = MutableStateFlow<List<Long>>(emptyList())

    val todayFocusItems: StateFlow<List<TodoItem>> = combine(_todos, _focusIds) { todos, ids ->
        val idToIndex = ids.mapIndexed { i, id -> id to i }.toMap()
        todos.filter { it.id in idToIndex }
            .sortedWith(compareBy(
                { if (it.isCompleted) 1 else 0 },   // 완료 항목은 하단
                { idToIndex[it.id] ?: Int.MAX_VALUE } // 미완료 내에서 우선순위 순
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

    private fun initFocusIds(allTodos: List<TodoItem>) {
        val todayKey = startOfDay(System.currentTimeMillis())
        val storedKey = focusPrefs.getLong("date_key", 0L)
        // focus_ids_ordered 없으면 이전 버전 focus_ids(Set) 폴백
        val storedOrdered = focusPrefs.getString("focus_ids_ordered", null)
            ?.split(",")?.mapNotNull { it.toLongOrNull() }
            ?: focusPrefs.getStringSet("focus_ids", emptySet())
                ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()

        if (storedKey == todayKey && storedOrdered.isNotEmpty()) {
            // 오늘이면 우선순위 순서 그대로 복원
            _focusIds.value = storedOrdered
            return
        }

        // 새로운 오늘의 목표 선정 후 저장 (이전 날 완료 항목은 DB/CSV에 보존)
        val newIds = selectTodayFocus(allTodos).map { it.id }
        _focusIds.value = newIds
        focusPrefs.edit()
            .putLong("date_key", todayKey)
            .putString("focus_ids_ordered", newIds.joinToString(","))
            .apply()
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

    // 전체 할 일 체크: 완료 기록은 DB/CSV에 계속 누적, UI는 maxByOrNull로 최신 1개만 표시
    fun completeTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.updateCompleted(
                id = todo.id,
                isCompleted = true,
                completedAt = System.currentTimeMillis()
            )
        }
    }

    // 오늘의 목표 체크: 항목은 섹션에 그대로 남고 다음 날 자동 삭제됨
    fun completeFocusTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.updateCompleted(
                id = todo.id,
                isCompleted = true,
                completedAt = System.currentTimeMillis()
            )
        }
    }

    fun uncompleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.updateCompleted(id = todo.id, isCompleted = false, completedAt = null)
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
        val newIds = selectTodayFocus(currentTodos).map { it.id }
        _focusIds.value = newIds
        val todayKey = startOfDay(System.currentTimeMillis())
        focusPrefs.edit()
            .putLong("date_key", todayKey)
            .putString("focus_ids_ordered", newIds.joinToString(","))
            .apply()
    }

    // durationMillis 단위로 받아서 내부적으로 초 단위로 저장
    fun addWorkTime(todoId: Long, durationMillis: Long) {
        val seconds = durationMillis / 1000L
        if (seconds <= 0L) return
        viewModelScope.launch {
            repository.addAccumulatedSeconds(todoId, seconds)
        }
    }

    // ─── 오늘의 목표 초기 선정 (미완료 항목 대상, 최초 1회만 호출) ────────
    // 우선순위: 과제 당일(0) > 과제 D-1(1) > 복습 D+1(2) > 과제 D-7(3) > 복습 D+7(4)
    // 3개 미달 시 전체 할 일 최신 순으로 채움.
    private fun selectTodayFocus(allTodos: List<TodoItem>): List<TodoItem> {
        val todayStart = startOfDay(System.currentTimeMillis())
        val active = allTodos.filter { !it.isCompleted }

        data class Candidate(val todo: TodoItem, val priority: Int)

        val priorityCandidates = active.mapNotNull { todo ->
            val priority = when (todo.category) {
                TodoCategory.ASSIGNMENT -> {
                    val deadline = todo.selectedDate ?: return@mapNotNull null
                    val daysUntil = daysDiff(todayStart, startOfDay(deadline))
                    when (daysUntil) {
                        0L -> 0          // 과제 당일
                        1L -> 1          // 과제 D-1
                        7L -> 4          // 과제 D-7
                        else -> null
                    }
                }
                TodoCategory.REVIEW -> {
                    val base = todo.selectedDate ?: todo.createdAt
                    val daysSince = daysDiff(startOfDay(base), todayStart)
                    when (daysSince) {
                        1L       -> 2    // 복습 D+1
                        2L, 3L   -> 3    // 밀린 D+1 복습
                        7L       -> 5    // 복습 D+7
                        8L, 9L, 10L -> 6 // 밀린 D+7 복습
                        else -> null
                    }
                }
                TodoCategory.NORMAL -> null
            }
            priority?.let { Candidate(todo, it) }
        }

        val result = priorityCandidates.sortedBy { it.priority }.take(3).map { it.todo }.toMutableList()

        // 부족한 슬롯은 미완료 최신 항목으로 채움
        if (result.size < 3) {
            val takenIds = result.map { it.id }.toSet()
            val fills = active
                .filter { it.id !in takenIds }
                .sortedByDescending { it.createdAt }
                .take(3 - result.size)
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
