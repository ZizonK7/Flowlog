package com.example.flowlog.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.flowlog.MainActivity
import com.example.flowlog.R
import com.example.flowlog.data.local.TodoLocalDataSource
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.ui.component.formatDuration

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            runCatching {
                appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context))
            }
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TodoWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { id ->
                runCatching {
                    manager.updateAppWidget(id, buildRemoteViews(context))
                }
            }
        }

        private fun buildRemoteViews(context: Context): RemoteViews {
            val todos = TodoLocalDataSource.loadSnapshot(context)
                .filter { !it.isDone }
                .sortedByDescending { it.createdAt }
                .take(4)

            return RemoteViews(context.packageName, R.layout.todo_widget).apply {
                setTextViewText(R.id.todo_widget_title, "Todo")
                setOnClickPendingIntent(R.id.todo_widget_root, openTodoIntent(context, 30))
                setOnClickPendingIntent(R.id.todo_widget_open, openTodoIntent(context, 31))

                if (todos.isEmpty()) {
                    setViewVisibility(R.id.todo_widget_empty, View.VISIBLE)
                } else {
                    setViewVisibility(R.id.todo_widget_empty, View.GONE)
                }

                bindTodoRow(
                    context = context,
                    views = this,
                    todo = todos.getOrNull(0),
                    rowId = R.id.todo_widget_row_1,
                    titleId = R.id.todo_widget_title_1,
                    timeId = R.id.todo_widget_time_1,
                    doneId = R.id.todo_widget_done_1,
                    startId = R.id.todo_widget_start_1,
                    requestCodeBase = 100
                )
                bindTodoRow(
                    context = context,
                    views = this,
                    todo = todos.getOrNull(1),
                    rowId = R.id.todo_widget_row_2,
                    titleId = R.id.todo_widget_title_2,
                    timeId = R.id.todo_widget_time_2,
                    doneId = R.id.todo_widget_done_2,
                    startId = R.id.todo_widget_start_2,
                    requestCodeBase = 110
                )
                bindTodoRow(
                    context = context,
                    views = this,
                    todo = todos.getOrNull(2),
                    rowId = R.id.todo_widget_row_3,
                    titleId = R.id.todo_widget_title_3,
                    timeId = R.id.todo_widget_time_3,
                    doneId = R.id.todo_widget_done_3,
                    startId = R.id.todo_widget_start_3,
                    requestCodeBase = 120
                )
                bindTodoRow(
                    context = context,
                    views = this,
                    todo = todos.getOrNull(3),
                    rowId = R.id.todo_widget_row_4,
                    titleId = R.id.todo_widget_title_4,
                    timeId = R.id.todo_widget_time_4,
                    doneId = R.id.todo_widget_done_4,
                    startId = R.id.todo_widget_start_4,
                    requestCodeBase = 130
                )
            }
        }

        private fun bindTodoRow(
            context: Context,
            views: RemoteViews,
            todo: TodoItem?,
            rowId: Int,
            titleId: Int,
            timeId: Int,
            doneId: Int,
            startId: Int,
            requestCodeBase: Int
        ) {
            if (todo == null) {
                views.setViewVisibility(rowId, View.GONE)
                return
            }

            views.setViewVisibility(rowId, View.VISIBLE)
            views.setTextViewText(titleId, todo.title)
            views.setTextViewText(timeId, formatDuration(todo.accumulatedMillis))
            views.setOnClickPendingIntent(
                doneId,
                todoActionIntent(
                    context = context,
                    action = TodoWidgetActionReceiver.ACTION_TOGGLE_DONE,
                    todo = todo,
                    requestCode = requestCodeBase
                )
            )
            views.setOnClickPendingIntent(
                startId,
                todoActionIntent(
                    context = context,
                    action = TodoWidgetActionReceiver.ACTION_START_TODO,
                    todo = todo,
                    requestCode = requestCodeBase + 1
                )
            )
        }

        private fun openTodoIntent(context: Context, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_SCREEN, MainActivity.SCREEN_TODO)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun todoActionIntent(
            context: Context,
            action: String,
            todo: TodoItem,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, TodoWidgetActionReceiver::class.java).apply {
                this.action = action
                putExtra(TodoWidgetActionReceiver.EXTRA_TODO_ID, todo.id)
                putExtra(TodoWidgetActionReceiver.EXTRA_TODO_TITLE, todo.title)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
