package com.example.flowlog.data.local

import android.content.Context
import java.util.Calendar

data class PendingInactivityReminderClick(
    val notificationId: String,
    val clickedAt: Long
)

object InactivityReminderStore {
    private const val PREFS_NAME = "inactivity_reminder"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_ACTIVITY_ENDED_AT = "last_activity_ended_at"
    private const val KEY_LAST_SHOWN_AT = "last_shown_at"
    private const val KEY_SHOWN_DATE_KEY = "shown_date_key"
    private const val KEY_SHOWN_DATE_COUNT = "shown_date_count"
    private const val KEY_PENDING_NOTIFICATION_ID = "pending_notification_id"
    private const val KEY_PENDING_CLICKED_AT = "pending_clicked_at"

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getLastActivityEndedAt(context: Context): Long =
        prefs(context).getLong(KEY_LAST_ACTIVITY_ENDED_AT, 0L)

    fun saveLastActivityEndedAt(context: Context, endedAt: Long) {
        prefs(context).edit().putLong(KEY_LAST_ACTIVITY_ENDED_AT, endedAt).apply()
    }

    fun getLastShownAt(context: Context): Long =
        prefs(context).getLong(KEY_LAST_SHOWN_AT, 0L)

    fun shownCountToday(context: Context, now: Long = System.currentTimeMillis()): Int {
        val preferences = prefs(context)
        return if (preferences.getLong(KEY_SHOWN_DATE_KEY, 0L) == dateKey(now)) {
            preferences.getInt(KEY_SHOWN_DATE_COUNT, 0)
        } else {
            0
        }
    }

    fun markShown(context: Context, notificationId: String, shownAt: Long) {
        val preferences = prefs(context)
        val todayKey = dateKey(shownAt)
        val previousKey = preferences.getLong(KEY_SHOWN_DATE_KEY, 0L)
        val previousCount = if (previousKey == todayKey) {
            preferences.getInt(KEY_SHOWN_DATE_COUNT, 0)
        } else {
            0
        }
        preferences.edit()
            .putLong(KEY_LAST_SHOWN_AT, shownAt)
            .putLong(KEY_SHOWN_DATE_KEY, todayKey)
            .putInt(KEY_SHOWN_DATE_COUNT, previousCount + 1)
            .apply()
    }

    fun markClicked(context: Context, notificationId: String, clickedAt: Long) {
        prefs(context).edit()
            .putString(KEY_PENDING_NOTIFICATION_ID, notificationId)
            .putLong(KEY_PENDING_CLICKED_AT, clickedAt)
            .apply()
    }

    fun consumePendingClick(context: Context): PendingInactivityReminderClick? {
        val preferences = prefs(context)
        val notificationId = preferences.getString(KEY_PENDING_NOTIFICATION_ID, null) ?: return null
        val clickedAt = preferences.getLong(KEY_PENDING_CLICKED_AT, 0L)
        if (clickedAt <= 0L) return null
        preferences.edit()
            .remove(KEY_PENDING_NOTIFICATION_ID)
            .remove(KEY_PENDING_CLICKED_AT)
            .apply()
        return PendingInactivityReminderClick(notificationId = notificationId, clickedAt = clickedAt)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun dateKey(timeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
