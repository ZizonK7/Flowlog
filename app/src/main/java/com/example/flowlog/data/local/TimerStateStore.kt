package com.example.flowlog.data.local

import android.content.Context
import android.os.SystemClock

data class ActiveTimerState(
    val category: String,
    val startTime: Long,
    val status: TimerStatus = TimerStatus.RUNNING,
    val pausedElapsedMillis: Long = 0L,
    val linkedTodoId: Long? = null,
    val linkedTodoTitle: String? = null
) {
    val elapsedMillis: Long
        get() = if (status == TimerStatus.PAUSED) {
            pausedElapsedMillis.coerceAtLeast(0L)
        } else {
            (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
        }

    val chronometerBaseElapsedRealtime: Long
        get() = SystemClock.elapsedRealtime() - elapsedMillis
}

enum class TimerStatus {
    RUNNING,
    PAUSED
}

object TimerStateStore {
    private const val PREFS_TIMER_STATE = "timer_state"
    private const val KEY_ACTIVE_CATEGORY = "active_category"
    private const val KEY_ACTIVE_START_TIME = "active_start_time"
    private const val KEY_ACTIVE_STATUS = "active_status"
    private const val KEY_PAUSED_ELAPSED_MILLIS = "paused_elapsed_millis"
    private const val KEY_ACTIVE_TODO_ID = "active_todo_id"
    private const val KEY_ACTIVE_TODO_TITLE = "active_todo_title"
    private const val NO_TODO_ID = -1L

    fun getActiveTimer(context: Context): ActiveTimerState? {
        val preferences = context.applicationContext.getSharedPreferences(
            PREFS_TIMER_STATE,
            Context.MODE_PRIVATE
        )
        val category = preferences.getString(KEY_ACTIVE_CATEGORY, null) ?: return null
        val startTime = preferences.getLong(KEY_ACTIVE_START_TIME, 0L)
        if (startTime == 0L) return null

        val status = runCatching {
            TimerStatus.valueOf(preferences.getString(KEY_ACTIVE_STATUS, TimerStatus.RUNNING.name).orEmpty())
        }.getOrDefault(TimerStatus.RUNNING)
        val pausedElapsedMillis = preferences.getLong(KEY_PAUSED_ELAPSED_MILLIS, 0L)
        val linkedTodoId = preferences.getLong(KEY_ACTIVE_TODO_ID, NO_TODO_ID)
            .takeUnless { it == NO_TODO_ID }
        val linkedTodoTitle = preferences.getString(KEY_ACTIVE_TODO_TITLE, null)

        return ActiveTimerState(
            category = category,
            startTime = startTime,
            status = status,
            pausedElapsedMillis = pausedElapsedMillis,
            linkedTodoId = linkedTodoId,
            linkedTodoTitle = linkedTodoTitle
        )
    }

    fun saveActiveTimer(
        context: Context,
        category: String,
        startTime: Long,
        linkedTodoId: Long? = null,
        linkedTodoTitle: String? = null
    ) {
        context.applicationContext.getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_CATEGORY, category)
            .putLong(KEY_ACTIVE_START_TIME, startTime)
            .putString(KEY_ACTIVE_STATUS, TimerStatus.RUNNING.name)
            .remove(KEY_PAUSED_ELAPSED_MILLIS)
            .putLong(KEY_ACTIVE_TODO_ID, linkedTodoId ?: NO_TODO_ID)
            .putString(KEY_ACTIVE_TODO_TITLE, linkedTodoTitle)
            .apply()
    }

    fun pauseActiveTimer(context: Context, elapsedMillis: Long) {
        context.applicationContext.getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_STATUS, TimerStatus.PAUSED.name)
            .putLong(KEY_PAUSED_ELAPSED_MILLIS, elapsedMillis.coerceAtLeast(0L))
            .apply()
    }

    fun clearActiveTimer(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACTIVE_CATEGORY)
            .remove(KEY_ACTIVE_START_TIME)
            .remove(KEY_ACTIVE_STATUS)
            .remove(KEY_PAUSED_ELAPSED_MILLIS)
            .remove(KEY_ACTIVE_TODO_ID)
            .remove(KEY_ACTIVE_TODO_TITLE)
            .apply()
    }
}
