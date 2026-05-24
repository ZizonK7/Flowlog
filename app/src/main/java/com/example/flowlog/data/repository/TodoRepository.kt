package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.local.TodoDao
import com.example.flowlog.data.local.TodoLocalDataSource
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.remote.FirestoreSyncRepository
import com.example.flowlog.notification.TodoReminderScheduler
import com.example.flowlog.widget.TodoWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TodoRepository(context: Context) {
    private val appContext = context.applicationContext
    private val todoDao: TodoDao = TodoLocalDataSource(context)
    private val todoReminderScheduler = TodoReminderScheduler(appContext)
    private val syncRepository = FirestoreSyncRepository()

    fun getAllTodos(): Flow<List<TodoItem>> {
        return todoDao.getAllTodos()
    }

    fun getIncompleteTodos(): Flow<List<TodoItem>> {
        return todoDao.getIncompleteTodos()
    }

    suspend fun insertTodo(todo: TodoItem): Long {
        val id = todoDao.insertTodo(todo)
        runCatching {
            syncRepository.syncTodo(todo.copy(id = id))
        }
        todoReminderScheduler.scheduleInitialReminder(id, todo.createdAt)
        updateWidget()
        return id
    }

    suspend fun updateDone(id: Long, isDone: Boolean, completedAt: Long?) {
        todoDao.updateDone(id, isDone, completedAt)
        runCatching {
            syncAllTodos()
        }
        if (isDone) {
            todoReminderScheduler.cancelReminder(id)
        } else {
            todoReminderScheduler.scheduleInitialReminder(id, System.currentTimeMillis())
        }
        updateWidget()
    }

    suspend fun deleteTodo(todo: TodoItem) {
        todoDao.deleteTodo(todo)
        runCatching {
            syncRepository.deleteTodo(todo.id)
        }
        todoReminderScheduler.cancelReminder(todo.id)
        updateWidget()
    }

    suspend fun addAccumulatedMillis(id: Long, durationMillis: Long) {
        todoDao.addAccumulatedMillis(id, durationMillis)
        runCatching {
            syncAllTodos()
        }
        updateWidget()
    }

    private suspend fun syncAllTodos() {
        syncRepository.syncTodos(TodoLocalDataSource.loadSnapshot(appContext))
    }

    private suspend fun updateWidget() {
        withContext(Dispatchers.IO) {
            TodoWidgetProvider.updateAll(appContext)
        }
    }
}
