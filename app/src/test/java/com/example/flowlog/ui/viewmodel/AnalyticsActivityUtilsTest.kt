package com.example.flowlog.ui.viewmodel

import com.example.flowlog.data.model.ActivitySession
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsActivityUtilsTest {
    private val timeZone = TimeZone.getTimeZone("Asia/Seoul")

    @Test
    fun activityCrossingMidnightIsSplitBetweenBothDates() {
        val activity = activity(
            start = time(2026, Calendar.JUNE, 19, 23, 0),
            end = time(2026, Calendar.JUNE, 20, 1, 0)
        )

        val result = splitActivitiesAcrossDays(
            activities = listOf(activity),
            rangeStartMillis = time(2026, Calendar.JUNE, 19, 0, 0),
            rangeEndMillis = time(2026, Calendar.JUNE, 21, 0, 0),
            timeZone = timeZone
        )

        assertEquals(2, result.size)
        assertEquals(time(2026, Calendar.JUNE, 19, 23, 0), result[0].startTime)
        assertEquals(time(2026, Calendar.JUNE, 20, 0, 0), result[0].endTime)
        assertEquals(60 * 60 * 1000L, result[0].durationMillis)
        assertEquals(time(2026, Calendar.JUNE, 20, 0, 0), result[1].startTime)
        assertEquals(time(2026, Calendar.JUNE, 20, 1, 0), result[1].endTime)
        assertEquals(60 * 60 * 1000L, result[1].durationMillis)
    }

    @Test
    fun multiDayActivityProducesOneSlicePerDate() {
        val activity = activity(
            start = time(2026, Calendar.JUNE, 18, 22, 0),
            end = time(2026, Calendar.JUNE, 20, 2, 0)
        )

        val result = splitActivitiesAcrossDays(
            activities = listOf(activity),
            rangeStartMillis = time(2026, Calendar.JUNE, 18, 0, 0),
            rangeEndMillis = time(2026, Calendar.JUNE, 21, 0, 0),
            timeZone = timeZone
        )

        assertEquals(3, result.size)
        assertEquals(listOf(2L, 24L, 2L), result.map { it.durationMillis / 3_600_000L })
    }

    @Test
    fun slicesAreClippedToAnalyticsRangeWithoutChangingOriginal() {
        val originalStart = time(2026, Calendar.JUNE, 18, 23, 0)
        val activity = activity(
            start = originalStart,
            end = time(2026, Calendar.JUNE, 20, 1, 0)
        )

        val result = splitActivitiesAcrossDays(
            activities = listOf(activity),
            rangeStartMillis = time(2026, Calendar.JUNE, 19, 0, 0),
            rangeEndMillis = time(2026, Calendar.JUNE, 20, 0, 0),
            timeZone = timeZone
        )

        assertEquals(1, result.size)
        assertEquals(24 * 60 * 60 * 1000L, result.single().durationMillis)
        assertEquals(originalStart, activity.startTime)
        assertTrue(activity.durationMillis > result.single().durationMillis)
    }

    private fun activity(start: Long, end: Long) = ActivitySession(
        category = "STUDY",
        title = "Study",
        startTime = start,
        endTime = end,
        durationMillis = end - start
    )

    private fun time(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
    }
}
