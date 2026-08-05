package com.example.flowlog.ui.viewmodel

import com.example.flowlog.data.model.ActivitySession
import java.util.Calendar
import java.util.TimeZone

/**
 * Builds today/yesterday category totals from completed activity records.
 * Pure calculation only; does not read or mutate ViewModel state.
 */
internal fun buildAnalytics(activities: List<ActivitySession>): AnalyticsState {
    val now = System.currentTimeMillis()
    val todayStart = startOfDay(Calendar.getInstance().apply {
        timeInMillis = now
    }).timeInMillis
    val tomorrowStart = startOfDay(Calendar.getInstance().apply {
        timeInMillis = todayStart
        add(Calendar.DAY_OF_YEAR, 1)
    }).timeInMillis
    val yesterdayStart = startOfDay(Calendar.getInstance().apply {
        timeInMillis = todayStart
        add(Calendar.DAY_OF_YEAR, -1)
    }).timeInMillis
    val analyticsActivities = splitActivitiesAcrossDays(
        activities = activities,
        rangeStartMillis = yesterdayStart,
        rangeEndMillis = tomorrowStart
    )
    val todayActivities = analyticsActivities.filter { it.startTime >= todayStart && it.startTime < tomorrowStart }
    val yesterdayActivities = analyticsActivities.filter { it.startTime >= yesterdayStart && it.startTime < todayStart }
    return AnalyticsState(
        todayCategoryStats = buildCategoryStats(todayActivities),
        yesterdayCategoryStats = buildCategoryStats(yesterdayActivities)
    )
}

internal fun buildCategoryStats(activities: List<ActivitySession>): List<CategoryStat> {
    return activities.groupBy { it.category }
        .map { (category, sessions) ->
            val total = sessions.sumOf { it.durationMillis }
            CategoryStat(
                category = category,
                totalMillis = total,
                count = sessions.size,
                averageMillis = total
            )
        }
        .sortedByDescending { it.totalMillis }
}

internal fun startOfDay(calendar: Calendar): Calendar {
    return calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

internal fun koreaTimeMillis(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int
): Long {
    return Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

internal fun isTimedCategory(category: String): Boolean {
    return category != "SNACK" && category != "TOOTHBRUSH"
}

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
