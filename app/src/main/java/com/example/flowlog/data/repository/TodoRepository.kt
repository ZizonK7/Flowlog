package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.local.TodoDao
import com.example.flowlog.data.local.TodoLocalDataSource
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.remote.FirestoreSyncRepository
import com.example.flowlog.notification.TodoReminderScheduler
import kotlinx.coroutines.flow.Flow

class TodoRepository(context: Context) {
    private val appContext = context.applicationContext
    private val todoDao: TodoDao = TodoLocalDataSource(context)
    private val todoReminderScheduler = TodoReminderScheduler(appContext)
    private val syncRepository = FirestoreSyncRepository()

    fun getAllTodos(): Flow<List<TodoItem>> = todoDao.getAllTodos()

    fun getIncompleteTodos(): Flow<List<TodoItem>> = todoDao.getIncompleteTodos()

    suspend fun insertTodo(todo: TodoItem): Long {
        val id = todoDao.insertTodo(todo)
        runCatching { syncRepository.syncTodo(todo.copy(id = id)) }
        todoReminderScheduler.scheduleInitialReminder(id, todo.createdAt)
        return id
    }

    suspend fun updateCompleted(id: Long, isCompleted: Boolean, completedAt: Long?) {
        todoDao.updateCompleted(id, isCompleted, completedAt)
        runCatching { syncAllTodos() }
        if (isCompleted) {
            todoReminderScheduler.cancelReminder(id)
        } else {
            todoReminderScheduler.scheduleInitialReminder(id, System.currentTimeMillis())
        }
    }

    suspend fun updateTodo(todo: TodoItem) {
        todoDao.updateTodo(todo)
        runCatching { syncAllTodos() }
    }

    suspend fun deleteTodo(todo: TodoItem) {
        todoDao.deleteTodo(todo)
        runCatching { syncRepository.deleteTodo(todo.id) }
        todoReminderScheduler.cancelReminder(todo.id)
    }

    suspend fun addAccumulatedSeconds(id: Long, seconds: Long) {
        todoDao.addAccumulatedSeconds(id, seconds)
        runCatching { syncAllTodos() }
    }

    private suspend fun syncAllTodos() {
        syncRepository.syncTodos(TodoLocalDataSource.loadSnapshot(appContext))
    }
}
