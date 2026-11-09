package com.example.flowlog.data.external

import com.example.flowlog.data.repository.EventMetadata
import java.util.Calendar
import java.util.TimeZone

/** One `UsageEvents.Event` in plain values, so the aggregation is tested on the JVM. */
data class UsageEvent(
    val packageName: String,
    val type: Int,
    val timestamp: Long,
    val className: String? = null
)

/**
 * The event types read, with the same values as `android.app.usage.UsageEvents.Event`
 * (written out so these rules need no Android classes).
 */
object UsageEventType {
    /** ACTIVITY_RESUMED (API 29); the same value as MOVE_TO_FOREGROUND before it. */
    const val FOREGROUND = 1
    /** ACTIVITY_PAUSED (API 29); the same value as MOVE_TO_BACKGROUND before it. */
    const val BACKGROUND = 2
    /** API 28+. */
    const val SCREEN_INTERACTIVE = 15
    /** API 28+. */
    const val SCREEN_NON_INTERACTIVE = 16
    /** API 29+: closes whatever was still open. */
    const val DEVICE_SHUTDOWN = 26

    val READ: Set<Int> = setOf(FOREGROUND, BACKGROUND, SCREEN_INTERACTIVE, SCREEN_NON_INTERACTIVE, DEVICE_SHUTDOWN)
}

data class TimeInterval(val start: Long, val end: Long)

/** A part of an interval inside one local day [dayStart, dayEnd). */
data class DayPiece(val dayStart: Long, val dayEnd: Long, val start: Long, val end: Long)

/**
 * Turns raw usage events into `external_samples` rows (pure; no Android).
 *
 * - `app_foreground_daily`: per package and local day, the time an activity of it was in front.
 *   Intervals pair FOREGROUND with BACKGROUND of the same activity (package + class); a
 *   BACKGROUND with no open start (it began before the window) is ignored; a second
 *   FOREGROUND with no end in between replaces the first (its end is unknown); DEVICE_SHUTDOWN
 *   closes all; still open at the window end = until the window end. Overlapping intervals of
 *   one package (two of its activities at once) are merged so time is not counted twice. Cut at
 *   local midnights; one row per (package, day) with startTime = that day's 0:00, endTime = the
 *   next day's 0:00, value = ms, payload `{sessions: n}` (intervals that day).
 * - `screen_interactive`: one row per SCREEN_INTERACTIVE → SCREEN_NON_INTERACTIVE pair, not cut
 *   at midnight. An off with no on is ignored; a screen still on at the window end gives no row
 *   (closing it at the window end would make a new id on every fetch; the next fetch reads it whole).
 */
object UsageStatsAggregation {

    fun foregroundIntervals(events: List<UsageEvent>, windowEnd: Long): Map<String, List<TimeInterval>> {
        val open = LinkedHashMap<Pair<String, String?>, Long>()
        val closed = LinkedHashMap<String, MutableList<TimeInterval>>()
        fun close(key: Pair<String, String?>, at: Long) {
            val start = open.remove(key) ?: return
            if (at > start) closed.getOrPut(key.first) { mutableListOf() } += TimeInterval(start, at)
        }
        for (event in events.sortedBy { it.timestamp }) {
            val key = event.packageName to event.className
            when (event.type) {
                UsageEventType.FOREGROUND -> open[key] = event.timestamp
                UsageEventType.BACKGROUND -> close(key, event.timestamp)
                UsageEventType.DEVICE_SHUTDOWN -> open.keys.toList().forEach { close(it, event.timestamp) }
            }
        }
        open.keys.toList().forEach { close(it, windowEnd) }
        return closed.mapValues { (_, intervals) -> mergeOverlapping(intervals) }
    }

    fun mergeOverlapping(intervals: List<TimeInterval>): List<TimeInterval> {
        val sorted = intervals.filter { it.end > it.start }.sortedBy { it.start }
        val out = mutableListOf<TimeInterval>()
        for (interval in sorted) {
            val last = out.lastOrNull()
            if (last != null && interval.start <= last.end) {
                out[out.size - 1] = TimeInterval(last.start, maxOf(last.end, interval.end))
            } else {
                out += interval
            }
        }
        return out
    }

    fun splitAtMidnights(start: Long, end: Long, timeZone: TimeZone): List<DayPiece> {
        val out = mutableListOf<DayPiece>()
        var cursor = start
        while (cursor < end) {
            val dayStart = startOfDay(cursor, timeZone)
            val dayEnd = nextDayStart(dayStart, timeZone)
            val pieceEnd = minOf(end, dayEnd)
            out += DayPiece(dayStart = dayStart, dayEnd = dayEnd, start = cursor, end = pieceEnd)
            cursor = pieceEnd
        }
        return out
    }

    fun appForegroundDailyRows(
        events: List<UsageEvent>,
        windowStart: Long,
        windowEnd: Long,
        timeZone: TimeZone
    ): List<ExternalSampleRow> {
        data class Day(val dayEnd: Long, var millis: Long = 0L, var sessions: Int = 0)
        val days = LinkedHashMap<Pair<String, Long>, Day>()
        for ((packageName, intervals) in foregroundIntervals(events, windowEnd)) {
            for (interval in intervals) {
                val start = maxOf(interval.start, windowStart)
                val end = minOf(interval.end, windowEnd)
                if (end <= start) continue
                for (piece in splitAtMidnights(start, end, timeZone)) {
                    val day = days.getOrPut(packageName to piece.dayStart) { Day(dayEnd = piece.dayEnd) }
                    day.millis += piece.end - piece.start
                    day.sessions += 1
                }
            }
        }
        return days.entries
            .sortedWith(compareBy({ it.key.second }, { it.key.first }))
            .map { (key, day) ->
                val (packageName, dayStart) = key
                ExternalSampleRow(
                    sampleId = ExternalSamples.sampleId(
                        ExternalSampleProvider.USAGE_STATS,
                        ExternalSampleKind.APP_FOREGROUND_DAILY,
                        dayStart,
                        day.dayEnd,
                        packageName
                    ),
                    provider = ExternalSampleProvider.USAGE_STATS,
                    kind = ExternalSampleKind.APP_FOREGROUND_DAILY,
                    startTime = dayStart,
                    endTime = day.dayEnd,
                    value = day.millis.toDouble(),
                    unit = ExternalSampleUnit.MILLIS,
                    sourcePackage = packageName,
                    payloadJson = EventMetadata.json("sessions" to day.sessions)
                )
            }
    }

    fun screenInteractiveRows(events: List<UsageEvent>): List<ExternalSampleRow> {
        val out = mutableListOf<ExternalSampleRow>()
        var onAt: Long? = null
        fun close(at: Long) {
            val start = onAt ?: return
            onAt = null
            if (at <= start) return
            out += ExternalSampleRow(
                sampleId = ExternalSamples.sampleId(
                    ExternalSampleProvider.USAGE_STATS,
                    ExternalSampleKind.SCREEN_INTERACTIVE,
                    start,
                    at,
                    null
                ),
                provider = ExternalSampleProvider.USAGE_STATS,
                kind = ExternalSampleKind.SCREEN_INTERACTIVE,
                startTime = start,
                endTime = at,
                value = (at - start).toDouble(),
                unit = ExternalSampleUnit.MILLIS,
                sourcePackage = null,
                payloadJson = null
            )
        }
        for (event in events.sortedBy { it.timestamp }) {
            when (event.type) {
                UsageEventType.SCREEN_INTERACTIVE -> onAt = event.timestamp
                UsageEventType.SCREEN_NON_INTERACTIVE, UsageEventType.DEVICE_SHUTDOWN -> close(event.timestamp)
            }
        }
        return out
    }

    private fun startOfDay(millis: Long, timeZone: TimeZone): Long = Calendar.getInstance(timeZone).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun nextDayStart(dayStart: Long, timeZone: TimeZone): Long = Calendar.getInstance(timeZone).apply {
        timeInMillis = dayStart
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}
