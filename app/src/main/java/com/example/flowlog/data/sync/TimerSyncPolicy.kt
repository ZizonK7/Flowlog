package com.example.flowlog.data.sync

/**
 * Shared running timer between the phone and the PC web app: one server document,
 * `users/{uid}/flowlog/timer`. This file is the pure part (no Android, no Firebase) so the
 * decisions can be unit tested; [RemoteTimerSync] reads and writes the document.
 *
 * The phone's [com.example.flowlog.data.local.TimerStateStore] stays the source of truth
 * for the local timer. The document only carries what the web needs to show or follow it.
 *
 * Rule: the later action wins. A web start that happened after the phone's timer started
 * replaces the phone's timer (without saving a record); an older one is overwritten by
 * the phone's state. A web stop of the same timer (same startTime) ends the phone's timer
 * without a record, because the web already saved it.
 */
object TimerSyncPolicy {
    const val STATUS_RUNNING = "running"
    const val STATUS_PAUSED = "paused"
    const val STATUS_STOPPED = "stopped"
    const val BY_APP = "app"
    const val BY_WEB = "web"

    private val STATUSES = setOf(STATUS_RUNNING, STATUS_PAUSED, STATUS_STOPPED)

    /**
     * What to do with one snapshot of the server document.
     *
     * @param local the phone's current timer, or null when none is running.
     * @param remote the document, or null when it does not exist.
     * @param lastEndedStartTime startTime of the last timer this phone ended (stopped,
     *   cancelled, or replaced). A web "running" document for that same timer is stale:
     *   the phone already finished it and its "stopped" write did not reach the server.
     */
    fun decide(
        local: LocalTimer?,
        remote: RemoteTimer?,
        myDeviceId: String,
        lastEndedStartTime: Long?
    ): TimerSyncAction {
        if (remote == null) {
            return if (local != null) TimerSyncAction.Republish else TimerSyncAction.Ignore(IgnoreReason.NO_DOCUMENT)
        }
        // Our own write still on its way, or a cached copy that may be out of date.
        if (remote.hasPendingWrites) return TimerSyncAction.Ignore(IgnoreReason.PENDING_WRITE)
        if (remote.isFromCache) return TimerSyncAction.Ignore(IgnoreReason.FROM_CACHE)

        val fromWeb = remote.updatedBy == BY_WEB && remote.deviceId != myDeviceId
        if (!fromWeb) {
            // Echo of this phone's write. If it no longer matches the phone (a write was lost,
            // or the timer changed while signed out), write the phone's state again.
            if (remote.deviceId == myDeviceId && !mirrors(remote, local)) return TimerSyncAction.Republish
            return TimerSyncAction.Ignore(
                if (remote.deviceId == myDeviceId) IgnoreReason.ECHO else IgnoreReason.OTHER_APP_DEVICE
            )
        }
        if (remote.status !in STATUSES || remote.category.isBlank() || remote.startTime <= 0L) {
            return TimerSyncAction.Ignore(IgnoreReason.INVALID)
        }
        val updatedAt = remote.updatedAtMillis ?: return TimerSyncAction.Ignore(IgnoreReason.INVALID)

        if (remote.status == STATUS_STOPPED) {
            return if (local != null && local.startTime == remote.startTime) {
                TimerSyncAction.CancelLocal
            } else {
                TimerSyncAction.Ignore(IgnoreReason.OTHER_TIMER_STOPPED)
            }
        }

        // running, or paused: the phone has no pause/resume, so a paused web timer is
        // followed as a running one with the same startTime.
        if (local == null) {
            if (lastEndedStartTime != null && remote.startTime == lastEndedStartTime) {
                return TimerSyncAction.Ignore(IgnoreReason.STALE)
            }
            return TimerSyncAction.StartRemote(remote, replacesLocal = false)
        }
        if (local.startTime == remote.startTime) {
            return if (local.category != remote.category || local.title != remote.title) {
                TimerSyncAction.UpdateFromRemote(remote)
            } else {
                TimerSyncAction.Ignore(IgnoreReason.SAME_TIMER)
            }
        }
        // Two different timers: the later action wins. updatedAt is server time and
        // startTime is the phone's clock, so a skewed phone clock shifts this comparison.
        return if (updatedAt > local.startTime) {
            TimerSyncAction.StartRemote(remote, replacesLocal = true)
        } else {
            TimerSyncAction.Republish
        }
    }

    /** True when [remote] already describes [local] (or "no timer" when local is null). */
    fun mirrors(remote: RemoteTimer, local: LocalTimer?): Boolean {
        if (local == null) return remote.status == STATUS_STOPPED
        val status = if (local.paused) STATUS_PAUSED else STATUS_RUNNING
        return remote.status == status &&
            remote.startTime == local.startTime &&
            remote.category == local.category &&
            remote.title == local.title
    }

    /**
     * Fields the phone writes, without `updatedAt` (the caller adds the server timestamp).
     * A stopped timer keeps its category, title and startTime so the web can tell which
     * timer ended.
     */
    fun documentFields(timer: LocalTimer, stopped: Boolean, deviceId: String): Map<String, Any?> = mapOf(
        "status" to when {
            stopped -> STATUS_STOPPED
            timer.paused -> STATUS_PAUSED
            else -> STATUS_RUNNING
        },
        "category" to timer.category,
        "title" to timer.title,
        "startTime" to timer.startTime,
        "pausedElapsedMs" to if (timer.paused && !stopped) timer.pausedElapsedMs.coerceAtLeast(0L) else 0L,
        "linkedTodoId" to timer.linkedTodoId,
        "updatedBy" to BY_APP,
        "deviceId" to deviceId
    )

    /** Reads document fields; `updatedAt` arrives separately as server time in millis. */
    fun remoteFrom(
        fields: Map<String, Any?>,
        updatedAtMillis: Long?,
        hasPendingWrites: Boolean,
        isFromCache: Boolean
    ): RemoteTimer = RemoteTimer(
        status = fields["status"] as? String ?: "",
        category = (fields["category"] as? String).orEmpty(),
        title = (fields["title"] as? String).orEmpty(),
        startTime = (fields["startTime"] as? Number)?.toLong() ?: 0L,
        pausedElapsedMs = (fields["pausedElapsedMs"] as? Number)?.toLong() ?: 0L,
        linkedTodoId = when (val id = fields["linkedTodoId"]) {
            is String -> id.takeIf { it.isNotBlank() }
            is Number -> id.toLong().toString()
            else -> null
        },
        updatedAtMillis = updatedAtMillis,
        updatedBy = fields["updatedBy"] as? String,
        deviceId = fields["deviceId"] as? String,
        hasPendingWrites = hasPendingWrites,
        isFromCache = isFromCache
    )
}

/** The phone's timer as the shared document sees it. */
data class LocalTimer(
    val category: String,
    val title: String,
    val startTime: Long,
    val paused: Boolean = false,
    val pausedElapsedMs: Long = 0L,
    val linkedTodoId: String? = null
)

/** One snapshot of `users/{uid}/flowlog/timer` with its snapshot metadata. */
data class RemoteTimer(
    val status: String,
    val category: String,
    val title: String,
    val startTime: Long,
    val pausedElapsedMs: Long = 0L,
    val linkedTodoId: String? = null,
    val updatedAtMillis: Long?,
    val updatedBy: String?,
    val deviceId: String?,
    val hasPendingWrites: Boolean = false,
    val isFromCache: Boolean = false
)

sealed interface TimerSyncAction {
    data class Ignore(val reason: IgnoreReason) : TimerSyncAction

    /** Start the web's timer here; [replacesLocal] = end the phone's timer first, no record. */
    data class StartRemote(val remote: RemoteTimer, val replacesLocal: Boolean) : TimerSyncAction

    /** Same timer (same startTime); take the web's category and title. */
    data class UpdateFromRemote(val remote: RemoteTimer) : TimerSyncAction

    /** The web stopped this timer and saved the record; end it here without a record. */
    data object CancelLocal : TimerSyncAction

    /** The server copy is older than the phone's state; write the phone's state again. */
    data object Republish : TimerSyncAction
}

enum class IgnoreReason {
    NO_DOCUMENT,
    PENDING_WRITE,
    FROM_CACHE,
    ECHO,
    OTHER_APP_DEVICE,
    INVALID,
    OTHER_TIMER_STOPPED,
    STALE,
    SAME_TIMER
}
