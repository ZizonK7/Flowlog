package com.example.flowlog.shortcut

import com.example.flowlog.data.model.MainButtonConfig

/**
 * Pure rules for the home-screen "start timer" shortcuts: which categories get a
 * shortcut, and which incoming intents are accepted. No Android APIs.
 */
object TimerShortcutRules {
    const val ACTION_START_TIMER = "com.example.flowlog.action.START_TIMER"
    const val EXTRA_CATEGORY = "category"
    const val MAX_SHORTCUTS = 4
    const val SHORTCUT_ID_PREFIX = "start_timer_"

    /** First [limit] main buttons (by order) that are known timer categories. */
    fun shortcutCategories(
        config: MainButtonConfig,
        limit: Int = MAX_SHORTCUTS
    ): List<String> {
        return config.buttons
            .sortedBy { it.order }
            .map { it.category }
            .filter { it in MainButtonConfig.ALL_SELECTABLE_CATEGORIES }
            .distinct()
            .take(limit.coerceAtLeast(0))
    }

    /**
     * Returns the category to start, or null when the intent is not a valid
     * start-timer request. MainActivity is exported (launcher entry), so any app
     * can send it an intent: only the exact action and a known category pass.
     * Intents replayed from the recent-apps list are ignored so reopening the
     * app does not start the timer again.
     */
    fun parseStartCategory(
        action: String?,
        category: String?,
        launchedFromHistory: Boolean
    ): String? {
        if (action != ACTION_START_TIMER || launchedFromHistory) return null
        return category?.takeIf { it in MainButtonConfig.ALL_SELECTABLE_CATEGORIES }
    }
}
