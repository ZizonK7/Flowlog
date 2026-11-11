package com.example.flowlog.data.external

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class UsageStatsAggregationTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    private fun at(dayOfMonth: Int, h: Int, m: Int = 0): Long = Calendar.getInstance(seoul).apply {
        clear()
        set(2026, Calendar.NOVEMBER, dayOfMonth, h, m, 0)
    }.timeInMillis

    private val day8 = at(8, 0)
    private val day9 = at(9, 0)
    private val day10 = at(10, 0)
    private val windowStart = at(2, 0)

    private fun fg(pkg: String, t: Long, cls: String? = "Main") = UsageEvent(pkg, UsageEventType.FOREGROUND, t, cls)
    private fun bg(pkg: String, t: Long, cls: String? = "Main") = UsageEvent(pkg, UsageEventType.BACKGROUND, t, cls)
    private fun screenOn(t: Long) = UsageEvent("android", UsageEventType.SCREEN_INTERACTIVE, t)
    private fun screenOff(t: Long) = UsageEvent("android", UsageEventType.SCREEN_NON_INTERACTIVE, t)

    private fun daily(events: List<UsageEvent>, windowEnd: Long = at(9, 20)) =
        UsageStatsAggregation.appForegroundDailyRows(events, windowStart, windowEnd, seoul)

    @Test
    fun intervalOverMidnightIsCutIntoTwoDayRows() {
        val rows = daily(listOf(fg("a.app", at(8, 23, 30)), bg("a.app", at(9, 0, 45))))
        assertEquals(2, rows.size)
        val (first, second) = rows
        assertEquals(day8, first.startTime)
        assertEquals(day9, first.endTime)
        assertEquals((30 * minute).toDouble(), first.value, 0.0)
        assertEquals(day9, second.startTime)
        assertEquals(day10, second.endTime)
        assertEquals((45 * minute).toDouble(), second.value, 0.0)
        assertEquals("usage_stats:app_foreground_daily:$day8:$day9:a.app", first.sampleId)
        assertEquals(ExternalSampleProvider.USAGE_STATS, first.provider)
        assertEquals(ExternalSampleKind.APP_FOREGROUND_DAILY, first.kind)
        assertEquals(ExternalSampleUnit.MILLIS, first.unit)
        assertEquals("a.app", first.sourcePackage)
        assertEquals("""{"sessions":1}""", first.payloadJson)
    }

    @Test
    fun unpairedBackgroundIsIgnored() {
        val rows = daily(listOf(bg("a.app", at(9, 1)), fg("a.app", at(9, 2)), bg("a.app", at(9, 2, 10))))
        assertEquals(1, rows.size)
        assertEquals((10 * minute).toDouble(), rows[0].value, 0.0)
    }

    @Test
    fun intervalStillOpenAtTheWindowEndRunsToTheWindowEnd() {
        // at(day, hour): opened on the 9th at 15:00, window ends on the 9th at 20:00.
        val rows = daily(listOf(fg("a.app", at(9, 15))), windowEnd = at(9, 20))
        assertEquals(1, rows.size)
        assertEquals((5 * hour).toDouble(), rows[0].value, 0.0)
        // Today's row keeps its day bounds (and id) while its value grows.
        assertEquals(day9, rows[0].startTime)
        assertEquals(day10, rows[0].endTime)
    }

    @Test
    fun severalIntervalsOfOnePackageInADayAreSummedAndCounted() {
        val rows = daily(
            listOf(
                fg("a.app", at(9, 1)), bg("a.app", at(9, 1, 10)),
                fg("b.app", at(9, 2)), bg("b.app", at(9, 2, 5)),
                fg("a.app", at(9, 3)), bg("a.app", at(9, 3, 20))
            )
        )
        assertEquals(listOf("a.app", "b.app"), rows.map { it.sourcePackage })
        assertEquals((30 * minute).toDouble(), rows[0].value, 0.0)
        assertEquals("""{"sessions":2}""", rows[0].payloadJson)
        assertEquals((5 * minute).toDouble(), rows[1].value, 0.0)
    }

    @Test
    fun overlappingActivitiesOfOnePackageAreNotCountedTwice() {
        // Activity B resumes before A pauses (same app): 1:00–1:30 once, not 1:00–1:20 + 1:10–1:30.
        val rows = daily(
            listOf(
                fg("a.app", at(9, 1), cls = "A"),
                fg("a.app", at(9, 1, 10), cls = "B"),
                bg("a.app", at(9, 1, 20), cls = "A"),
                bg("a.app", at(9, 1, 30), cls = "B")
            )
        )
        assertEquals(1, rows.size)
        assertEquals((30 * minute).toDouble(), rows[0].value, 0.0)
        assertEquals("""{"sessions":1}""", rows[0].payloadJson)
    }

    @Test
    fun aSecondStartWithoutAnEndReplacesTheFirstAndShutdownClosesAll() {
        val rows = daily(
            listOf(
                fg("a.app", at(9, 1)),
                fg("a.app", at(9, 2)),
                bg("a.app", at(9, 2, 10)),
                fg("b.app", at(9, 3)),
                UsageEvent("android", UsageEventType.DEVICE_SHUTDOWN, at(9, 3, 15))
            )
        )
        assertEquals((10 * minute).toDouble(), rows.first { it.sourcePackage == "a.app" }.value, 0.0)
        assertEquals((15 * minute).toDouble(), rows.first { it.sourcePackage == "b.app" }.value, 0.0)
    }

    @Test
    fun eventsOutOfOrderAreSortedFirst() {
        val rows = daily(listOf(bg("a.app", at(9, 1, 10)), fg("a.app", at(9, 1))))
        assertEquals((10 * minute).toDouble(), rows.single().value, 0.0)
    }

    @Test
    fun splitAtMidnightsGivesDayBoundsForEachPiece() {
        val pieces = UsageStatsAggregation.splitAtMidnights(at(8, 22), at(10, 1), seoul)
        assertEquals(3, pieces.size)
        assertEquals(DayPiece(day8, day9, at(8, 22), day9), pieces[0])
        assertEquals(DayPiece(day9, day10, day9, day10), pieces[1])
        assertEquals(DayPiece(day10, day10 + day, day10, at(10, 1)), pieces[2])
        assertTrue(UsageStatsAggregation.splitAtMidnights(5L, 5L, seoul).isEmpty())
    }

    @Test
    fun screenOnOffPairsBecomeRowsWithoutMidnightCut() {
        val rows = UsageStatsAggregation.screenInteractiveRows(
            listOf(
                screenOff(at(8, 22)),            // no on before: ignored
                screenOn(at(8, 23, 50)), screenOff(at(9, 0, 10)),
                screenOn(at(9, 7)), screenOff(at(9, 7, 3)),
                screenOn(at(9, 9))               // still on at the window end: no row yet
            )
        )
        assertEquals(2, rows.size)
        assertEquals(at(8, 23, 50), rows[0].startTime)
        assertEquals(at(9, 0, 10), rows[0].endTime)
        assertEquals((20 * minute).toDouble(), rows[0].value, 0.0)
        assertEquals("usage_stats:screen_interactive:${at(8, 23, 50)}:${at(9, 0, 10)}:-", rows[0].sampleId)
        assertEquals(ExternalSampleKind.SCREEN_INTERACTIVE, rows[0].kind)
        assertEquals(ExternalSampleUnit.MILLIS, rows[0].unit)
        assertNull(rows[0].sourcePackage)
        assertNull(rows[0].payloadJson)
        assertEquals((3 * minute).toDouble(), rows[1].value, 0.0)
    }

    @Test
    fun usageAccessModeRule() {
        assertTrue(UsageAccess.isAllowedMode(UsageAccess.MODE_ALLOWED, permissionGranted = false))
        assertTrue(UsageAccess.isAllowedMode(UsageAccess.MODE_DEFAULT, permissionGranted = true))
        assertEquals(false, UsageAccess.isAllowedMode(UsageAccess.MODE_DEFAULT, permissionGranted = false))
        assertEquals(false, UsageAccess.isAllowedMode(1, permissionGranted = true)) // MODE_IGNORED
    }
}
