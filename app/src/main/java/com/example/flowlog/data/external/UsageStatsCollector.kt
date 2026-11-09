package com.example.flowlog.data.external

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.example.flowlog.data.constants.EventSource
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.local.ExternalDataSettingsStore
import com.example.flowlog.data.repository.EventLogRepository
import com.example.flowlog.data.repository.EventMetadata
import com.example.flowlog.data.repository.ExternalSampleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TimeZone

/**
 * Reads the system usage events into `external_samples`: per-app foreground time per local day
 * (`app_foreground_daily`) and screen-on intervals (`screen_interactive`, Android 9+). Package
 * names are stored as they are (Flowlog's own included); notification contents, locations and
 * anything else are not read. Window: [ExternalSamples.fetchWindow] (7 days, first fetch 30;
 * the system may keep fewer days of events and then returns what it has). One
 * EXTERNAL_SAMPLES_FETCHED event per fetch, success or failure.
 *
 * The rows are built from raw events ([UsageStatsAggregation]), not from
 * `queryUsageStats(INTERVAL_DAILY)`: its buckets follow the system's own day boundaries, which
 * differ between devices and versions and are not the local midnight, so the same day could
 * come back with different bounds (and ids) or mixed with the next day.
 */
class UsageStatsCollector(context: Context) {
    private val appContext = context.applicationContext

    suspend fun collect(trigger: String): ExternalFetchResult = collectMutex.withLock { collectLocked(trigger) }

    private suspend fun collectLocked(trigger: String): ExternalFetchResult {
        val now = System.currentTimeMillis()
        val timeZone = TimeZone.getDefault()
        val window = ExternalSamples.fetchWindow(
            ExternalDataSettingsStore.lastFetchedAt(appContext, PROVIDER),
            now,
            timeZone
        )
        val screenSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val kinds = mutableListOf<String>()
        if (!UsageAccess.isGranted(appContext)) {
            // Not counted as an attempt: once the user grants access, the next foreground fetches
            // at once instead of waiting 6 hours.
            val result = ExternalFetchResult(0, 0, 0, error = ERROR_NO_PERMISSION)
            logFetch(window, result, trigger, kinds, screenSupported)
            return result
        }
        ExternalDataSettingsStore.markAttempt(appContext, PROVIDER, now)
        val result = try {
            val events = readEvents(window)
            val rows = UsageStatsAggregation.appForegroundDailyRows(events, window.startMillis, window.endMillis, timeZone)
                .also { kinds += ExternalSampleKind.APP_FOREGROUND_DAILY }
                .toMutableList()
            if (screenSupported) {
                rows += UsageStatsAggregation.screenInteractiveRows(events)
                kinds += ExternalSampleKind.SCREEN_INTERACTIVE
            }
            // fetchedAt = write time, so an upload that began earlier never marks these synced.
            val counts = ExternalSampleRepository(appContext).upsert(rows, fetchedAt = System.currentTimeMillis())
            ExternalDataSettingsStore.markFetched(appContext, PROVIDER, now)
            ExternalFetchResult(fetched = rows.size, inserted = counts.inserted, updated = counts.updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            kinds.clear()
            ExternalFetchResult(0, 0, 0, error = e.javaClass.simpleName)
        }
        logFetch(window, result, trigger, kinds, screenSupported)
        return result
    }

    private fun readEvents(window: FetchWindow): List<UsageEvent> {
        val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        // null while the phone is still locked after a restart (user storage not unlocked yet).
        val events = usageStatsManager.queryEvents(window.startMillis, window.endMillis) ?: return emptyList()
        val out = mutableListOf<UsageEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            if (!events.getNextEvent(event)) break
            if (event.eventType !in UsageEventType.READ) continue
            val packageName = event.packageName ?: continue
            out += UsageEvent(
                packageName = packageName,
                type = event.eventType,
                timestamp = event.timeStamp,
                className = event.className
            )
        }
        return out
    }

    private suspend fun logFetch(
        window: FetchWindow,
        result: ExternalFetchResult,
        trigger: String,
        kinds: List<String>,
        screenSupported: Boolean
    ) {
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
                    kinds = kinds,
                    screenEventsSupported = if (screenSupported) null else false
                )
            )
        }
    }

    companion object {
        private const val PROVIDER = ExternalSampleProvider.USAGE_STATS
        const val ERROR_NO_PERMISSION = "no_permission"
        private val collectMutex = Mutex()
    }
}
