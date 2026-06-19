package com.example.flowlog.ui.screen

import com.example.flowlog.data.model.ActivitySession
import java.util.Calendar

/**
 * Builds the activity list used only by today's timetable.
 *
 * Completed activities that started before today but ended during today are
 * added as display-only copies whose start is clipped to midnight. The stored
 * activity remains unchanged.
 */
internal fun activitiesForTodayTimetable(
    todayActivities: List<ActivitySession>,
    allActivities: List<ActivitySession>,
    nowMillis: Long = System.currentTimeMillis()
): List<ActivitySession> {
    if (allActivities.isEmpty()) return todayActivities

    val dayStart = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val carryOverActivities = allActivities
        .asSequence()
        .filter { activity ->
            activity.startTime < dayStart &&
                activity.endTime > dayStart &&
                activity.endTime <= nowMillis
        }
        .map { activity ->
            activity.copy(
                startTime = dayStart,
                durationMillis = activity.endTime - dayStart
            )
        }
        .toList()

    if (carryOverActivities.isEmpty()) return todayActivities
    return (carryOverActivities + todayActivities).sortedBy { it.startTime }
}
