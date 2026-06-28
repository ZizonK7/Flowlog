package com.example.flowlog.data.local

import android.content.Context

data class FocusModeState(
    val startedAtMillis: Long,
    val endsAtMillis: Long
)

object FocusModeStore {
    const val DEFAULT_FOCUS_DURATION_MILLIS = 25L * 60L * 1000L

    private const val PREFS_NAME = "focus_mode_state"
    private const val KEY_FOCUS_DURATION = "focus_duration_millis"

    fun getFocusDurationMillis(context: Context): Long =
        prefs(context).getLong(KEY_FOCUS_DURATION, DEFAULT_FOCUS_DURATION_MILLIS)

    fun setFocusDurationMillis(context: Context, millis: Long) {
        prefs(context).edit().putLong(KEY_FOCUS_DURATION, millis).apply()
    }

    fun getFocusDurationLabel(context: Context): String {
        val millis = getFocusDurationMillis(context)
        return when {
            millis < 60_000L -> "${millis / 1000}초"
            millis % 3_600_000L == 0L -> "${millis / 3_600_000}시간"
            else -> "${millis / 60_000}분"
        }
    }
    private const val KEY_ENDS_AT = "ends_at"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_SOUND_ENABLED = "notification_sound_enabled"
    private const val KEY_CONFIRM_ACKNOWLEDGED = "focus_confirm_acknowledged"
    private const val KEY_ENABLE_DND = "enable_system_dnd_for_focus"
    private const val KEY_PREVIOUS_DND_FILTER = "previous_interruption_filter"
    private const val KEY_DND_CHANGED = "dnd_changed_by_focus"

    fun getFocusModeState(context: Context): FocusModeState? {
        val prefs = prefs(context)
        val endsAt = prefs.getLong(KEY_ENDS_AT, 0L)
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        if (endsAt == 0L || startedAt == 0L) return null
        return FocusModeState(startedAtMillis = startedAt, endsAtMillis = endsAt)
    }

    fun isFocusModeActive(context: Context): Boolean {
        val state = getFocusModeState(context) ?: return false
        return state.endsAtMillis > System.currentTimeMillis()
    }

    fun saveFocusModeActive(context: Context, startedAt: Long, endsAt: Long) {
        prefs(context).edit()
            .putLong(KEY_STARTED_AT, startedAt)
            .putLong(KEY_ENDS_AT, endsAt)
            .apply()
    }

    fun clearFocusMode(context: Context) {
        prefs(context).edit()
            .remove(KEY_STARTED_AT)
            .remove(KEY_ENDS_AT)
            .apply()
    }

    // ── 알림 소리 전역 설정 ──────────────────────────────────────

    fun isNotificationSoundEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SOUND_ENABLED, true)

    fun setNotificationSoundEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    /** Regular sounds play when global sound is on AND focus mode is inactive. */
    fun shouldPlayRegularSound(context: Context): Boolean =
        isNotificationSoundEnabled(context) && !isFocusModeActive(context)

    // ── 첫 실행 확인 다이얼로그 ──────────────────────────────────

    fun isFocusConfirmAcknowledged(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CONFIRM_ACKNOWLEDGED, false)

    fun setFocusConfirmAcknowledged(context: Context) {
        prefs(context).edit().putBoolean(KEY_CONFIRM_ACKNOWLEDGED, true).apply()
    }

    // ── 시스템 방해금지 설정 ─────────────────────────────────────

    fun getEnableSystemDndForFocus(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLE_DND, false)

    fun setEnableSystemDndForFocus(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLE_DND, enabled).apply()
    }

    fun savePreviousDndFilter(context: Context, filter: Int) {
        prefs(context).edit().putInt(KEY_PREVIOUS_DND_FILTER, filter).apply()
    }

    /** Returns null if no previous filter was saved. */
    fun getPreviousDndFilter(context: Context): Int? {
        val p = prefs(context)
        if (!p.contains(KEY_PREVIOUS_DND_FILTER)) return null
        val v = p.getInt(KEY_PREVIOUS_DND_FILTER, -1)
        return if (v == -1) null else v
    }

    fun setDndChangedByFocus(context: Context, changed: Boolean) {
        prefs(context).edit().putBoolean(KEY_DND_CHANGED, changed).apply()
    }

    fun isDndChangedByFocus(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DND_CHANGED, false)

    /** Clears DND tracking state (called after restoring DND). */
    fun clearDndState(context: Context) {
        prefs(context).edit()
            .remove(KEY_PREVIOUS_DND_FILTER)
            .remove(KEY_DND_CHANGED)
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
