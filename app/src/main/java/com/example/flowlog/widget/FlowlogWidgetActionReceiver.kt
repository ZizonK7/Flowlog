package com.example.flowlog.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.TodoRepository
import com.example.flowlog.notification.ReminderScheduler
import com.example.flowlog.ui.component.displayCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlowlogWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                handleAction(context.applicationContext, intent)
            }
            pendingResult.finish()
        }
    }

    private suspend fun handleAction(context: Context, intent: Intent) {
        when (intent.action) {
            FlowlogWidgetProvider.ACTION_START -> {
                val category = intent.getStringExtra(FlowlogWidgetProvider.EXTRA_CATEGORY) ?: return
                if (FlowlogWidgetProvider.getActiveSessionDetails(context) != null) {
                    FlowlogWidgetProvider.updateAll(context)
                    return
                }
                FlowlogWidgetProvider.setActiveSession(context, category, System.currentTimeMillis())
                FlowlogWidgetProvider.updateAll(context)
            }

            FlowlogWidgetProvider.ACTION_STOP -> {
                saveActiveSession(context)
                FlowlogWidgetProvider.clearActiveSession(context)
                FlowlogWidgetProvider.updateAll(context)
                TodoWidgetProvider.updateAll(context)
            }

            FlowlogWidgetProvider.ACTION_SNACK -> {
                ReminderScheduler(context).scheduleSnackReminder()
                FlowlogWidgetProvider.updateAll(context)
            }

            FlowlogWidgetProvider.ACTION_TOOTHBRUSH -> {
                ReminderScheduler(context).scheduleBrushTimers()
                FlowlogWidgetProvider.updateAll(context)
            }
        }
    }

    private suspend fun saveActiveSession(context: Context) {
        val activeSession = FlowlogWidgetProvider.getActiveSessionDetails(context) ?: return
        val endTime = System.currentTimeMillis()
        val durationMillis = (endTime - activeSession.startTime).coerceAtLeast(0L)
        val activity = ActivitySession(
            category = activeSession.category,
            title = activeSession.linkedTodoTitle ?: displayCategory(activeSession.category),
            startTime = activeSession.startTime,
            endTime = endTime,
            durationMillis = durationMillis,
            linkedTodoId = activeSession.linkedTodoId
        )
        val repository = ActivityRepository(context)
        val newId = repository.insertActivity(activity)
        activeSession.linkedTodoId?.let { todoId ->
            TodoRepository(context).addAccumulatedMillis(todoId, durationMillis)
        }
        runCatching {
            ReminderScheduler(context).scheduleToothbrushReminder(activity.copy(id = newId))
        }
    }
}
