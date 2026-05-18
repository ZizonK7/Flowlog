package com.example.flowlog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodoViewModel(
    private val repository: TodoRepository
) : ViewModel() {
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllTodos().collect { items ->
                _todos.value = items
            }
        }
    }

    fun addTodo(title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        viewModelScope.launch {
            repository.insertTodo(
                TodoItem(
                    title = cleanTitle,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleTodoDone(todo: TodoItem) {
        val nextDone = !todo.isDone
        viewModelScope.launch {
            repository.updateDone(
                id = todo.id,
                isDone = nextDone,
                completedAt = if (nextDone) System.currentTimeMillis() else null
            )
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }

    fun addWorkTime(todoId: Long, durationMillis: Long) {
        viewModelScope.launch {
            repository.addAccumulatedMillis(todoId, durationMillis)
        }
    }
}
