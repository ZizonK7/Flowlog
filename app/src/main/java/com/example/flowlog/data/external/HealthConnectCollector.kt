package com.example.flowlog.data.external

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.flowlog.data.constants.EventSource
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.local.ExternalDataSettingsStore
import com.example.flowlog.data.repository.EventLogRepository
import com.example.flowlog.data.repository.EventMetadata
import com.example.flowlog.data.repository.ExternalSampleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.util.TimeZone
import kotlin.reflect.KClass

data class ExternalFetchResult(
    val fetched: Int,
    val inserted: Int,
    val updated: Int,
    val error: String? = null
)

/**
 * Reads sleep sessions, daily step totals, and exercise sessions from Health Connect into
 * `external_samples` (read only; routes, heart rate and anything else are not read).
 * Window: [ExternalSamples.fetchWindow] (7 days, first fetch 30). Each fetch, success or
 * failure, leaves one EXTERNAL_SAMPLES_FETCHED event. Samples are only stored: sleep is not
 * turned into an activity, and nothing here feeds a recommendation.
 */
@RequiresApi(Build.VERSION_CODES.P)
class HealthConnectCollector(context: Context) {
    private val appContext = context.applicationContext

    suspend fun collect(trigger: String): ExternalFetchResult = collectMutex.withLock { collectLocked(trigger) }

    private suspend fun collectLocked(trigger: String): ExternalFetchResult {
        val now = System.currentTimeMillis()
        val window = ExternalSamples.fetchWindow(
            ExternalDataSettingsStore.lastFetchedAt(appContext, PROVIDER),
            now,
            TimeZone.getDefault()
        )
        ExternalDataSettingsStore.markAttempt(appContext, PROVIDER, now)
        val kinds = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val result = try {
            val client = HealthConnectClient.getOrCreate(appContext)
            val granted = client.permissionController.getGrantedPermissions()
            val rows = mutableListOf<ExternalSampleRow>()
            suspend fun read(kind: String, permission: String, block: suspend () -> List<ExternalSampleRow>) {
                if (permission !in granted) return
                try {
                    rows += block()
                    kinds += kind
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errors += "$kind:${e.javaClass.simpleName}"
                }
            }
            read(ExternalSampleKind.SLEEP_SESSION, HealthConnectAccess.READ_SLEEP) { readSleep(client, window) }
            read(ExternalSampleKind.STEPS_DAILY, HealthConnectAccess.READ_STEPS) { readStepsDaily(client, window) }
            read(ExternalSampleKind.EXERCISE_SESSION, HealthConnectAccess.READ_EXERCISE) { readExercise(client, window) }
            if (kinds.isEmpty()) {
                ExternalFetchResult(0, 0, 0, error = errors.joinToString(";").ifEmpty { ERROR_NO_PERMISSION })
            } else {
                // fetchedAt = write time, so an upload that began earlier never marks these synced.
                val counts = ExternalSampleRepository(appContext).upsert(rows, fetchedAt = System.currentTimeMillis())
                ExternalDataSettingsStore.markFetched(appContext, PROVIDER, now)
                ExternalFetchResult(
                    fetched = rows.size,
                    inserted = counts.inserted,
                    updated = counts.updated,
                    error = errors.joinToString(";").ifEmpty { null }
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ExternalFetchResult(0, 0, 0, error = e.javaClass.simpleName)
        }
        logFetch(window, result, trigger, kinds)
        return result
    }

    private suspend fun readSleep(client: HealthConnectClient, window: FetchWindow): List<ExternalSampleRow> =
        readAll(client, SleepSessionRecord::class, instantRange(window)).map { record ->
            ExternalSamples.sleepSessionRow(
                SleepSessionInput(
                    recordId = record.metadata.id.takeIf { it.isNotBlank() },
                    startMillis = record.startTime.toEpochMilli(),
                    endMillis = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    stages = record.stages.map { stage ->
                        SleepStageInput(
                            type = stage.stage,
                            startMillis = stage.startTime.toEpochMilli(),
                            endMillis = stage.endTime.toEpochMilli()
                        )
                    }
                )
            )
        }

    private suspend fun readExercise(client: HealthConnectClient, window: FetchWindow): List<ExternalSampleRow> =
        readAll(client, ExerciseSessionRecord::class, instantRange(window)).map { record ->
            // Only type and title; the route (location) is never read.
            ExternalSamples.exerciseSessionRow(
                ExerciseSessionInput(
                    recordId = record.metadata.id.takeIf { it.isNotBlank() },
                    startMillis = record.startTime.toEpochMilli(),
                    endMillis = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    exerciseType = record.exerciseType,
                    title = record.title
                )
            )
        }

    /**
     * One row per local day, from Health Connect's own aggregate (it de-duplicates steps that
     * several apps wrote). The range runs to tomorrow's midnight so today's day keeps the same
     * start and end (and id) on every fetch while its count grows.
     */
    private suspend fun readStepsDaily(client: HealthConnectClient, window: FetchWindow): List<ExternalSampleRow> {
        val zone = ZoneId.systemDefault()
        val firstDay = Instant.ofEpochMilli(window.startMillis).atZone(zone).toLocalDate()
        val dayAfterLast = Instant.ofEpochMilli(window.endMillis).atZone(zone).toLocalDate().plusDays(1)
        val groups = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(firstDay.atStartOfDay(), dayAfterLast.atStartOfDay()),
                timeRangeSlicer = Period.ofDays(1)
            )
        )
        return groups.mapNotNull { group ->
            val count = group.result[StepsRecord.COUNT_TOTAL] ?: return@mapNotNull null
            ExternalSamples.stepsDayRow(
                StepsDayInput(
                    dayStartMillis = group.startTime.atZone(zone).toInstant().toEpochMilli(),
                    dayEndMillis = group.endTime.atZone(zone).toInstant().toEpochMilli(),
                    count = count,
                    sourcePackages = group.result.dataOrigins.map { it.packageName }
                )
            )
        }
    }

    private suspend fun <T : Record> readAll(
        client: HealthConnectClient,
        type: KClass<T>,
        filter: TimeRangeFilter
    ): List<T> {
        val out = mutableListOf<T>()
        var pageToken: String? = null
        var pages = 0
        do {
            val response = client.readRecords(
                ReadRecordsRequest(recordType = type, timeRangeFilter = filter, pageToken = pageToken)
            )
            out += response.records
            pageToken = response.pageToken
            pages++
        } while (pageToken != null && pages < MAX_PAGES)
        return out
    }

    private fun instantRange(window: FetchWindow): TimeRangeFilter =
        TimeRangeFilter.between(Instant.ofEpochMilli(window.startMillis), Instant.ofEpochMilli(window.endMillis))

    private suspend fun logFetch(window: FetchWindow, result: ExternalFetchResult, trigger: String, kinds: List<String>) {
        runCatching {
            EventLogRepository(appContext).log(
                eventType = EventType.EXTERNAL_SAMPLES_FETCHED,
                source = EventSource.SYSTEM,
                metadataJson = EventMetadata.externalSamplesFetched(
                    provider = PROVIDER,
                    windowStart = window.startMillis,
                    windowEnd = window.endMillis,
                    fetched = result.fetched,
                    inserted = result.inserted,
                    updated = result.updated,
                    error = result.error,
                    trigger = trigger,
                    kinds = kinds
                )
            )
        }
    }

    companion object {
        private const val PROVIDER = ExternalSampleProvider.HEALTH_CONNECT
        private const val MAX_PAGES = 50
        const val ERROR_NO_PERMISSION = "no_permission"
        private val collectMutex = Mutex()
    }
}
