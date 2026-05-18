package com.example.flowlog.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.flowlog.data.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoWidgetActionReceiver : BroadcastReceiver() {
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
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) return

        when (intent.action) {
            ACTION_TOGGLE_DONE -> {
                TodoRepository(context).updateDone(
                    id = todoId,
                    isDone = true,
                    completedAt = System.currentTimeMillis()
                )
            }

            ACTION_START_TODO -> {
                val title = intent.getStringExtra(EXTRA_TODO_TITLE).orEmpty()
                FlowlogWidgetProvider.setActiveSession(
                    context = context,
                    category = "TODO",
                    startTime = System.currentTimeMillis(),
                    linkedTodoId = todoId,
                    linkedTodoTitle = title
                )
                FlowlogWidgetProvider.updateAll(context)
                TodoWidgetProvider.updateAll(context)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_DONE = "com.example.flowlog.todo_widget.TOGGLE_DONE"
        const val ACTION_START_TODO = "com.example.flowlog.todo_widget.START_TODO"
        const val EXTRA_TODO_ID = "extra_todo_id"
        const val EXTRA_TODO_TITLE = "extra_todo_title"
    }
}
