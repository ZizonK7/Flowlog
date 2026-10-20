package com.example.flowlog.ui.viewmodel

import com.example.flowlog.data.model.ActivitySession
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Why a manually entered past activity cannot be saved. */
enum class ManualActivityError {
    END_NOT_AFTER_START,
    TOO_LONG,
    IN_FUTURE
}

/**
 * Result of checking a manually entered activity.
 * [error] blocks saving; [overlaps] is only a warning (saving is still allowed).
 */
data class ManualActivityValidation(
    val error: ManualActivityError?,
    val overlaps: List<ActivitySession>
) {
    val canSave: Boolean get() = error == null
    val hasOverlapWarning: Boolean get() = overlaps.isNotEmpty()
}

/**
 * Pure checks for "add a past activity by hand". No Android or ViewModel state.
 */
object ManualActivityValidator {
    val MAX_DURATION_MILLIS: Long = TimeUnit.HOURS.toMillis(24)

    fun validate(
        startMillis: Long,
        endMillis: Long,
        nowMillis: Long,
        existing: List<ActivitySession>
    ): ManualActivityValidation {
        val error = when {
            endMillis <= startMillis -> ManualActivityError.END_NOT_AFTER_START
            endMillis - startMillis > MAX_DURATION_MILLIS -> ManualActivityError.TOO_LONG
            endMillis > nowMillis -> ManualActivityError.IN_FUTURE
            else -> null
        }
        val overlaps = if (endMillis > startMillis) {
            findOverlaps(startMillis, endMillis, existing)
        } else {
            emptyList()
        }
        return ManualActivityValidation(error = error, overlaps = overlaps)
    }

    /**
     * Saved records whose time range intersects [startMillis, endMillis).
     * Touching edges (one ends exactly when the other starts) do not count, and
     * zero-length check records are ignored.
     */
    fun findOverlaps(
        startMillis: Long,
        endMillis: Long,
        existing: List<ActivitySession>
    ): List<ActivitySession> {
        return existing
            .filter { it.endTime > it.startTime }
            .filter { it.startTime < endMillis && it.endTime > startMillis }
            .sortedBy { it.startTime }
    }

    /**
     * Turns a picked day plus start/end minutes-of-day into epoch millis.
     * If the end minute is earlier than the start minute, the end is placed on
     * the next day (for example sleep from 23:30 to 07:00). Equal minutes stay on
     * the same day, so the result has zero length and fails validation instead
     * of silently becoming a 24-hour record.
     *
     * @param dayStartMillis any time on the picked day; only its date is used.
     */
    fun resolveTimes(
        dayStartMillis: Long,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Pair<Long, Long> {
        val start = atMinuteOfDay(dayStartMillis, startMinuteOfDay, timeZone, addDays = 0)
        val endDayOffset = if (endMinuteOfDay < startMinuteOfDay) 1 else 0
        val end = atMinuteOfDay(dayStartMillis, endMinuteOfDay, timeZone, addDays = endDayOffset)
        return start to end
    }

    /** True when both instants fall on the same local calendar date in [timeZone]. */
    fun isSameLocalDay(
        firstMillis: Long,
        secondMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val first = Calendar.getInstance(timeZone).apply { timeInMillis = firstMillis }
        val second = Calendar.getInstance(timeZone).apply { timeInMillis = secondMillis }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }

    private fun atMinuteOfDay(
        dayMillis: Long,
        minuteOfDay: Int,
        timeZone: TimeZone,
        addDays: Int
    ): Long {
        val clamped = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (addDays != 0) add(Calendar.DAY_OF_YEAR, addDays)
            set(Calendar.HOUR_OF_DAY, clamped / 60)
            set(Calendar.MINUTE, clamped % 60)
        }.timeInMillis
    }
}
