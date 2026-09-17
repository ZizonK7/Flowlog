package com.example.flowlog.data.local

import android.content.Context

object TimetablePlanSettingsStore {
    private const val PREFS_NAME = "timetable_plan_settings"
    private const val KEY_AUTO_PLACE_ENABLED = "auto_place_enabled"
    private const val KEY_REMINDER_ENABLED = "reminder_enabled"

    fun isAutoPlaceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_PLACE_ENABLED, true)

    fun setAutoPlaceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_PLACE_ENABLED, enabled).apply()
    }

    fun isReminderEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMINDER_ENABLED, true)

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
