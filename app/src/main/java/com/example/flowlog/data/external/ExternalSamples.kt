package com.example.flowlog.data.external

import com.example.flowlog.data.repository.EventMetadata
import java.util.Calendar
import java.util.TimeZone

/** `external_samples.provider`: where a sample was read. */
object ExternalSampleProvider {
    const val HEALTH_CONNECT = "health_connect"
    /** Android usage statistics (UsageStatsCollector). */
    const val USAGE_STATS = "usage_stats"
}

/** `external_samples.kind`. */
object ExternalSampleKind {
    const val SLEEP_SESSION = "sleep_session"
    const val STEPS_DAILY = "steps_daily"
    const val EXERCISE_SESSION = "exercise_session"
    /** Per package and local day: ms in front (UsageStatsAggregation). */
    const val APP_FOREGROUND_DAILY = "app_foreground_daily"
    /** One screen-on interval, Android 9+ (UsageStatsAggregation). */
    const val SCREEN_INTERACTIVE = "screen_interactive"
}

/** `external_samples.unit`. */
object ExternalSampleUnit {
    const val COUNT = "count"
    const val MINUTES = "minutes"
    const val MILLIS = "millis"
}

/** One sample before it gets an owner and timestamps (the parts compared on re-read). */
data class ExternalSampleRow(
    val sampleId: String,
    val provider: String,
    val kind: String,
    val startTime: Long,
    val endTime: Long,
    val value: Double,
    val unit: String,
    val sourcePackage: String?,
    val payloadJson: String?
)

// Inputs in plain values, so the conversions are tested without Health Connect classes.
data class SleepStageInput(val type: Int, val startMillis: Long, val endMillis: Long)

data class SleepSessionInput(
    val recordId: String?,
    val startMillis: Long,
    val endMillis: Long,
    val sourcePackage: String?,
    val stages: List<SleepStageInput>
)

data class ExerciseSessionInput(
    val recordId: String?,
    val startMillis: Long,
    val endMillis: Long,
    val sourcePackage: String?,
    val exerciseType: Int,
    val title: String?
)

/** One local day of steps from Health Connect's aggregate (already de-duplicated across apps). */
data class StepsDayInput(
    val dayStartMillis: Long,
    val dayEndMillis: Long,
    val count: Long,
    val sourcePackages: Collection<String>
)

data class FetchWindow(val startMillis: Long, val endMillis: Long, val firstFetch: Boolean)

enum class UpsertAction { INSERT, UPDATE, UNCHANGED }

/** Whether Health Connect can be used on this phone at all. */
enum class HealthConnectAvailability { NOT_SUPPORTED, NOT_INSTALLED, UPDATE_REQUIRED, AVAILABLE }

/** What the "앱 사용 통계" settings row shows. */
enum class UsageStatsState { OFF, NO_PERMISSION, CONNECTED }

/** What the settings row shows. */
enum class HealthConnectState { NOT_SUPPORTED, NOT_INSTALLED, UPDATE_REQUIRED, OFF, NO_PERMISSION, CONNECTED_PARTIAL, CONNECTED }

/**
 * Pure rules for external samples (no Android, no Health Connect classes): ids, the
 * conversion of each record kind into a row, the read window, when a fetch is due, and
 * whether a re-read sample is new, changed, or the same.
 */
object ExternalSamples {
    const val FETCH_INTERVAL_MILLIS = 6L * 60 * 60 * 1000
    const val WINDOW_DAYS = 7
    /** Health Connect lets an app read about 30 days before its first permission grant. */
    const val FIRST_WINDOW_DAYS = 30
    /** steps_daily is one row per day for all apps together, so its id has no app in it. */
    const val AGGREGATE_SOURCE_KEY = "aggregate"
    private const val NO_SOURCE_KEY = "-"
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /**
     * Plain text, so reading the same sample again lands on the same row (and the same
     * Firestore document):
     * - with a [recordId] (Health Connect sleep and exercise sessions): `<provider>:<kind>:<recordId>`,
     *   so a session whose times were edited in the source app still overwrites its own row;
     * - otherwise `<provider>:<kind>:<startTime>:<endTime>:<sourcePackage>` (`-` for no app).
     * A record id with `/` (not a valid Firestore document id) falls back to the second form.
     */
    fun sampleId(
        provider: String,
        kind: String,
        startTime: Long,
        endTime: Long,
        sourcePackage: String?,
        recordId: String? = null
    ): String {
        val record = recordId?.trim()?.takeIf { it.isNotEmpty() && '/' !in it }
        if (record != null) return listOf(provider, kind, record).joinToString(":")
        return listOf(
            provider,
            kind,
            startTime.toString(),
            endTime.toString(),
            sourcePackage?.trim()?.takeIf { it.isNotEmpty() } ?: NO_SOURCE_KEY
        ).joinToString(":")
    }

    fun minutesBetween(startMillis: Long, endMillis: Long): Double =
        (endMillis - startMillis).coerceAtLeast(0L) / 60_000.0

    fun sleepSessionRow(input: SleepSessionInput): ExternalSampleRow {
        val stages = input.stages.sortedBy { it.startMillis }.map { stage ->
            linkedMapOf<String, Any?>("type" to stage.type, "start" to stage.startMillis, "end" to stage.endMillis)
        }
        return ExternalSampleRow(
            sampleId = sampleId(
                ExternalSampleProvider.HEALTH_CONNECT,
                ExternalSampleKind.SLEEP_SESSION,
                input.startMillis,
                input.endMillis,
                input.sourcePackage,
                recordId = input.recordId
            ),
            provider = ExternalSampleProvider.HEALTH_CONNECT,
            kind = ExternalSampleKind.SLEEP_SESSION,
            startTime = input.startMillis,
            endTime = input.endMillis,
            value = minutesBetween(input.startMillis, input.endMillis),
            unit = ExternalSampleUnit.MINUTES,
            sourcePackage = input.sourcePackage?.takeIf { it.isNotBlank() },
            payloadJson = EventMetadata.json("recordId" to input.recordId, "stages" to stages)
        )
    }

    fun exerciseSessionRow(input: ExerciseSessionInput): ExternalSampleRow = ExternalSampleRow(
        sampleId = sampleId(
            ExternalSampleProvider.HEALTH_CONNECT,
            ExternalSampleKind.EXERCISE_SESSION,
            input.startMillis,
            input.endMillis,
            input.sourcePackage,
            recordId = input.recordId
        ),
        provider = ExternalSampleProvider.HEALTH_CONNECT,
        kind = ExternalSampleKind.EXERCISE_SESSION,
        startTime = input.startMillis,
        endTime = input.endMillis,
        value = minutesBetween(input.startMillis, input.endMillis),
        unit = ExternalSampleUnit.MINUTES,
        sourcePackage = input.sourcePackage?.takeIf { it.isNotBlank() },
        payloadJson = EventMetadata.json(
            "recordId" to input.recordId,
            "exerciseType" to input.exerciseType,
            "title" to input.title?.takeIf { it.isNotBlank() }
        )
    )

    fun stepsDayRow(input: StepsDayInput): ExternalSampleRow {
        val sources = input.sourcePackages.filter { it.isNotBlank() }.distinct().sorted()
        return ExternalSampleRow(
            sampleId = sampleId(
                ExternalSampleProvider.HEALTH_CONNECT,
                ExternalSampleKind.STEPS_DAILY,
                input.dayStartMillis,
                input.dayEndMillis,
                AGGREGATE_SOURCE_KEY
            ),
            provider = ExternalSampleProvider.HEALTH_CONNECT,
            kind = ExternalSampleKind.STEPS_DAILY,
            startTime = input.dayStartMillis,
            endTime = input.dayEndMillis,
            value = input.count.coerceAtLeast(0L).toDouble(),
            unit = ExternalSampleUnit.COUNT,
            sourcePackage = sources.takeIf { it.isNotEmpty() }?.joinToString(","),
            payloadJson = EventMetadata.json("sources" to sources)
        )
    }

    /** True when nothing was tried yet, [intervalMillis] has passed, or the clock went back. */
    fun isFetchDue(lastAttemptAt: Long?, now: Long, intervalMillis: Long = FETCH_INTERVAL_MILLIS): Boolean {
        val last = lastAttemptAt?.takeIf { it > 0L } ?: return true
        return now < last || now - last >= intervalMillis
    }

    /**
     * From local midnight [WINDOW_DAYS] days before today until [now]; the first fetch reaches
     * back [FIRST_WINDOW_DAYS]. When the last success is older than the normal window (phone off,
     * work not run), the window reaches back to the day before it, still at most 30 days.
     */
    fun fetchWindow(lastSuccessAt: Long?, now: Long, timeZone: TimeZone): FetchWindow {
        val last = lastSuccessAt?.takeIf { it in 1..now }
        val days = if (last == null) {
            FIRST_WINDOW_DAYS
        } else {
            val gapDays = ((now - last) / DAY_MILLIS).toInt() + 1
            gapDays.coerceIn(WINDOW_DAYS, FIRST_WINDOW_DAYS)
        }
        val start = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -days)
        }.timeInMillis
        return FetchWindow(startMillis = start, endMillis = now, firstFetch = last == null)
    }

    /**
     * New row, a changed one (written again and uploaded again), or the same as stored.
     * [sameOwner] false (another account's row with this id) counts as changed.
     */
    fun upsertAction(existing: ExternalSampleRow?, incoming: ExternalSampleRow, sameOwner: Boolean = true): UpsertAction = when {
        existing == null -> UpsertAction.INSERT
        !sameOwner || existing != incoming -> UpsertAction.UPDATE
        else -> UpsertAction.UNCHANGED
    }

    fun usageStatsState(enabled: Boolean, granted: Boolean): UsageStatsState = when {
        !enabled -> UsageStatsState.OFF
        !granted -> UsageStatsState.NO_PERMISSION
        else -> UsageStatsState.CONNECTED
    }

    fun healthConnectState(
        availability: HealthConnectAvailability,
        enabled: Boolean,
        grantedCount: Int,
        requiredCount: Int
    ): HealthConnectState = when (availability) {
        HealthConnectAvailability.NOT_SUPPORTED -> HealthConnectState.NOT_SUPPORTED
        HealthConnectAvailability.NOT_INSTALLED -> HealthConnectState.NOT_INSTALLED
        HealthConnectAvailability.UPDATE_REQUIRED -> HealthConnectState.UPDATE_REQUIRED
        HealthConnectAvailability.AVAILABLE -> when {
            !enabled -> HealthConnectState.OFF
            grantedCount <= 0 -> HealthConnectState.NO_PERMISSION
            grantedCount < requiredCount -> HealthConnectState.CONNECTED_PARTIAL
            else -> HealthConnectState.CONNECTED
        }
    }
}
