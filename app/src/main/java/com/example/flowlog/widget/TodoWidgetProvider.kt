package com.example.flowlog.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.flowlog.MainActivity
import com.example.flowlog.R

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            runCatching {
                appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context, widgetId))
            }
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TodoWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.todo_widget_list)
            }
            ids.forEach { id ->
                runCatching {
                    manager.updateAppWidget(id, buildRemoteViews(context, id))
                }
            }
        }

        private fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
            val adapterIntent = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            return RemoteViews(context.packageName, R.layout.todo_widget).apply {
                setTextViewText(R.id.todo_widget_title, "Todo")
                setRemoteAdapter(R.id.todo_widget_list, adapterIntent)
                setEmptyView(R.id.todo_widget_list, R.id.todo_widget_empty)
                setPendingIntentTemplate(R.id.todo_widget_list, todoActionTemplateIntent(context))
                setOnClickPendingIntent(R.id.todo_widget_root, openTodoIntent(context, 30))
                setOnClickPendingIntent(R.id.todo_widget_open, openTodoIntent(context, 31))
            }
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

        private fun todoActionTemplateIntent(context: Context): PendingIntent {
            val intent = Intent(context, TodoWidgetActionReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                200,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }
    }
}
