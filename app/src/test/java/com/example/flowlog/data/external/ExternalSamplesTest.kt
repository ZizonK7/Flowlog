package com.example.flowlog.data.external

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExternalSamplesTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val hour = 60L * 60 * 1000
    private val day = 24 * hour

    // 2026-11-09 15:30 in Asia/Seoul (UTC+9, no DST)
    private val now = Calendar.getInstance(seoul).apply {
        clear()
        set(2026, Calendar.NOVEMBER, 9, 15, 30, 0)
    }.timeInMillis
    private val todayMidnight = Calendar.getInstance(seoul).apply {
        clear()
        set(2026, Calendar.NOVEMBER, 9, 0, 0, 0)
    }.timeInMillis

    @Test
    fun sampleIdIsStableTextWithADashForNoSource() {
        assertEquals(
            "health_connect:sleep_session:1000:2000:com.example.health",
            ExternalSamples.sampleId("health_connect", "sleep_session", 1000L, 2000L, "com.example.health")
        )
        assertEquals("health_connect:sleep_session:1000:2000:-", ExternalSamples.sampleId("health_connect", "sleep_session", 1000L, 2000L, null))
        assertEquals("health_connect:sleep_session:1000:2000:-", ExternalSamples.sampleId("health_connect", "sleep_session", 1000L, 2000L, "  "))
        // Same input, same id (a re-read lands on the same row); a different end time is another sample.
        assertEquals(
            ExternalSamples.sampleId("p", "k", 1L, 2L, "a"),
            ExternalSamples.sampleId("p", "k", 1L, 2L, "a")
        )
        assertFalse(ExternalSamples.sampleId("p", "k", 1L, 2L, "a") == ExternalSamples.sampleId("p", "k", 1L, 3L, "a"))
    }

    @Test
    fun sampleIdUsesTheRecordIdWhenThereIsOne() {
        assertEquals("health_connect:sleep_session:abc-1", ExternalSamples.sampleId("health_connect", "sleep_session", 1L, 2L, "a", recordId = "abc-1"))
        // Times edited in the source app: same record, same id.
        assertEquals(
            ExternalSamples.sampleId("health_connect", "sleep_session", 1L, 2L, "a", recordId = "abc-1"),
            ExternalSamples.sampleId("health_connect", "sleep_session", 5L, 9L, "a", recordId = "abc-1")
        )
        // Blank or not a valid Firestore document id: back to the time-based form.
        assertEquals("health_connect:sleep_session:1:2:a", ExternalSamples.sampleId("health_connect", "sleep_session", 1L, 2L, "a", recordId = " "))
        assertEquals("health_connect:sleep_session:1:2:a", ExternalSamples.sampleId("health_connect", "sleep_session", 1L, 2L, "a", recordId = "x/y"))
    }

    @Test
    fun sleepSessionBecomesMinutesWithSortedStagesInPayload() {
        val start = todayMidnight - 2 * hour      // 22:00 the day before
        val end = todayMidnight + 6 * hour + 30 * 60_000L  // 06:30
        val row = ExternalSamples.sleepSessionRow(
            SleepSessionInput(
                recordId = "r1",
                startMillis = start,
                endMillis = end,
                sourcePackage = "com.example.health",
                stages = listOf(
                    SleepStageInput(type = 5, startMillis = start + hour, endMillis = start + 2 * hour),
                    SleepStageInput(type = 4, startMillis = start, endMillis = start + hour)
                )
            )
        )
        assertEquals("health_connect:sleep_session:r1", row.sampleId)
        assertEquals(ExternalSampleProvider.HEALTH_CONNECT, row.provider)
        assertEquals(ExternalSampleKind.SLEEP_SESSION, row.kind)
        assertEquals(510.0, row.value, 0.0001)
        assertEquals(ExternalSampleUnit.MINUTES, row.unit)
        assertEquals("com.example.health", row.sourcePackage)
        assertEquals(
            """{"recordId":"r1","stages":[{"type":4,"start":$start,"end":${start + hour}},{"type":5,"start":${start + hour},"end":${start + 2 * hour}}]}""",
            row.payloadJson
        )
    }

    @Test
    fun sleepSessionWithoutStagesOrIdKeepsAnEmptyList() {
        val row = ExternalSamples.sleepSessionRow(
            SleepSessionInput(recordId = null, startMillis = 0L, endMillis = 60_000L, sourcePackage = "", stages = emptyList())
        )
        assertNull(row.sourcePackage)
        assertEquals(1.0, row.value, 0.0001)
        assertEquals("""{"stages":[]}""", row.payloadJson)
        assertTrue(row.sampleId.endsWith(":-"))
    }

    @Test
    fun exerciseSessionKeepsTypeAndTitleOnly() {
        val row = ExternalSamples.exerciseSessionRow(
            ExerciseSessionInput(
                recordId = "e1",
                startMillis = 1_000L,
                endMillis = 1_000L + 45 * 60_000L,
                sourcePackage = "com.example.health",
                exerciseType = 56,
                title = "Evening \"run\""
            )
        )
        assertEquals("health_connect:exercise_session:e1", row.sampleId)
        assertEquals(ExternalSampleKind.EXERCISE_SESSION, row.kind)
        assertEquals(45.0, row.value, 0.0001)
        assertEquals(ExternalSampleUnit.MINUTES, row.unit)
        assertEquals("""{"recordId":"e1","exerciseType":56,"title":"Evening \"run\""}""", row.payloadJson)
        val untitled = ExternalSamples.exerciseSessionRow(
            ExerciseSessionInput(recordId = null, startMillis = 0L, endMillis = 0L, sourcePackage = null, exerciseType = 0, title = " ")
        )
        assertEquals("""{"exerciseType":0}""", untitled.payloadJson)
        assertEquals("health_connect:exercise_session:0:0:-", untitled.sampleId)
        assertEquals(0.0, untitled.value, 0.0)
    }

    @Test
    fun negativeDurationIsZeroMinutes() {
        assertEquals(0.0, ExternalSamples.minutesBetween(2_000L, 1_000L), 0.0)
        assertEquals(1.5, ExternalSamples.minutesBetween(0L, 90_000L), 0.0001)
    }

    @Test
    fun stepsDayIsOneRowPerDayWhoseIdIgnoresWhichAppsWrote() {
        val one = ExternalSamples.stepsDayRow(
            StepsDayInput(todayMidnight, todayMidnight + day, count = 4200L, sourcePackages = listOf("b.app"))
        )
        val later = ExternalSamples.stepsDayRow(
            StepsDayInput(todayMidnight, todayMidnight + day, count = 8100L, sourcePackages = listOf("b.app", "a.app", "b.app", ""))
        )
        assertEquals("health_connect:steps_daily:$todayMidnight:${todayMidnight + day}:aggregate", one.sampleId)
        assertEquals(one.sampleId, later.sampleId)
        assertEquals(4200.0, one.value, 0.0)
        assertEquals(8100.0, later.value, 0.0)
        assertEquals(ExternalSampleUnit.COUNT, one.unit)
        assertEquals("a.app,b.app", later.sourcePackage)
        assertEquals("""{"sources":["a.app","b.app"]}""", later.payloadJson)
        val noSource = ExternalSamples.stepsDayRow(StepsDayInput(0L, day, count = -3L, sourcePackages = emptyList()))
        assertNull(noSource.sourcePackage)
        assertEquals(0.0, noSource.value, 0.0)
    }

    @Test
    fun fetchIsDueAfterSixHoursOrWhenNeverTried() {
        assertTrue(ExternalSamples.isFetchDue(null, now))
        assertTrue(ExternalSamples.isFetchDue(0L, now))
        assertFalse(ExternalSamples.isFetchDue(now - 6 * hour + 1, now))
        assertTrue(ExternalSamples.isFetchDue(now - 6 * hour, now))
        assertTrue(ExternalSamples.isFetchDue(now - 7 * hour, now))
        // Clock moved back: fetch rather than wait for a time that already passed.
        assertTrue(ExternalSamples.isFetchDue(now + hour, now))
    }

    @Test
    fun windowIsThirtyDaysFirstThenSevenDaysFromLocalMidnight() {
        val first = ExternalSamples.fetchWindow(lastSuccessAt = null, now = now, timeZone = seoul)
        assertTrue(first.firstFetch)
        assertEquals(todayMidnight - 30 * day, first.startMillis)
        assertEquals(now, first.endMillis)

        val normal = ExternalSamples.fetchWindow(lastSuccessAt = now - 6 * hour, now = now, timeZone = seoul)
        assertFalse(normal.firstFetch)
        assertEquals(todayMidnight - 7 * day, normal.startMillis)
        assertEquals(now, normal.endMillis)
    }

    @Test
    fun windowReachesBackPastALongGapButNotBeyondThirtyDays() {
        val tenDays = ExternalSamples.fetchWindow(lastSuccessAt = now - 10 * day, now = now, timeZone = seoul)
        assertEquals(todayMidnight - 11 * day, tenDays.startMillis)
        val sixtyDays = ExternalSamples.fetchWindow(lastSuccessAt = now - 60 * day, now = now, timeZone = seoul)
        assertEquals(todayMidnight - 30 * day, sixtyDays.startMillis)
        // A last success "in the future" (clock change) counts as never.
        assertTrue(ExternalSamples.fetchWindow(lastSuccessAt = now + day, now = now, timeZone = seoul).firstFetch)
    }

    @Test
    fun upsertInsertsNewUpdatesChangedAndLeavesTheSame() {
        val row = ExternalSamples.stepsDayRow(StepsDayInput(todayMidnight, todayMidnight + day, 100L, listOf("a.app")))
        assertEquals(UpsertAction.INSERT, ExternalSamples.upsertAction(null, row))
        assertEquals(UpsertAction.UNCHANGED, ExternalSamples.upsertAction(row, row.copy()))
        assertEquals(UpsertAction.UPDATE, ExternalSamples.upsertAction(row, row.copy(value = 150.0)))
        assertEquals(UpsertAction.UPDATE, ExternalSamples.upsertAction(row, row.copy(payloadJson = """{"sources":[]}""")))
        assertEquals(UpsertAction.UPDATE, ExternalSamples.upsertAction(row, row.copy(), sameOwner = false))
    }

    @Test
    fun healthConnectStateFollowsAvailabilitySwitchAndPermissions() {
        fun state(a: HealthConnectAvailability, enabled: Boolean, granted: Int) =
            ExternalSamples.healthConnectState(a, enabled, granted, requiredCount = 3)
        assertEquals(HealthConnectState.NOT_SUPPORTED, state(HealthConnectAvailability.NOT_SUPPORTED, true, 3))
        assertEquals(HealthConnectState.NOT_INSTALLED, state(HealthConnectAvailability.NOT_INSTALLED, true, 0))
        assertEquals(HealthConnectState.UPDATE_REQUIRED, state(HealthConnectAvailability.UPDATE_REQUIRED, false, 0))
        assertEquals(HealthConnectState.OFF, state(HealthConnectAvailability.AVAILABLE, false, 3))
        assertEquals(HealthConnectState.NO_PERMISSION, state(HealthConnectAvailability.AVAILABLE, true, 0))
        assertEquals(HealthConnectState.CONNECTED_PARTIAL, state(HealthConnectAvailability.AVAILABLE, true, 2))
        assertEquals(HealthConnectState.CONNECTED, state(HealthConnectAvailability.AVAILABLE, true, 3))
    }

    @Test
    fun usageStatsStateFollowsSwitchThenAccess() {
        assertEquals(UsageStatsState.OFF, ExternalSamples.usageStatsState(enabled = false, granted = true))
        assertEquals(UsageStatsState.NO_PERMISSION, ExternalSamples.usageStatsState(enabled = true, granted = false))
        assertEquals(UsageStatsState.CONNECTED, ExternalSamples.usageStatsState(enabled = true, granted = true))
    }
}
