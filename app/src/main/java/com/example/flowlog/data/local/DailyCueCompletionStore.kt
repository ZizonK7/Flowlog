package com.example.flowlog.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONArray
import java.util.Calendar

object DailyCueCompletionStore {
    private const val PREFS_DAILY_CUES = "daily_cues"
    private const val KEY_DATE = "date_key"
    private const val KEY_ITEMS = "items_json"
    private const val KEY_PREV_DATE = "prev_date_key"
    private const val KEY_PREV_ITEMS = "prev_items_json"

    fun markCompletedToday(context: Context, cueId: Long): Boolean =
        markCompletedIn(context, cueId, KEY_DATE, KEY_ITEMS, startOfDay(System.currentTimeMillis()))

    // targetDateKey가 오늘(date_key) 또는 어제(prev_date_key) 스냅샷 중 어디에 해당하는지 찾아 완료 처리한다.
    // 둘 다 아니면(2일 이상 앱을 열지 않은 경우 등) no-op.
    fun markCompletedForDate(context: Context, cueId: Long, targetDateKey: Long): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_DAILY_CUES, Context.MODE_PRIVATE)
        return when (targetDateKey) {
            prefs.getLong(KEY_DATE, 0L) -> markCompletedIn(context, cueId, KEY_DATE, KEY_ITEMS, targetDateKey)
            prefs.getLong(KEY_PREV_DATE, 0L) -> markCompletedIn(context, cueId, KEY_PREV_DATE, KEY_PREV_ITEMS, targetDateKey)
            else -> false
        }
    }

    private fun markCompletedIn(
        context: Context,
        cueId: Long,
        dateKeyPref: String,
        itemsKeyPref: String,
        expectedDateKey: Long
    ): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_DAILY_CUES, Context.MODE_PRIVATE)
        if (prefs.getLong(dateKeyPref, 0L) != expectedDateKey) return false
        val rawItems = prefs.getString(itemsKeyPref, null) ?: return false

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
                prefs.edit().putString(itemsKeyPref, items.toString()).apply()
            }
            changed
        }.getOrDefault(false)
    }

    fun completedIdsToday(context: Context): Set<Long> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_DAILY_CUES, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_DATE, 0L) != startOfDay(System.currentTimeMillis())) return emptySet()
        val rawItems = prefs.getString(KEY_ITEMS, null) ?: return emptySet()
        return runCatching {
            val items = JSONArray(rawItems)
            buildSet {
                for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    if (item.optBoolean("isCompleted", false)) add(item.optLong("id"))
                }
            }
        }.getOrDefault(emptySet())
    }

    fun observeCompletedIdsToday(context: Context): Flow<Set<Long>> = callbackFlow {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_DAILY_CUES, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DATE || key == KEY_ITEMS) {
                trySend(completedIdsToday(context))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(completedIdsToday(context))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

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
