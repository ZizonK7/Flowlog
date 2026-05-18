package com.example.flowlog.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.flowlog.data.model.TodoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TodoLocalDataSource(context: Context) : TodoDao {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_TODO,
        Context.MODE_PRIVATE
    )
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val todos = MutableStateFlow(loadTodos())

    override fun getAllTodos(): Flow<List<TodoItem>> {
        return todos
    }

    override fun getIncompleteTodos(): Flow<List<TodoItem>> {
        return todos.map { items ->
            items.filter { !it.isDone }
        }
    }

    override suspend fun insertTodo(todo: TodoItem): Long {
        val allTodos = todos.value.toMutableList()
        val newTodo = todo.copy(id = (allTodos.maxOfOrNull { it.id } ?: 0L) + 1L)
        allTodos.add(0, newTodo)
        saveTodos(allTodos)
        return newTodo.id
    }

    override suspend fun updateDone(id: Long, isDone: Boolean, completedAt: Long?) {
        updateTodo(id) { todo ->
            todo.copy(
                isDone = isDone,
                completedAt = completedAt
            )
        }
    }

    override suspend fun deleteTodo(todo: TodoItem) {
        val nextTodos = todos.value.filterNot { it.id == todo.id }
        saveTodos(nextTodos)
    }

    override suspend fun addAccumulatedMillis(id: Long, durationMillis: Long) {
        if (durationMillis <= 0L) return

        updateTodo(id) { todo ->
            todo.copy(accumulatedMillis = todo.accumulatedMillis + durationMillis)
        }
    }

    private suspend fun updateTodo(id: Long, transform: (TodoItem) -> TodoItem) {
        val nextTodos = todos.value.map { todo ->
            if (todo.id == id) transform(todo) else todo
        }
        saveTodos(nextTodos)
    }

    private fun loadTodos(): List<TodoItem> {
        val data = sharedPreferences.getString(KEY_ALL_TODOS, "[]") ?: "[]"
        return try {
            json.decodeFromString<List<TodoItem>>(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveTodos(newTodos: List<TodoItem>) {
        val sortedTodos = withContext(Dispatchers.Default) {
            newTodos.sortedWith(
                compareBy<TodoItem> { it.isDone }
                    .thenByDescending { it.createdAt }
            )
        }
        todos.value = sortedTodos
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .putString(KEY_ALL_TODOS, json.encodeToString(sortedTodos))
                .apply()
        }
    }

    companion object {
        const val PREFS_TODO = "todo_data"
        const val KEY_ALL_TODOS = "all_todos"
        private val snapshotJson = Json { ignoreUnknownKeys = true }

        fun loadSnapshot(context: Context): List<TodoItem> {
            val data = context.getSharedPreferences(PREFS_TODO, Context.MODE_PRIVATE)
                .getString(KEY_ALL_TODOS, "[]") ?: "[]"
            return try {
                snapshotJson.decodeFromString<List<TodoItem>>(data)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
