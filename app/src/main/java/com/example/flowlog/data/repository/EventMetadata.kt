package com.example.flowlog.data.repository

/**
 * Builds the `metadataJson` strings passed to [EventLogRepository.log].
 *
 * Pure Kotlin (no `org.json`, no Android classes) so it runs in JVM unit tests
 * (`EventMetadataTest`). Metadata holds facts that anyone would read the same way —
 * times, kinds, counts, what was shown or chosen — not interpretations.
 * Fields whose value is null are left out.
 */
object EventMetadata {

    /** Keys for SETTING_CHANGED events: `<store name without Store/SettingsStore/Repository>_<setter without "set">`, snake_case. */
    object SettingKey {
        /** FocusModeStore.setNotificationSoundEnabled */
        const val NOTIFICATION_SOUND = "focus_mode_notification_sound_enabled"
        /** InactivityReminderStore.setEnabled */
        const val INACTIVITY_REMINDER = "inactivity_reminder_enabled"
        /** TimetablePlanSettingsStore.setAutoPlaceEnabled */
        const val TIMETABLE_AUTO_PLACE = "timetable_plan_auto_place_enabled"
        /** TimetablePlanSettingsStore.setReminderEnabled */
        const val TIMETABLE_REMINDER = "timetable_plan_reminder_enabled"
        /** AutoButtonScheduleRepository.setEnabled (per schedule; entityId = scheduleId) */
        const val AUTO_BUTTON_SCHEDULE = "auto_button_schedule_enabled"
    }

    /** Notification kinds written in the `type` field of NOTIFICATION_* events. */
    object NotificationType {
        const val PLANNED_TODO_REMINDER = "PLANNED_TODO_REMINDER"
        const val TOOTHBRUSH_REMINDER = "TOOTHBRUSH_REMINDER"
    }

    fun activityStopped(reason: String, category: String? = null, elapsedMs: Long? = null): String = json(
        "reason" to reason,
        "category" to category?.takeIf { it.isNotBlank() },
        "elapsedMs" to elapsedMs
    )

    fun favoriteToggled(isFavorite: Boolean): String = json("isFavorite" to isFavorite)

    /**
     * DAILY_GOAL_ITEM_CLICKED / COMPLETED / SKIPPED. [via] names the repository call that
     * changed the row; [rows] is how many daily_goal_items rows it changed.
     */
    fun dailyGoalItem(
        todoId: String?,
        via: String,
        rows: Int,
        dateKey: String? = null,
        recommendationId: String? = null
    ): String = json(
        "todoId" to todoId,
        "dateKey" to dateKey,
        "recommendationId" to recommendationId,
        "via" to via,
        "rows" to rows
    )

    fun notificationShown(
        type: String,
        notificationId: String,
        shownAt: Long,
        extra: Map<String, Any?> = emptyMap()
    ): String = json(
        *(listOf<Pair<String, Any?>>(
            "type" to type,
            "notificationId" to notificationId,
            "shownAt" to shownAt
        ) + extra.toList()).toTypedArray()
    )

    fun notificationClicked(type: String, notificationId: String, clickedAt: Long): String = json(
        "type" to type,
        "notificationId" to notificationId,
        "clickedAt" to clickedAt
    )

    fun notificationDismissed(type: String, notificationId: String, dismissedAt: Long, shownAt: Long? = null): String = json(
        "type" to type,
        "notificationId" to notificationId,
        "shownAt" to shownAt,
        "dismissedAt" to dismissedAt
    )

    fun developerMode(reason: String): String = json("reason" to reason)

    fun settingChanged(key: String, value: Boolean): String = json("key" to key, "value" to value)

    fun exportCreated(format: String, count: Int): String = json("format" to format, "count" to count)

    fun syncStarted(mode: String): String = json("mode" to mode)

    fun syncFinished(
        mode: String,
        attempted: Int,
        success: Int,
        failure: Int,
        deferred: Boolean,
        error: String? = null
    ): String = json(
        "mode" to mode,
        "attempted" to attempted,
        "success" to success,
        "failure" to failure,
        "deferred" to deferred,
        "error" to error
    )

    /** A sync counts as failed when any item failed or the call threw. */
    fun isSyncFailure(failureCount: Int, threw: Boolean): Boolean = threw || failureCount > 0

    fun migrationCompleted(from: Int, to: Int): String = json("from" to from, "to" to to)

    /**
     * The version to report as `from` for MIGRATION_COMPLETED, or null when nothing should
     * be recorded: no stored version (first install, or first run of a build that tracks it)
     * or the stored version equals the one now open.
     */
    fun migrationFromVersion(storedVersion: Int?, openedVersion: Int): Int? =
        storedVersion?.takeIf { it > 0 && it != openedVersion }

    /** A JSON object from [fields] in the given order; null values are skipped. */
    fun json(vararg fields: Pair<String, Any?>): String {
        val sb = StringBuilder("{")
        var first = true
        for ((key, value) in fields) {
            if (value == null) continue
            if (!first) sb.append(',')
            first = false
            appendString(sb, key)
            sb.append(':')
            appendValue(sb, value)
        }
        return sb.append('}').toString()
    }

    private fun appendValue(sb: StringBuilder, value: Any) {
        when (value) {
            is Boolean, is Int, is Long, is Short, is Byte -> sb.append(value.toString())
            is Double -> if (value.isFinite()) sb.append(value.toString()) else sb.append("null")
            is Float -> if (value.isFinite()) sb.append(value.toString()) else sb.append("null")
            is Number -> sb.append(value.toString())
            else -> appendString(sb, value.toString())
        }
    }

    private fun appendString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c < ' ') {
                    sb.append("\\u").append(String.format("%04x", c.code))
                } else {
                    sb.append(c)
                }
            }
        }
        sb.append('"')
    }
}
