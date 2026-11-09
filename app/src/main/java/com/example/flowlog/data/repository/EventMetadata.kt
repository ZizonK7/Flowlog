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
        /** ExternalDataSettingsStore.setHealthConnectEnabled */
        const val EXTERNAL_DATA_HEALTH_CONNECT = "external_data_health_connect_enabled"
        /** ExternalDataSettingsStore.setUsageStatsEnabled */
        const val EXTERNAL_DATA_USAGE_STATS = "external_data_usage_stats_enabled"
    }

    /** Notification kinds written in the `type` field of NOTIFICATION_* events. */
    object NotificationType {
        const val PLANNED_TODO_REMINDER = "PLANNED_TODO_REMINDER"
        const val TOOTHBRUSH_REMINDER = "TOOTHBRUSH_REMINDER"
    }

    /** `via` values of ACTIVITY_STARTED: which code path started the timer (one per caller). */
    object StartVia {
        const val MANUAL_BUTTON = "manual_button"
        const val APP_SHORTCUT = "app_shortcut"
        const val EXISTING_BUTTON_RECOMMENDATION = "existing_button_recommendation"
        const val SCHEDULED_AUTO_BUTTON_NOW = "scheduled_auto_button_now"
        const val OVERLAPPING = "overlapping"
        const val TODO = "todo"
        const val DAILY_CUE_ROUTINE = "daily_cue_routine"
        const val CALENDAR_PETITE = "calendar_petite"
        const val FLOW_RECOMMENDATION = "flow_recommendation"
        const val RECOMMENDED_TODO = "recommended_todo"
        const val AUTO_BUTTON_ALARM = "auto_button_alarm"
        const val STUDY_PLAN_AUTO_START = "study_plan_auto_start"
        const val WEB_START = "web_start"
        /** Not a new start: the auto-button undo brings the previous timer back. */
        const val AUTO_BUTTON_UNDO_RESTORE = "auto_button_undo_restore"
    }

    /** `via` values of ACTIVITY_SAVED: which code path wrote the record (one per caller). */
    object SaveVia {
        const val TIMER_STOP = "timer_stop"
        const val OVERLAPPING_STOP = "overlapping_stop"
        const val FILL_SLEEP = "fill_sleep"
        const val MANUAL_ADD = "manual_add"
        const val IMPORT = "import"
        const val DAILY_CUE_CHECK = "daily_cue_check"
        const val UNDO_RESTORE = "undo_restore"
        const val SEED_SLEEP = "seed_sleep"
        const val AUTO_BUTTON_END = "auto_button_end"
        /** The running (or pinned) timer is saved because an auto button starts over it. */
        const val AUTO_BUTTON_PREEMPT = "auto_button_preempt"
        const val STUDY_PLAN_AUTO_END = "study_plan_auto_end"
    }

    /** `titleSource` of ACTIVITY_SAVED: where the saved title came from. */
    object TitleSource {
        /** A chip of the title suggestions was picked and kept. */
        const val SUGGESTION = "suggestion"
        /** Entered by hand (or edited after picking a chip). */
        const val TYPED = "typed"
        /** No title given; the category's default title was saved. */
        const val DEFAULT = "default"
        /** Came with the start (to-do, routine, plan item, auto button title) and was kept. */
        const val PRESET = "preset"
        /** Not known on this screen: e.g. changed in the PC web app, or the app restarted mid-timer. */
        const val UNKNOWN = "unknown"
    }

    const val TITLE_RANKER_ALGORITHM = "title_ranker_v1"
    const val BUTTON_ENGINE_ALGORITHM = "button_engine_v1"

    /**
     * ACTIVITY_STARTED. [pinned] marks the second (overlapping) timer slot.
     * [reminderNotificationId]/[reminderClickedAt] are set when the start followed a click on
     * the inactivity reminder (`afterInactivityReminder: true`); otherwise those keys are left out.
     */
    fun activityStarted(
        via: String,
        category: String,
        startTime: Long,
        sourceType: String? = null,
        sourceId: String? = null,
        linkedTodoId: Long? = null,
        pendingTitle: String? = null,
        pinned: Boolean = false,
        reminderNotificationId: String? = null,
        reminderClickedAt: Long? = null
    ): String = json(
        "category" to category,
        "sourceType" to sourceType,
        "sourceId" to sourceId,
        "linkedTodoId" to linkedTodoId?.takeIf { it > 0L },
        "pendingTitle" to pendingTitle?.takeIf { it.isNotBlank() },
        "startTime" to startTime,
        "via" to via,
        "pinned" to (if (pinned) true else null),
        "afterInactivityReminder" to (if (reminderNotificationId != null) true else null),
        "reminderNotificationId" to reminderNotificationId,
        "reminderClickedAt" to reminderNotificationId?.let { reminderClickedAt }
    )

    fun titleSuggestionShown(category: String, slots: List<String>): String = json(
        "category" to category,
        "slots" to slots.take(MAX_TITLE_SLOTS),
        "algorithm" to TITLE_RANKER_ALGORITHM
    )

    fun titleSuggestionPicked(category: String, slot: Int, title: String): String = json(
        "category" to category,
        "slot" to slot,
        "title" to title
    )

    /**
     * Where the saved title came from. [choiceTitle]/[choiceSource] are the last title the
     * screen recorded a source for (picked chip, typed text, start preset); they count only
     * when the saved title is still that one.
     */
    fun titleSource(
        finalTitle: String,
        choiceTitle: String?,
        choiceSource: String?,
        defaultTitle: String
    ): String = when {
        choiceSource != null && choiceTitle == finalTitle -> choiceSource
        finalTitle == defaultTitle -> TitleSource.DEFAULT
        else -> TitleSource.UNKNOWN
    }

    /** ACTIVITY_SAVED. [slot] only for a kept suggestion; [shownSlots] left out when nothing was shown. */
    fun activitySaved(
        via: String,
        titleSource: String? = null,
        slot: Int? = null,
        shownSlots: List<String> = emptyList()
    ): String = json(
        "via" to via,
        "titleSource" to titleSource,
        "slot" to slot?.takeIf { titleSource == TitleSource.SUGGESTION },
        "shownSlots" to shownSlots.takeIf { it.isNotEmpty() }
    )

    /** The fields ACTIVITY_UPDATED compares. [note] is read only for its length. */
    data class ActivityFields(
        val title: String,
        val category: String,
        val note: String?,
        val startTime: Long,
        val endTime: Long,
        val isFavorite: Boolean,
        val linkedTodoId: Long?,
        val exerciseSetCount: Int = 0
    )

    /**
     * ACTIVITY_UPDATED: `{changed: {<field>: {from, to}}}` with only the fields that differ.
     * The note is never written — only `{fromLen, toLen}` (null counts as empty).
     * [before] null (row not found before the update) gives `{beforeFound: false}`.
     */
    fun activityChanged(before: ActivityFields?, after: ActivityFields): String {
        if (before == null) return json("beforeFound" to false)
        val changed = LinkedHashMap<String, Any?>()
        fun <T> diff(name: String, from: T, to: T) {
            if (from != to) changed[name] = linkedMapOf("from" to from, "to" to to)
        }
        diff("title", before.title, after.title)
        diff("category", before.category, after.category)
        val fromNote = before.note.orEmpty()
        val toNote = after.note.orEmpty()
        if (fromNote != toNote) changed["note"] = linkedMapOf("fromLen" to fromNote.length, "toLen" to toNote.length)
        diff("startTime", before.startTime, after.startTime)
        diff("endTime", before.endTime, after.endTime)
        diff("isFavorite", before.isFavorite, after.isFavorite)
        diff("linkedTodoId", before.linkedTodoId, after.linkedTodoId)
        diff("exerciseSetCount", before.exerciseSetCount, after.exerciseSetCount)
        return json("changed" to changed)
    }

    /** BUTTON_RECOMMENDATION_SHOWN / ACCEPTED / DISMISSED. */
    fun buttonRecommendation(category: String, messageId: String): String = json(
        "category" to category,
        "messageId" to messageId
    )

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
     * EXTERNAL_SAMPLES_FETCHED: one fetch of external samples. Keys as decided for this event
     * (snake_case `window_*`). [fetched] = rows read, [inserted]/[updated] = rows written;
     * the rest were unchanged. [error] = exception class name or a short reason; the counts
     * are 0 then. [trigger] = what started it, [kinds] = the record kinds read.
     * [screenEventsSupported] = false only for usage statistics below Android 9 (no screen
     * events there); left out otherwise.
     */
    fun externalSamplesFetched(
        provider: String,
        windowStart: Long,
        windowEnd: Long,
        fetched: Int,
        inserted: Int,
        updated: Int,
        error: String? = null,
        trigger: String? = null,
        kinds: List<String>? = null,
        screenEventsSupported: Boolean? = null
    ): String = json(
        "provider" to provider,
        "window_start" to windowStart,
        "window_end" to windowEnd,
        "fetched" to fetched,
        "inserted" to inserted,
        "updated" to updated,
        "error" to error,
        "trigger" to trigger,
        "kinds" to kinds,
        "screen_events_supported" to screenEventsSupported
    )

    /**
     * The version to report as `from` for MIGRATION_COMPLETED, or null when nothing should
     * be recorded: no stored version (first install, or first run of a build that tracks it)
     * or the stored version equals the one now open.
     */
    fun migrationFromVersion(storedVersion: Int?, openedVersion: Int): Int? =
        storedVersion?.takeIf { it > 0 && it != openedVersion }

    private const val MAX_TITLE_SLOTS = 5

    /**
     * A JSON object from [fields] in the given order; null values are skipped.
     * Values may be strings, numbers, booleans, maps (nested objects) and lists (arrays);
     * inside those, nulls are kept as JSON null.
     */
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

    private fun appendValue(sb: StringBuilder, value: Any?) {
        when (value) {
            null -> sb.append("null")
            is Map<*, *> -> {
                // Nested object: every entry is written, a null value as null (a diff's
                // `from: null` is a fact). Only top-level fields skip nulls.
                sb.append('{')
                var first = true
                for ((key, item) in value) {
                    if (!first) sb.append(',')
                    first = false
                    appendString(sb, key.toString())
                    sb.append(':')
                    appendValue(sb, item)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                // Arrays keep every element; a null element is written as null.
                sb.append('[')
                var first = true
                for (item in value) {
                    if (!first) sb.append(',')
                    first = false
                    appendValue(sb, item)
                }
                sb.append(']')
            }
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
