package com.example.flowlog.data.local

import android.content.Context
import android.os.SystemClock
import com.example.flowlog.data.constants.ActivitySourceType

data class ActiveTimerState(
    val category: String,
    val startTime: Long,
    val goalMillis: Long = TimerStateStore.DEFAULT_GOAL_MILLIS,
    val status: TimerStatus = TimerStatus.RUNNING,
    val pausedElapsedMillis: Long = 0L,
    val linkedTodoId: Long? = null,
    val linkedTodoTitle: String? = null,
    val pendingNote: String? = null,
    val dailyCueId: Long? = null,
    val sourceType: String = ActivitySourceType.MANUAL,
    val sourceId: String? = null
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
    private const val KEY_ACTIVE_GOAL_MILLIS = "active_goal_millis"
    private const val KEY_ACTIVE_STATUS = "active_status"
    private const val KEY_PAUSED_ELAPSED_MILLIS = "paused_elapsed_millis"
    private const val KEY_ACTIVE_TODO_ID = "active_todo_id"
    private const val KEY_ACTIVE_TODO_TITLE = "active_todo_title"
    private const val KEY_ACTIVE_PENDING_NOTE = "active_pending_note"
    private const val KEY_ACTIVE_DAILY_CUE_ID = "active_daily_cue_id"
    private const val KEY_ACTIVE_SOURCE_TYPE = "active_source_type"
    private const val KEY_ACTIVE_SOURCE_ID = "active_source_id"
    private const val KEY_PINNED_CATEGORY = "pinned_category"
    private const val KEY_PINNED_START_TIME = "pinned_start_time"
    private const val KEY_PINNED_GOAL_MILLIS = "pinned_goal_millis"
    private const val NO_TODO_ID = -1L
    const val DEFAULT_GOAL_MILLIS = 2L * 60L * 60L * 1000L

    fun getActiveTimer(context: Context): ActiveTimerState? {
        val preferences = context.applicationContext.getSharedPreferences(
            PREFS_TIMER_STATE,
            Context.MODE_PRIVATE
        )
        val category = preferences.getString(KEY_ACTIVE_CATEGORY, null) ?: return null
        val startTime = preferences.getLong(KEY_ACTIVE_START_TIME, 0L)
        if (startTime == 0L) return null
        val goalMillis = preferences.getLong(KEY_ACTIVE_GOAL_MILLIS, DEFAULT_GOAL_MILLIS)
            .coerceAtLeast(0L)

        val status = runCatching {
            TimerStatus.valueOf(preferences.getString(KEY_ACTIVE_STATUS, TimerStatus.RUNNING.name).orEmpty())
        }.getOrDefault(TimerStatus.RUNNING)
        val pausedElapsedMillis = preferences.getLong(KEY_PAUSED_ELAPSED_MILLIS, 0L)
        val linkedTodoId = preferences.getLong(KEY_ACTIVE_TODO_ID, NO_TODO_ID)
            .takeUnless { it == NO_TODO_ID }
        val linkedTodoTitle = preferences.getString(KEY_ACTIVE_TODO_TITLE, null)
        val pendingNote = preferences.getString(KEY_ACTIVE_PENDING_NOTE, null)
        val dailyCueId = preferences.getLong(KEY_ACTIVE_DAILY_CUE_ID, NO_TODO_ID)
            .takeUnless { it == NO_TODO_ID }
        val sourceType = preferences.getString(KEY_ACTIVE_SOURCE_TYPE, ActivitySourceType.MANUAL)
            ?: ActivitySourceType.MANUAL
        val sourceId = preferences.getString(KEY_ACTIVE_SOURCE_ID, null)

        return ActiveTimerState(
            category = category,
            startTime = startTime,
            goalMillis = goalMillis,
            status = status,
            pausedElapsedMillis = pausedElapsedMillis,
            linkedTodoId = linkedTodoId,
            linkedTodoTitle = linkedTodoTitle,
            pendingNote = pendingNote,
            dailyCueId = dailyCueId,
            sourceType = sourceType,
            sourceId = sourceId
        )
    }

    fun saveActiveTimer(
        context: Context,
        category: String,
        startTime: Long,
        goalMillis: Long = DEFAULT_GOAL_MILLIS,
        linkedTodoId: Long? = null,
        linkedTodoTitle: String? = null,
        pendingNote: String? = null,
        dailyCueId: Long? = null,
        sourceType: String = ActivitySourceType.MANUAL,
        sourceId: String? = null
    ) {
        context.applicationContext.getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_CATEGORY, category)
            .putLong(KEY_ACTIVE_START_TIME, startTime)
            .putLong(KEY_ACTIVE_GOAL_MILLIS, goalMillis.coerceAtLeast(0L))
            .putString(KEY_ACTIVE_STATUS, TimerStatus.RUNNING.name)
            .remove(KEY_PAUSED_ELAPSED_MILLIS)
            .putLong(KEY_ACTIVE_TODO_ID, linkedTodoId ?: NO_TODO_ID)
            .putString(KEY_ACTIVE_TODO_TITLE, linkedTodoTitle)
            .putString(KEY_ACTIVE_PENDING_NOTE, pendingNote)
            .putLong(KEY_ACTIVE_DAILY_CUE_ID, dailyCueId ?: NO_TODO_ID)
            .putString(KEY_ACTIVE_SOURCE_TYPE, sourceType)
            .putString(KEY_ACTIVE_SOURCE_ID, sourceId)
            .apply()
    }

    fun getPinnedTimer(context: Context): ActiveTimerState? {
        val preferences = context.applicationContext.getSharedPreferences(
            PREFS_TIMER_STATE,
            Context.MODE_PRIVATE
        )
        val category = preferences.getString(KEY_PINNED_CATEGORY, null) ?: return null
        val startTime = preferences.getLong(KEY_PINNED_START_TIME, 0L)
        if (startTime == 0L) return null
        val goalMillis = preferences.getLong(KEY_PINNED_GOAL_MILLIS, DEFAULT_GOAL_MILLIS)
            .coerceAtLeast(1L)

        return ActiveTimerState(
            category = category,
            startTime = startTime,
            goalMillis = goalMillis,
            status = TimerStatus.RUNNING
        )
    }

    fun savePinnedTimer(
        context: Context,
        category: String,
        startTime: Long,
        goalMillis: Long = DEFAULT_GOAL_MILLIS
    ) {
        context.applicationContext.getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PINNED_CATEGORY, category)
            .putLong(KEY_PINNED_START_TIME, startTime)
            .putLong(KEY_PINNED_GOAL_MILLIS, goalMillis.coerceAtLeast(1L))
            .apply()
    }

    fun clearPinnedTimer(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PINNED_CATEGORY)
            .remove(KEY_PINNED_START_TIME)
            .remove(KEY_PINNED_GOAL_MILLIS)
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
            .remove(KEY_ACTIVE_GOAL_MILLIS)
            .remove(KEY_ACTIVE_STATUS)
            .remove(KEY_PAUSED_ELAPSED_MILLIS)
            .remove(KEY_ACTIVE_TODO_ID)
            .remove(KEY_ACTIVE_TODO_TITLE)
            .remove(KEY_ACTIVE_PENDING_NOTE)
            .remove(KEY_ACTIVE_DAILY_CUE_ID)
            .remove(KEY_ACTIVE_SOURCE_TYPE)
            .remove(KEY_ACTIVE_SOURCE_ID)
            .apply()
    }
}
