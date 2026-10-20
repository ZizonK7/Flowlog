package com.example.flowlog.ui.viewmodel

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.ExerciseSetRecord
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** File formats offered by "export records". */
enum class ActivityExportFormat(val mimeType: String, val extension: String) {
    CSV("text/csv", "csv"),
    JSON("application/json", "json")
}

/**
 * Pure formatting of saved activity records for export. No Android APIs, so it
 * is unit tested directly. Callers pass only non-deleted records.
 */
object ActivityExportFormatter {
    val CSV_COLUMNS = listOf(
        "id",
        "category",
        "title",
        "start",
        "end",
        "durationMinutes",
        "note",
        "tags",
        "linkedTodoId",
        "sourceType",
        "exerciseSets"
    )

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    private val compactJson = Json { encodeDefaults = true }

    /**
     * One header row plus one row per session, oldest first. Times are local
     * ISO 8601 with offset (e.g. 2026-10-20T09:30:00+09:00) in [timeZone].
     * Rows end with CRLF as RFC 4180 describes.
     */
    fun toCsv(sessions: List<ActivitySession>, timeZone: TimeZone): String {
        val builder = StringBuilder()
        builder.append(CSV_COLUMNS.joinToString(",")).append("\r\n")
        sortedForExport(sessions).forEach { session ->
            val row = listOf(
                session.id.toString(),
                session.category,
                session.title,
                formatLocalIso(session.startTime, timeZone),
                formatLocalIso(session.endTime, timeZone),
                TimeUnit.MILLISECONDS.toMinutes(session.durationMillis.coerceAtLeast(0L)).toString(),
                session.note.orEmpty(),
                session.tags.joinToString(";"),
                session.linkedTodoId?.toString().orEmpty(),
                session.sourceType,
                if (session.exerciseSets.isEmpty()) "" else compactJson.encodeToString(
                    ListSerializer(ExerciseSetRecord.serializer()),
                    session.exerciseSets
                )
            )
            builder.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }
        return builder.toString()
    }

    /** JSON array of the stored session model, oldest first. */
    fun toJson(sessions: List<ActivitySession>): String {
        return json.encodeToString(
            ListSerializer(ActivitySession.serializer()),
            sortedForExport(sessions)
        )
    }

    fun format(sessions: List<ActivitySession>, format: ActivityExportFormat, timeZone: TimeZone): String {
        return when (format) {
            ActivityExportFormat.CSV -> toCsv(sessions, timeZone)
            ActivityExportFormat.JSON -> toJson(sessions)
        }
    }

    /** Default file name, e.g. flowlog-activities-20261020.csv. */
    fun defaultFileName(nowMillis: Long, format: ActivityExportFormat, timeZone: TimeZone): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val date = String.format(
            Locale.US,
            "%04d%02d%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        return "flowlog-activities-$date.${format.extension}"
    }

    /** Quotes a field when it contains a comma, quote, or line break; doubles inner quotes. */
    fun escapeCsv(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    fun formatLocalIso(millis: Long, timeZone: TimeZone): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        formatter.timeZone = timeZone
        return formatter.format(Date(millis))
    }

    private fun sortedForExport(sessions: List<ActivitySession>): List<ActivitySession> =
        sessions.sortedWith(compareBy<ActivitySession> { it.startTime }.thenBy { it.id })
}
