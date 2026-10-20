package com.example.flowlog.shortcut

import com.example.flowlog.data.model.MainButtonConfig
import com.example.flowlog.data.model.MainButtonItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerShortcutRulesTest {
    private val action = TimerShortcutRules.ACTION_START_TIMER

    @Test
    fun shortcutCategoriesFollowButtonOrderAndStopAtFour() {
        val config = config("REST" to 4, "STUDY" to 0, "WORK" to 1, "MEAL" to 2, "SLEEP" to 3)

        assertEquals(listOf("STUDY", "WORK", "MEAL", "SLEEP"), TimerShortcutRules.shortcutCategories(config))
    }

    @Test
    fun shortcutCategoriesSkipUnknownAndDuplicateCategories() {
        val config = config("STUDY" to 0, "SNACK" to 1, "STUDY" to 2, "UNKNOWN" to 3, "WORK" to 4)

        assertEquals(listOf("STUDY", "WORK"), TimerShortcutRules.shortcutCategories(config))
        assertEquals(emptyList<String>(), TimerShortcutRules.shortcutCategories(MainButtonConfig.EMPTY))
    }

    @Test
    fun knownCategoryWithStartActionIsAccepted() {
        assertEquals("STUDY", TimerShortcutRules.parseStartCategory(action, "STUDY", launchedFromHistory = false))
    }

    @Test
    fun otherActionsUnknownCategoriesAndMissingExtraAreRejected() {
        assertNull(TimerShortcutRules.parseStartCategory("android.intent.action.MAIN", "STUDY", false))
        assertNull(TimerShortcutRules.parseStartCategory(null, "STUDY", false))
        assertNull(TimerShortcutRules.parseStartCategory(action, "DROP TABLE", false))
        assertNull(TimerShortcutRules.parseStartCategory(action, "study", false))
        assertNull(TimerShortcutRules.parseStartCategory(action, "SNACK", false))
        assertNull(TimerShortcutRules.parseStartCategory(action, null, false))
    }

    @Test
    fun intentReplayedFromRecentAppsIsIgnored() {
        assertNull(TimerShortcutRules.parseStartCategory(action, "STUDY", launchedFromHistory = true))
    }

    private fun config(vararg buttons: Pair<String, Int>) = MainButtonConfig(
        buttons = buttons.map { (category, order) -> MainButtonItem(category = category, order = order) },
        configured = true,
        version = MainButtonConfig.CURRENT_VERSION
    )
}
