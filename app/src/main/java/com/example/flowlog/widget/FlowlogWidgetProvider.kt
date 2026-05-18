package com.example.flowlog.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.example.flowlog.MainActivity
import com.example.flowlog.R
import com.example.flowlog.ui.component.displayCategory

class FlowlogWidgetProvider : AppWidgetProvider() {
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
        const val PREFS_WIDGET = "flowlog_widget_state"
        const val KEY_ACTIVE_CATEGORY = "active_category"
        const val KEY_ACTIVE_START_TIME = "active_start_time"
        const val KEY_ACTIVE_TODO_ID = "active_todo_id"
        const val KEY_ACTIVE_TODO_TITLE = "active_todo_title"
        const val ACTION_START = "com.example.flowlog.widget.START"
        const val ACTION_STOP = "com.example.flowlog.widget.STOP"
        const val ACTION_SNACK = "com.example.flowlog.widget.SNACK"
        const val ACTION_TOOTHBRUSH = "com.example.flowlog.widget.TOOTHBRUSH"
        const val EXTRA_CATEGORY = "extra_category"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, FlowlogWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { id ->
                runCatching {
                    manager.updateAppWidget(id, buildRemoteViews(context))
                }
            }
        }

        data class ActiveSession(
            val category: String,
            val startTime: Long,
            val linkedTodoId: Long? = null,
            val linkedTodoTitle: String? = null
        )

        fun clearActiveSession(context: Context) {
            context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ACTIVE_CATEGORY)
                .remove(KEY_ACTIVE_START_TIME)
                .remove(KEY_ACTIVE_TODO_ID)
                .remove(KEY_ACTIVE_TODO_TITLE)
                .apply()
        }

        fun setActiveSession(
            context: Context,
            category: String,
            startTime: Long,
            linkedTodoId: Long? = null,
            linkedTodoTitle: String? = null
        ) {
            context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_CATEGORY, category)
                .putLong(KEY_ACTIVE_START_TIME, startTime)
                .apply {
                    if (linkedTodoId == null) {
                        remove(KEY_ACTIVE_TODO_ID)
                    } else {
                        putLong(KEY_ACTIVE_TODO_ID, linkedTodoId)
                    }
                    if (linkedTodoTitle.isNullOrBlank()) {
                        remove(KEY_ACTIVE_TODO_TITLE)
                    } else {
                        putString(KEY_ACTIVE_TODO_TITLE, linkedTodoTitle)
                    }
                }
                .apply()
        }

        fun getActiveSession(context: Context): Pair<String, Long>? {
            val activeSession = getActiveSessionDetails(context) ?: return null
            return activeSession.category to activeSession.startTime
        }

        fun getActiveSessionDetails(context: Context): ActiveSession? {
            val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
            val category = prefs.getString(KEY_ACTIVE_CATEGORY, null) ?: return null
            val startTime = prefs.getLong(KEY_ACTIVE_START_TIME, 0L)
            if (startTime <= 0L) return null
            val todoId = prefs.getLong(KEY_ACTIVE_TODO_ID, -1L).takeIf { it > 0L }
            val todoTitle = prefs.getString(KEY_ACTIVE_TODO_TITLE, null)
            return ActiveSession(category, startTime, todoId, todoTitle)
        }

        private fun buildRemoteViews(context: Context): RemoteViews {
            val activeSession = getActiveSession(context)

            return RemoteViews(context.packageName, R.layout.flowlog_widget).apply {
                setTextViewText(R.id.widget_title, "Flowlog")
                if (activeSession != null) {
                    val (category, startTime) = activeSession
                    val base = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startTime)
                    setChronometer(
                        R.id.widget_chronometer,
                        base,
                        "${runningStatusText(category)} %s",
                        true
                    )
                } else {
                    setChronometer(
                        R.id.widget_chronometer,
                        SystemClock.elapsedRealtime(),
                        "대기 중",
                        false
                    )
                }

                setOnClickPendingIntent(R.id.widget_root, openAppIntent(context, 0))
                setOnClickPendingIntent(R.id.widget_stop, widgetActionIntent(context, ACTION_STOP, null, 2))
                setOnClickPendingIntent(R.id.widget_toothbrush, widgetActionIntent(context, ACTION_TOOTHBRUSH, null, 3))
                setOnClickPendingIntent(R.id.widget_snack, widgetActionIntent(context, ACTION_SNACK, null, 4))
                setOnClickPendingIntent(R.id.widget_study, widgetActionIntent(context, ACTION_START, "STUDY", 5))
                setOnClickPendingIntent(R.id.widget_meal, widgetActionIntent(context, ACTION_START, "MEAL", 6))
                setOnClickPendingIntent(R.id.widget_school, widgetActionIntent(context, ACTION_START, "SCHOOL", 7))
                setOnClickPendingIntent(R.id.widget_exercise, widgetActionIntent(context, ACTION_START, "EXERCISE", 8))
                setOnClickPendingIntent(R.id.widget_sleep, widgetActionIntent(context, ACTION_START, "SLEEP", 9))
                setOnClickPendingIntent(R.id.widget_rest, widgetActionIntent(context, ACTION_START, "REST", 10))
                setOnClickPendingIntent(R.id.widget_etc, widgetActionIntent(context, ACTION_START, "ETC", 11))
            }
        }

        private fun openAppIntent(context: Context, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun widgetActionIntent(
            context: Context,
            action: String,
            category: String?,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, FlowlogWidgetActionReceiver::class.java).apply {
                this.action = action
                category?.let { putExtra(EXTRA_CATEGORY, it) }
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun runningStatusText(category: String): String {
            return when (category) {
                "STUDY" -> "공부를 하는 중입니다!"
                "MEAL" -> "식사를 하는 중입니다!"
                "EXERCISE" -> "운동을 하는 중입니다!"
                "SLEEP" -> "수면 중입니다!"
                "REST" -> "휴식 중입니다!"
                "SCHOOL" -> "학교 활동 중입니다!"
                "TODO" -> "할 일을 하는 중입니다!"
                else -> "${displayCategory(category)} 활동 중입니다!"
            }
        }
    }
}
