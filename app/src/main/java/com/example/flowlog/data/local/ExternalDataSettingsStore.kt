package com.example.flowlog.data.local

import android.content.Context

/**
 * Switches and fetch times for data read from elsewhere on the phone. Both switches are
 * off by default: nothing is read until the user turns a source on in Settings.
 * Fetch times are kept per provider (`ExternalSampleProvider`).
 */
object ExternalDataSettingsStore {
    private const val PREFS_NAME = "external_data_settings"
    private const val KEY_HEALTH_CONNECT_ENABLED = "health_connect_enabled"
    // Usage statistics (UsageStatsCollector).
    private const val KEY_USAGE_STATS_ENABLED = "usage_stats_enabled"
    private const val SUFFIX_LAST_ATTEMPT_AT = "_last_attempt_at"
    private const val SUFFIX_LAST_FETCHED_AT = "_last_fetched_at"

    fun isHealthConnectEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HEALTH_CONNECT_ENABLED, false)

    fun setHealthConnectEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HEALTH_CONNECT_ENABLED, enabled).apply()
    }

    fun isUsageStatsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USAGE_STATS_ENABLED, false)

    fun setUsageStatsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_USAGE_STATS_ENABLED, enabled).apply()
    }

    /** Last time a fetch was started (success or not); decides when the next one is due. */
    fun lastAttemptAt(context: Context, provider: String): Long? =
        prefs(context).getLong(provider + SUFFIX_LAST_ATTEMPT_AT, 0L).takeIf { it > 0L }

    fun markAttempt(context: Context, provider: String, at: Long) {
        prefs(context).edit().putLong(provider + SUFFIX_LAST_ATTEMPT_AT, at).apply()
    }

    /** Last successful fetch; shown in Settings and sets the read window. */
    fun lastFetchedAt(context: Context, provider: String): Long? =
        prefs(context).getLong(provider + SUFFIX_LAST_FETCHED_AT, 0L).takeIf { it > 0L }

    fun markFetched(context: Context, provider: String, at: Long) {
        prefs(context).edit().putLong(provider + SUFFIX_LAST_FETCHED_AT, at).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
