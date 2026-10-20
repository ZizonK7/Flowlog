package com.example.flowlog.ui.viewmodel

import com.example.flowlog.data.model.ActivitySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ActivityImportParserTest {
    private val hour = TimeUnit.HOURS.toMillis(1)
    private val now = 1_800_000_000_000L

    private fun session(
        id: Long,
        start: Long,
        end: Long = start + hour,
        category: String = "STUDY",
        localId: String? = "legacy_activity_$id",
        duration: Long = end - start
    ) = ActivitySession(
        id = id,
        category = category,
        title = "t$id",
        startTime = start,
        endTime = end,
        durationMillis = duration,
        modifiedTime = start,
        remoteRevision = 3L,
        localActivityId = localId
    )

    private fun exported(vararg sessions: ActivitySession): String =
        ActivityExportFormatter.toJson(sessions.toList())

    private fun ready(result: ActivityImportResult) = result as ActivityImportResult.Ready

    @Test
    fun exportedFileRoundTripsKeepingIdentityAndResettingRevision() {
        val a = session(11, now - 5 * hour)
        val b = session(12, now - 3 * hour, category = "REST")

        val result = ready(ActivityImportParser.parse(exported(a, b), emptySet(), emptyList(), now))

        assertEquals(listOf(11L, 12L), result.toInsert.map { it.id })
        assertEquals(listOf("legacy_activity_11", "legacy_activity_12"), result.toInsert.map { it.localActivityId })
        assertTrue(result.toInsert.all { it.remoteRevision == 0L })
        assertEquals("t11", result.toInsert[0].title)
    }

    @Test
    fun duplicateByStoredIdentityIsSkippedEvenIfDeletedOrOtherAccount() {
        val a = session(11, now - 5 * hour)

        val result = ready(ActivityImportParser.parse(exported(a), setOf("legacy_activity_11"), emptyList(), now))

        assertEquals(0, result.summary.newCount)
        assertEquals(1, result.summary.duplicateCount)
    }

    @Test
    fun duplicateByCategoryStartAndEndIsSkipped() {
        val saved = session(99, now - 5 * hour, localId = "other")
        val sameRange = session(11, now - 5 * hour)
        val sameRangeOtherCategory = session(12, now - 5 * hour, category = "REST")

        val result = ready(
            ActivityImportParser.parse(exported(sameRange, sameRangeOtherCategory), emptySet(), listOf(saved), now)
        )

        assertEquals(listOf(12L), result.toInsert.map { it.id })
        assertEquals(1, result.summary.duplicateCount)
    }

    @Test
    fun repeatInsideTheFileCountsAsDuplicate() {
        val a = session(11, now - 5 * hour)
        val copyWithoutIdentity = session(0, now - 5 * hour, localId = null)

        val result = ready(ActivityImportParser.parse(exported(a, a, copyWithoutIdentity), emptySet(), emptyList(), now))

        assertEquals(1, result.summary.newCount)
        assertEquals(2, result.summary.duplicateCount)
    }

    @Test
    fun brokenItemsAreSkippedAndCounted() {
        val good = session(11, now - 5 * hour)
        val text = """
            [
              ${exported(good).trim().removePrefix("[").removeSuffix("]")},
              {"id": 20, "title": "no category", "startTime": 1, "endTime": 2, "durationMillis": 1},
              {"id": 21, "category": "STUDY", "title": "bad type", "startTime": "x", "endTime": 2, "durationMillis": 1},
              "not an object",
              {"id": 22, "category": "STUDY", "title": "end before start", "startTime": ${now - hour}, "endTime": ${now - 2 * hour}, "durationMillis": 0},
              {"id": 23, "category": " ", "title": "blank category", "startTime": ${now - 2 * hour}, "endTime": ${now - hour}, "durationMillis": 0}
            ]
        """.trimIndent()

        val result = ready(ActivityImportParser.parse(text, emptySet(), emptyList(), now))

        assertEquals(6, result.summary.total)
        assertEquals(1, result.summary.newCount)
        assertEquals(5, result.summary.invalidCount)
    }

    @Test
    fun emptyArrayIsReadyWithNothingToAdd() {
        val result = ready(ActivityImportParser.parse("[]", emptySet(), emptyList(), now))

        assertEquals(ActivityImportSummary(0, 0, 0, 0, null, null), result.summary)
        assertTrue(result.toInsert.isEmpty())
    }

    @Test
    fun nonJsonOrNonArrayIsRejected() {
        assertSame(ActivityImportResult.NotActivityJson, ActivityImportParser.parse("id,category\n1,STUDY", emptySet(), emptyList(), now))
        assertSame(ActivityImportResult.NotActivityJson, ActivityImportParser.parse("{\"id\": 1}", emptySet(), emptyList(), now))
        assertSame(ActivityImportResult.NotActivityJson, ActivityImportParser.parse("", emptySet(), emptyList(), now))
    }

    @Test
    fun futureRecordIsInvalid() {
        val future = session(11, now - hour / 2, end = now + hour / 2)

        val result = ready(ActivityImportParser.parse(exported(future), emptySet(), emptyList(), now))

        assertEquals(0, result.summary.newCount)
        assertEquals(1, result.summary.invalidCount)
    }

    @Test
    fun longerThan24HoursIsInvalidButExactly24HoursIsKept() {
        val tooLong = session(11, now - 30 * hour, end = now - 30 * hour + 24 * hour + 1)
        val day = session(12, now - 60 * hour, end = now - 36 * hour)

        val result = ready(ActivityImportParser.parse(exported(tooLong, day), emptySet(), emptyList(), now))

        assertEquals(listOf(12L), result.toInsert.map { it.id })
        assertEquals(1, result.summary.invalidCount)
    }

    @Test
    fun zeroLengthCheckRecordIsValidOnlyWithZeroDuration() {
        val check = session(11, now - hour, end = now - hour, duration = 0L)
        val broken = session(12, now - 2 * hour, end = now - 2 * hour, duration = 5L)

        assertTrue(ActivityImportParser.isValid(check, now))
        assertFalse(ActivityImportParser.isValid(broken, now))
        assertFalse(ActivityImportParser.isValid(session(13, now - 2 * hour, duration = 2 * hour), now))
    }

    @Test
    fun summaryCountsAndPeriodCoverNewRecords() {
        val first = session(11, now - 50 * hour)
        val last = session(12, now - 2 * hour)
        val dup = session(13, now - 10 * hour)
        val broken = session(14, now - 1 * hour, end = now + hour)

        val result = ready(
            ActivityImportParser.parse(exported(last, dup, first, broken), setOf("legacy_activity_13"), emptyList(), now)
        )

        assertEquals(ActivityImportSummary(4, 2, 1, 1, now - 50 * hour, now - hour), result.summary)
        assertEquals(listOf(11L, 12L), result.toInsert.map { it.id })
    }

    @Test
    fun identityFollowsTheRoomRule() {
        assertEquals("uuid-1", ActivityImportParser.recordIdentity(session(5, now - hour, localId = "uuid-1")))
        assertEquals("legacy_activity_5", ActivityImportParser.recordIdentity(session(5, now - hour, localId = null)))
        assertNull(ActivityImportParser.recordIdentity(session(0, now - hour, localId = null)))
    }

    @Test
    fun byteOrderMarkIsTolerated() {
        val text = "﻿" + exported(session(11, now - 5 * hour))
        assertEquals(1, ready(ActivityImportParser.parse(text, emptySet(), emptyList(), now)).summary.newCount)
    }

    @Test
    fun sameLegacyIdUnderAnotherIdentityIsDuplicate() {
        // A local row already owns legacy id 11 (its remote document id), with a different activityId.
        val a = session(11, now - 5 * hour, localId = "uuid-from-file")

        val result = ready(
            ActivityImportParser.parse(exported(a), setOf("uuid-local", ActivityImportParser.legacyKey(11)), emptyList(), now)
        )

        assertEquals(1, result.summary.duplicateCount)
    }
}
