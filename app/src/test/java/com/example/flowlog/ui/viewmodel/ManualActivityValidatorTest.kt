package com.example.flowlog.ui.viewmodel

import com.example.flowlog.data.model.ActivitySession
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualActivityValidatorTest {
    private val timeZone = TimeZone.getTimeZone("Asia/Seoul")
    private val now = time(2026, Calendar.OCTOBER, 20, 12, 0)

    @Test
    fun pastRangeWithoutOverlapIsValid() {
        val result = ManualActivityValidator.validate(
            startMillis = time(2026, Calendar.OCTOBER, 20, 9, 0),
            endMillis = time(2026, Calendar.OCTOBER, 20, 10, 0),
            nowMillis = now,
            existing = emptyList()
        )

        assertNull(result.error)
        assertTrue(result.canSave)
        assertFalse(result.hasOverlapWarning)
    }

    @Test
    fun endNotAfterStartIsRejected() {
        val start = time(2026, Calendar.OCTOBER, 20, 9, 0)
        val equal = ManualActivityValidator.validate(start, start, now, emptyList())
        val reversed = ManualActivityValidator.validate(start, start - 60_000L, now, emptyList())

        assertEquals(ManualActivityError.END_NOT_AFTER_START, equal.error)
        assertEquals(ManualActivityError.END_NOT_AFTER_START, reversed.error)
        assertFalse(equal.canSave)
    }

    @Test
    fun exactlyTwentyFourHoursIsAllowedButLongerIsRejected() {
        val start = time(2026, Calendar.OCTOBER, 18, 9, 0)
        val dayLater = start + TimeUnit.HOURS.toMillis(24)

        assertNull(ManualActivityValidator.validate(start, dayLater, now, emptyList()).error)
        assertEquals(
            ManualActivityError.TOO_LONG,
            ManualActivityValidator.validate(start, dayLater + 60_000L, now, emptyList()).error
        )
    }

    @Test
    fun rangeEndingInTheFutureIsRejected() {
        val result = ManualActivityValidator.validate(
            startMillis = time(2026, Calendar.OCTOBER, 20, 11, 30),
            endMillis = time(2026, Calendar.OCTOBER, 20, 12, 1),
            nowMillis = now,
            existing = emptyList()
        )

        assertEquals(ManualActivityError.IN_FUTURE, result.error)
    }

    @Test
    fun rangeEndingExactlyNowIsAllowed() {
        val result = ManualActivityValidator.validate(
            startMillis = time(2026, Calendar.OCTOBER, 20, 11, 30),
            endMillis = now,
            nowMillis = now,
            existing = emptyList()
        )

        assertNull(result.error)
    }

    @Test
    fun overlapIsAWarningAndDoesNotBlockSaving() {
        val study = session(1L, "STUDY", time(2026, Calendar.OCTOBER, 20, 9, 30), time(2026, Calendar.OCTOBER, 20, 10, 30))
        val meal = session(2L, "MEAL", time(2026, Calendar.OCTOBER, 20, 7, 0), time(2026, Calendar.OCTOBER, 20, 7, 30))

        val result = ManualActivityValidator.validate(
            startMillis = time(2026, Calendar.OCTOBER, 20, 9, 0),
            endMillis = time(2026, Calendar.OCTOBER, 20, 10, 0),
            nowMillis = now,
            existing = listOf(study, meal)
        )

        assertTrue(result.canSave)
        assertTrue(result.hasOverlapWarning)
        assertEquals(listOf(1L), result.overlaps.map { it.id })
    }

    @Test
    fun touchingEdgesAndZeroLengthRecordsAreNotOverlaps() {
        val before = session(1L, "STUDY", time(2026, Calendar.OCTOBER, 20, 8, 0), time(2026, Calendar.OCTOBER, 20, 9, 0))
        val after = session(2L, "WORK", time(2026, Calendar.OCTOBER, 20, 10, 0), time(2026, Calendar.OCTOBER, 20, 11, 0))
        val check = session(3L, "TODO", time(2026, Calendar.OCTOBER, 20, 9, 30), time(2026, Calendar.OCTOBER, 20, 9, 30))

        val overlaps = ManualActivityValidator.findOverlaps(
            startMillis = time(2026, Calendar.OCTOBER, 20, 9, 0),
            endMillis = time(2026, Calendar.OCTOBER, 20, 10, 0),
            existing = listOf(before, after, check)
        )

        assertTrue(overlaps.isEmpty())
    }

    @Test
    fun overlapsAreSortedByStartTime() {
        val late = session(1L, "WORK", time(2026, Calendar.OCTOBER, 20, 9, 40), time(2026, Calendar.OCTOBER, 20, 9, 50))
        val early = session(2L, "STUDY", time(2026, Calendar.OCTOBER, 20, 8, 30), time(2026, Calendar.OCTOBER, 20, 9, 10))

        val overlaps = ManualActivityValidator.findOverlaps(
            startMillis = time(2026, Calendar.OCTOBER, 20, 9, 0),
            endMillis = time(2026, Calendar.OCTOBER, 20, 10, 0),
            existing = listOf(late, early)
        )

        assertEquals(listOf(2L, 1L), overlaps.map { it.id })
    }

    @Test
    fun resolveTimesKeepsSameDayWhenEndIsLater() {
        val day = time(2026, Calendar.OCTOBER, 19, 15, 45)

        val (start, end) = ManualActivityValidator.resolveTimes(day, 9 * 60 + 30, 11 * 60, timeZone)

        assertEquals(time(2026, Calendar.OCTOBER, 19, 9, 30), start)
        assertEquals(time(2026, Calendar.OCTOBER, 19, 11, 0), end)
    }

    @Test
    fun resolveTimesMovesEarlierEndToNextDay() {
        val day = time(2026, Calendar.OCTOBER, 19, 0, 0)

        val (start, end) = ManualActivityValidator.resolveTimes(day, 23 * 60 + 30, 7 * 60, timeZone)

        assertEquals(time(2026, Calendar.OCTOBER, 19, 23, 30), start)
        assertEquals(time(2026, Calendar.OCTOBER, 20, 7, 0), end)
        assertNull(ManualActivityValidator.validate(start, end, now, emptyList()).error)
    }

    @Test
    fun resolveTimesWithEqualMinutesGivesZeroLengthThatFailsValidation() {
        val day = time(2026, Calendar.OCTOBER, 19, 0, 0)

        val (start, end) = ManualActivityValidator.resolveTimes(day, 600, 600, timeZone)

        assertEquals(start, end)
        assertEquals(
            ManualActivityError.END_NOT_AFTER_START,
            ManualActivityValidator.validate(start, end, now, emptyList()).error
        )
    }

    @Test
    fun sameLocalDayComparesCalendarDatesInTheGivenZone() {
        val morning = time(2026, Calendar.OCTOBER, 20, 0, 5)
        val evening = time(2026, Calendar.OCTOBER, 20, 23, 55)
        val yesterday = time(2026, Calendar.OCTOBER, 19, 23, 55)

        assertTrue(ManualActivityValidator.isSameLocalDay(morning, evening, timeZone))
        assertFalse(ManualActivityValidator.isSameLocalDay(yesterday, morning, timeZone))
        // 00:05 in Seoul is still the previous day in UTC.
        assertTrue(ManualActivityValidator.isSameLocalDay(yesterday, morning, TimeZone.getTimeZone("UTC")))
    }

    private fun session(id: Long, category: String, start: Long, end: Long) = ActivitySession(
        id = id,
        category = category,
        title = category,
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
