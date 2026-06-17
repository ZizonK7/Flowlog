package com.example.flowlog.data.local

import android.content.Context
import org.json.JSONArray
import java.util.Calendar

object DailyCueCompletionStore {
    private const val PREFS_DAILY_CUES = "daily_cues"
    private const val KEY_DATE = "date_key"
    private const val KEY_ITEMS = "items_json"

    fun markCompletedToday(context: Context, cueId: Long): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_DAILY_CUES, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_DATE, 0L) != startOfDay(System.currentTimeMillis())) return false
        val rawItems = prefs.getString(KEY_ITEMS, null) ?: return false

        return runCatching {
            val items = JSONArray(rawItems)
            var changed = false
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                if (item.optLong("id") == cueId && !item.optBoolean("isCompleted", false)) {
                    item.put("isCompleted", true)
                    changed = true
                }
            }
            if (changed) {
                prefs.edit().putString(KEY_ITEMS, items.toString()).apply()
            }
            changed
        }.getOrDefault(false)
    }

    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
