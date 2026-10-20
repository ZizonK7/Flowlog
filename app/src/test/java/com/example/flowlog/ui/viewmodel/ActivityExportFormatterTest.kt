package com.example.flowlog.ui.viewmodel

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.ExerciseSetRecord
import java.util.Calendar
import java.util.TimeZone
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityExportFormatterTest {
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val utc = TimeZone.getTimeZone("UTC")
    private val header = "id,category,title,start,end,durationMinutes,note,tags,linkedTodoId,sourceType,exerciseSets"

    @Test
    fun emptyListGivesHeaderOnlyCsvAndEmptyJsonArray() {
        assertEquals("$header\r\n", ActivityExportFormatter.toCsv(emptyList(), seoul))
        assertEquals("[]", ActivityExportFormatter.toJson(emptyList()).trim())
    }

    @Test
    fun plainRowUsesLocalIsoTimesAndMinutes() {
        val start = seoulTime(2026, Calendar.OCTOBER, 20, 9, 30)
        val session = session(id = 7L, start = start, end = start + 45 * 60_000L)

        val csv = ActivityExportFormatter.toCsv(listOf(session), seoul)

        assertEquals(
            "$header\r\n" +
                "7,STUDY,Reading notes,2026-10-20T09:30:00+09:00,2026-10-20T10:15:00+09:00,45,,,,MANUAL,\r\n",
            csv
        )
    }

    @Test
    fun timeZoneChangesTheWrittenLocalTime() {
        val start = seoulTime(2026, Calendar.OCTOBER, 20, 1, 0)
        val session = session(id = 1L, start = start, end = start + 60 * 60_000L)

        val row = ActivityExportFormatter.toCsv(listOf(session), utc).split("\r\n")[1]

        assertTrue(row, row.contains(",2026-10-19T16:00:00Z,2026-10-19T17:00:00Z,"))
    }

    @Test
    fun fieldsWithCommaQuoteOrLineBreakAreQuoted() {
        assertEquals("plain", ActivityExportFormatter.escapeCsv("plain"))
        assertEquals("\"a,b\"", ActivityExportFormatter.escapeCsv("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", ActivityExportFormatter.escapeCsv("say \"hi\""))
        assertEquals("\"line1\nline2\"", ActivityExportFormatter.escapeCsv("line1\nline2"))
        assertEquals("\"cr\rhere\"", ActivityExportFormatter.escapeCsv("cr\rhere"))
        assertEquals("", ActivityExportFormatter.escapeCsv(""))
    }

    @Test
    fun noteTagsAndLinkedTodoAreWritten() {
        val start = seoulTime(2026, Calendar.OCTOBER, 20, 9, 0)
        val session = session(id = 3L, start = start, end = start + 30 * 60_000L).copy(
            title = "Chapter 3, part \"B\"",
            note = "first line\nsecond line",
            tags = listOf("exam", "review"),
            linkedTodoId = 42L,
            sourceType = "TIMER"
        )

        val csv = ActivityExportFormatter.toCsv(listOf(session), seoul)

        assertEquals(
            "$header\r\n" +
                "3,STUDY,\"Chapter 3, part \"\"B\"\"\",2026-10-20T09:00:00+09:00,2026-10-20T09:30:00+09:00,30," +
                "\"first line\nsecond line\",exam;review,42,TIMER,\r\n",
            csv
        )
    }

    @Test
    fun exerciseSetsAreWrittenAsQuotedJsonText() {
        val start = seoulTime(2026, Calendar.OCTOBER, 20, 18, 0)
        val sets = listOf(ExerciseSetRecord(name = "Squat", reps = 12, intensity = "MEDIUM"))
        val session = session(id = 9L, start = start, end = start + 20 * 60_000L).copy(
            category = "EXERCISE",
            title = "Squat",
            exerciseSets = sets
        )

        val row = ActivityExportFormatter.toCsv(listOf(session), seoul).split("\r\n")[1]
        val lastField = row.substringAfter(",MANUAL,")

        assertTrue(lastField, lastField.startsWith("\"[{\"\"name\"\":\"\"Squat\"\""))
        val unquoted = lastField.removeSurrounding("\"").replace("\"\"", "\"")
        val decoded = Json.decodeFromString(ListSerializer(ExerciseSetRecord.serializer()), unquoted)
        assertEquals(sets, decoded)
    }

    @Test
    fun rowsAreSortedOldestFirst() {
        val early = seoulTime(2026, Calendar.OCTOBER, 19, 8, 0)
        val late = seoulTime(2026, Calendar.OCTOBER, 20, 8, 0)
        val sessions = listOf(
            session(id = 2L, start = late, end = late + 60_000L),
            session(id = 1L, start = early, end = early + 60_000L)
        )

        val ids = ActivityExportFormatter.toCsv(sessions, seoul)
            .split("\r\n")
            .drop(1)
            .filter { it.isNotEmpty() }
            .map { it.substringBefore(",") }

        assertEquals(listOf("1", "2"), ids)
    }

    @Test
    fun jsonRoundTripsTheStoredModel() {
        val start = seoulTime(2026, Calendar.OCTOBER, 20, 18, 0)
        val sessions = listOf(
            session(id = 5L, start = start, end = start + 20 * 60_000L).copy(
                note = "memo",
                tags = listOf("a"),
                exerciseSets = listOf(ExerciseSetRecord(name = "Plank", reps = 0, intensity = "LOW", mode = "TIME", durationMillis = 60_000L))
            )
        )

        val json = ActivityExportFormatter.toJson(sessions)
        val decoded = Json.decodeFromString(ListSerializer(ActivitySession.serializer()), json)

        assertEquals(sessions, decoded)
        assertTrue(json.contains("\"sourceType\": \"MANUAL\""))
    }

    @Test
    fun defaultFileNameUsesLocalDateAndExtension() {
        val lateNightUtc = seoulTime(2026, Calendar.OCTOBER, 20, 1, 0)

        assertEquals(
            "flowlog-activities-20261020.csv",
            ActivityExportFormatter.defaultFileName(lateNightUtc, ActivityExportFormat.CSV, seoul)
        )
        assertEquals(
            "flowlog-activities-20261019.json",
            ActivityExportFormatter.defaultFileName(lateNightUtc, ActivityExportFormat.JSON, utc)
        )
    }

    private fun session(id: Long, start: Long, end: Long) = ActivitySession(
        id = id,
        category = "STUDY",
        title = "Reading notes",
        startTime = start,
        endTime = end,
        durationMillis = end - start,
        modifiedTime = end
    )

    private fun seoulTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(seoul).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
    }
}
