package com.example.flowlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.TodoRepository
import com.example.flowlog.notification.ReminderScheduler

class ActivityViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            val appContext = context.applicationContext
            val repository = ActivityRepository(appContext)
            val todoRepository = TodoRepository(appContext)
            val reminderScheduler = ReminderScheduler(appContext)
            @Suppress("UNCHECKED_CAST")
            return ActivityViewModel(repository, todoRepository, reminderScheduler, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

