package com.example.flowlog.ui.screen

import com.example.flowlog.data.model.ActivitySession
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableActivityUtilsTest {

    @Test
    fun completedCarryOverActivityIsClippedToMidnight() {
        val now = time(2026, Calendar.JUNE, 20, 2, 0)
        val midnight = time(2026, Calendar.JUNE, 20, 0, 0)
        val activity = activity(
            start = time(2026, Calendar.JUNE, 19, 23, 0),
            end = time(2026, Calendar.JUNE, 20, 1, 0)
        )

        val result = activitiesForTodayTimetable(
            todayActivities = emptyList(),
            allActivities = listOf(activity),
            nowMillis = now
        )

        assertEquals(1, result.size)
        assertEquals(midnight, result.single().startTime)
        assertEquals(time(2026, Calendar.JUNE, 20, 1, 0), result.single().endTime)
        assertEquals(60 * 60 * 1000L, result.single().durationMillis)
        assertEquals(time(2026, Calendar.JUNE, 19, 23, 0), activity.startTime)
    }

    @Test
    fun unfinishedCarryOverActivityIsNotAdded() {
        val now = time(2026, Calendar.JUNE, 20, 2, 0)
        val activity = activity(
            start = time(2026, Calendar.JUNE, 19, 23, 0),
            end = time(2026, Calendar.JUNE, 20, 3, 0)
        )

        val result = activitiesForTodayTimetable(
            todayActivities = emptyList(),
            allActivities = listOf(activity),
            nowMillis = now
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun activityThatEndedBeforeMidnightIsNotAdded() {
        val now = time(2026, Calendar.JUNE, 20, 2, 0)
        val activity = activity(
            start = time(2026, Calendar.JUNE, 19, 22, 0),
            end = time(2026, Calendar.JUNE, 19, 23, 0)
        )

        val result = activitiesForTodayTimetable(
            todayActivities = emptyList(),
            allActivities = listOf(activity),
            nowMillis = now
        )

        assertTrue(result.isEmpty())
    }

    private fun activity(start: Long, end: Long) = ActivitySession(
        category = "STUDY",
        title = "Study",
        startTime = start,
        endTime = end,
        durationMillis = end - start
    )

    private fun time(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
    }
}
