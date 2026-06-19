package com.example.flowlog.ui.viewmodel

import com.example.flowlog.data.model.ActivitySession
import java.util.Calendar
import java.util.TimeZone

/**
 * Creates calculation-only activity slices at local midnight boundaries.
 * Stored activity records are never changed.
 */
internal fun splitActivitiesAcrossDays(
    activities: List<ActivitySession>,
    rangeStartMillis: Long,
    rangeEndMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault()
): List<ActivitySession> {
    if (rangeEndMillis <= rangeStartMillis) return emptyList()

    return activities.flatMap { activity ->
        val activityEnd = activity.endTime.takeIf { it > activity.startTime }
            ?: (activity.startTime + activity.durationMillis.coerceAtLeast(0L))
        val clippedStart = maxOf(activity.startTime, rangeStartMillis)
        val clippedEnd = minOf(activityEnd, rangeEndMillis)
        if (clippedEnd <= clippedStart) return@flatMap emptyList()

        buildList {
            var sliceStart = clippedStart
            while (sliceStart < clippedEnd) {
                val nextDayStart = Calendar.getInstance(timeZone).apply {
                    timeInMillis = sliceStart
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis
                val sliceEnd = minOf(clippedEnd, nextDayStart)
                add(
                    activity.copy(
                        startTime = sliceStart,
                        endTime = sliceEnd,
                        durationMillis = sliceEnd - sliceStart
                    )
                )
                sliceStart = sliceEnd
            }
        }
    }.sortedBy { it.startTime }
}
