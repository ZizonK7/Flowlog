package com.example.flowlog.data.local

import com.example.flowlog.data.model.TodoItem
import kotlinx.coroutines.flow.Flow

interface TodoDao {
    fun getAllTodos(): Flow<List<TodoItem>>

    fun getIncompleteTodos(): Flow<List<TodoItem>>

    suspend fun insertTodo(todo: TodoItem): Long

    suspend fun updateDone(id: Long, isDone: Boolean, completedAt: Long?)

    suspend fun deleteTodo(todo: TodoItem)

    suspend fun addAccumulatedMillis(id: Long, durationMillis: Long)
}
