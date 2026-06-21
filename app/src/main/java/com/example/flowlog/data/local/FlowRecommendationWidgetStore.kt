package com.example.flowlog.data.local

import android.content.Context
import java.util.Calendar

data class WidgetFlowRecommendation(
    val title: String,
    val category: String
)

object FlowRecommendationWidgetStore {
    private const val PREFS_NAME = "flow_recommendation_widget"
    private const val KEY_TITLE = "title"
    private const val KEY_CATEGORY = "category"
    private const val KEY_DATE_KEY = "date_key"

    fun save(context: Context, title: String, category: String, now: Long = System.currentTimeMillis()) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TITLE, title)
            .putString(KEY_CATEGORY, category)
            .putLong(KEY_DATE_KEY, dateKey(now))
            .apply()
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun get(context: Context, now: Long = System.currentTimeMillis()): WidgetFlowRecommendation? {
        val preferences = context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        if (preferences.getLong(KEY_DATE_KEY, Long.MIN_VALUE) != dateKey(now)) return null
        val title = preferences.getString(KEY_TITLE, null)?.takeIf { it.isNotBlank() } ?: return null
        val category = preferences.getString(KEY_CATEGORY, null).orEmpty()
        return WidgetFlowRecommendation(title = title, category = category)
    }

    private fun dateKey(timeMillis: Long): Long = Calendar.getInstance().run {
        timeInMillis = timeMillis
        get(Calendar.YEAR) * 10_000L +
            (get(Calendar.MONTH) + 1) * 100L +
            get(Calendar.DAY_OF_MONTH)
    }
}
