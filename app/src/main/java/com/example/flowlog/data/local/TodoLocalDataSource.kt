package com.example.flowlog.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodoLocalDataSource(private val context: Context) : TodoDao {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_TODO,
        Context.MODE_PRIVATE
    )

    init {
        ensureLoaded(sharedPreferences)
    }

    override fun getAllTodos(): Flow<List<TodoItem>> = todos

    override fun getIncompleteTodos(): Flow<List<TodoItem>> =
        todos.map { items -> items.filter { !it.isCompleted } }

    override suspend fun insertTodo(todo: TodoItem): Long {
        val allTodos = todos.value.toMutableList()
        val newTodo = todo.copy(id = (allTodos.maxOfOrNull { it.id } ?: 0L) + 1L)
        allTodos.add(0, newTodo)
        saveTodos(allTodos)
        return newTodo.id
    }

    override suspend fun updateCompleted(id: Long, isCompleted: Boolean, completedAt: Long?) {
        updateTodoById(id) { it.copy(isCompleted = isCompleted, completedAt = completedAt, updatedAt = System.currentTimeMillis()) }
    }

    override suspend fun updateTodo(todo: TodoItem) {
        val next = todos.value.map { if (it.id == todo.id) todo else it }
        saveTodos(next)
    }

    override suspend fun deleteTodo(todo: TodoItem) {
        saveTodos(todos.value.filterNot { it.id == todo.id })
    }

    override suspend fun addAccumulatedSeconds(id: Long, seconds: Long) {
        if (seconds == 0L) return
        updateTodoById(id) { it.copy(accumulatedSeconds = (it.accumulatedSeconds + seconds).coerceAtLeast(0L)) }
    }

    private suspend fun updateTodoById(id: Long, transform: (TodoItem) -> TodoItem) {
        saveTodos(todos.value.map { if (it.id == id) transform(it) else it })
    }

    private suspend fun saveTodos(newTodos: List<TodoItem>) {
        val sorted = withContext(Dispatchers.Default) {
            newTodos.sortedWith(
                compareBy<TodoItem> { it.isCompleted }
                    .thenByDescending { it.createdAt }
            )
        }
        todos.value = sorted
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .putString(KEY_ALL_TODOS, snapshotJson.encodeToString(sorted))
                .apply()
            writeCsvSnapshot(sorted)
        }
    }

    private fun writeCsvSnapshot(allTodos: List<TodoItem>) {
        val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val csvFile = File(exportDir, "flowlog_todos.csv")
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val csv = buildString {
            append("﻿")
            appendLine("id,title,category,is_completed,created_at,completed_at,selected_date,accumulated_seconds,updated_at")
            allTodos.sortedBy { it.createdAt }.forEach { todo ->
                appendLine(
                    listOf(
                        todo.id.toString(),
                        todo.title,
                        todo.category.name,
                        todo.isCompleted.toString(),
                        formatCsvTime(todo.createdAt, timeFormat),
                        todo.completedAt?.let { formatCsvTime(it, timeFormat) } ?: "",
                        todo.selectedDate?.let { formatCsvTime(it, timeFormat) } ?: "",
                        todo.accumulatedSeconds.toString(),
                        formatCsvTime(todo.updatedAt, timeFormat)
                    ).joinToString(",") { csvEscape(it) }
                )
            }
        }

        runCatching { csvFile.writeText(csv, Charsets.UTF_8) }
    }

    private fun formatCsvTime(timestamp: Long, format: SimpleDateFormat): String =
        format.format(Date(timestamp))

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    companion object {
        const val PREFS_TODO = "todo_data"
        const val KEY_ALL_TODOS = "all_todos"
        private val snapshotJson = Json { ignoreUnknownKeys = true }
        private val todos = MutableStateFlow<List<TodoItem>>(emptyList())
        @Volatile private var isLoaded = false

        private fun ensureLoaded(sharedPreferences: SharedPreferences) {
            if (isLoaded) return
            synchronized(this) {
                if (isLoaded) return
                todos.value = loadSnapshot(sharedPreferences)
                isLoaded = true
            }
        }

        fun loadSnapshot(context: Context): List<TodoItem> =
            loadSnapshot(context.getSharedPreferences(PREFS_TODO, Context.MODE_PRIVATE))

        private fun loadSnapshot(sharedPreferences: SharedPreferences): List<TodoItem> {
            val data = sharedPreferences.getString(KEY_ALL_TODOS, "[]") ?: "[]"
            return try {
                snapshotJson.decodeFromString<List<TodoItem>>(data)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
