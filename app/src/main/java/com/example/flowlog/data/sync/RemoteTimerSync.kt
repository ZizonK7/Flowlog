package com.example.flowlog.data.sync

import android.content.Context
import android.util.Log
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.local.ActiveTimerState
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.TimerStatus
import com.example.flowlog.data.remote.awaitResult
import com.example.flowlog.notification.ActivityTimerNotifier
import com.example.flowlog.notification.RoutineGoalAlarmScheduler
import com.example.flowlog.widget.FlowStatusWidgetProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Shares the running timer with the PC web app through `users/{uid}/flowlog/timer`.
 *
 * - Writing: [TimerStateStore] calls [publishLocalChange] on every change of the active
 *   timer (start, title/category change, pause, stop, cancel), from screens and receivers
 *   alike. The write is a merge `set` left to Firestore's offline queue; failures are only
 *   logged.
 * - Listening: [listen] follows web changes while the app is in the foreground and signed
 *   in. Decisions are in [TimerSyncPolicy]; changes are applied to [TimerStateStore] with
 *   `publish = false` so they are not written back. The home screen's ViewModel picks the
 *   new state up from the store (it polls it every second), like timers started by alarms.
 *
 * Logs never include titles or the uid.
 */
object RemoteTimerSync {
    private const val TAG = "RemoteTimerSync"
    private const val PREFS = "flowlog_timer_sync"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_LAST_ENDED_START_TIME = "last_ended_start_time"

    /** A random id made once per install (not personal data); tells this phone's writes apart. */
    fun deviceId(context: Context): String {
        val prefs = prefs(context)
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    fun toLocal(state: ActiveTimerState): LocalTimer = LocalTimer(
        category = state.category,
        title = state.pendingTitle ?: state.linkedTodoTitle ?: "",
        startTime = state.startTime,
        paused = state.status == TimerStatus.PAUSED,
        pausedElapsedMs = state.pausedElapsedMillis,
        linkedTodoId = state.linkedTodoId?.takeIf { it > 0L }?.toString()
    )

    /**
     * Called by [TimerStateStore] after a local change. [current] null means the timer
     * ended; [previous] then says which one, and the document is marked "stopped".
     */
    fun publishLocalChange(context: Context, current: ActiveTimerState?, previous: ActiveTimerState?) {
        val timer = current ?: previous ?: return
        runCatching {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            write(uid, TimerSyncPolicy.documentFields(toLocal(timer), stopped = current == null, deviceId(context)))
        }.onFailure { Log.w(TAG, "timer publish skipped: ${it.javaClass.simpleName}") }
    }

    /** Remembers which timer this phone ended last (see TimerSyncPolicy.decide). */
    fun recordLocalEnd(context: Context, startTime: Long) {
        runCatching { prefs(context).edit().putLong(KEY_LAST_ENDED_START_TIME, startTime).apply() }
    }

    /** Starts following the document. Remove the returned registration on sign-out or stop. */
    fun listen(context: Context, uid: String): ListenerRegistration {
        val appContext = context.applicationContext
        return timerDocument(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "timer listener error: ${error.code}")
                return@addSnapshotListener
            }
            if (snapshot == null || FirebaseAuth.getInstance().currentUser?.uid != uid) return@addSnapshotListener
            runCatching { handleSnapshot(appContext, uid, snapshot) }
                .onFailure { Log.w(TAG, "timer snapshot not applied: ${it.javaClass.simpleName}") }
        }
    }

    /**
     * Account deletion: remove the shared timer before the phone is wiped and signed out,
     * so the web stops showing it. Waits briefly; a failure leaves it to the server job.
     */
    suspend fun deleteForWipe(context: Context) {
        runCatching {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            withTimeoutOrNull(3_000L) { timerDocument(uid).delete().awaitResult() }
        }.onFailure { Log.w(TAG, "timer document not deleted: ${it.javaClass.simpleName}") }
    }

    private fun handleSnapshot(context: Context, uid: String, snapshot: DocumentSnapshot) {
        val metadata = snapshot.metadata
        // A missing document seen only in the local cache says nothing about the server.
        if (!snapshot.exists() && (metadata.isFromCache || metadata.hasPendingWrites())) return
        val remote = snapshot.data?.let { fields ->
            TimerSyncPolicy.remoteFrom(
                fields = fields,
                updatedAtMillis = snapshot.getTimestamp("updatedAt")?.toDate()?.time,
                hasPendingWrites = metadata.hasPendingWrites(),
                isFromCache = metadata.isFromCache
            )
        }
        val activeTimer = TimerStateStore.getActiveTimer(context)
        val action = TimerSyncPolicy.decide(
            local = activeTimer?.let(::toLocal),
            remote = remote,
            myDeviceId = deviceId(context),
            lastEndedStartTime = prefs(context).getLong(KEY_LAST_ENDED_START_TIME, -1L).takeIf { it > 0L }
        )
        if (action !is TimerSyncAction.Ignore) Log.i(TAG, "timer sync action: ${action.javaClass.simpleName}")
        when (action) {
            is TimerSyncAction.Ignore -> Unit
            is TimerSyncAction.StartRemote -> {
                if (action.replacesLocal) endLocalWithoutRecord(context)
                startFromRemote(context, action.remote)
            }
            is TimerSyncAction.UpdateFromRemote -> activeTimer?.let { updateFromRemote(context, it, action.remote) }
            TimerSyncAction.CancelLocal -> endLocalWithoutRecord(context)
            TimerSyncAction.Republish -> republish(uid, context, activeTimer, remote)
        }
    }

    /** Same steps as the alarm receivers' start: store, widget, running notification. */
    private fun startFromRemote(context: Context, remote: RemoteTimer) {
        TimerStateStore.saveActiveTimer(
            context = context,
            category = remote.category,
            startTime = remote.startTime,
            goalMillis = TimerStateStore.DEFAULT_GOAL_MILLIS,
            linkedTodoId = remote.linkedTodoId?.toLongOrNull(),
            pendingTitle = remote.title.ifBlank { null },
            sourceType = ActivitySourceType.MANUAL,
            publish = false
        )
        FlowStatusWidgetProvider.updateAll(context)
        ActivityTimerNotifier(context).apply {
            clearRunningTimer()
            if (remote.category != "SLEEP") showRunningTimer(remote.category, remote.startTime)
        }
    }

    private fun updateFromRemote(context: Context, current: ActiveTimerState, remote: RemoteTimer) {
        TimerStateStore.saveActiveTimer(
            context = context,
            category = remote.category,
            startTime = current.startTime,
            goalMillis = current.goalMillis,
            linkedTodoId = current.linkedTodoId,
            linkedTodoCalendarSourceId = current.linkedTodoCalendarSourceId,
            linkedPetiteId = current.linkedPetiteId,
            linkedTodoTitle = current.linkedTodoTitle,
            pendingNote = current.pendingNote,
            pendingTitle = remote.title.ifBlank { null },
            dailyCueId = current.dailyCueId,
            dailyCueTargetDateKey = current.dailyCueTargetDateKey,
            sourceType = current.sourceType,
            sourceId = current.sourceId,
            routineGoalMillis = current.routineGoalMillis,
            publish = false
        )
        FlowStatusWidgetProvider.updateAll(context)
        if (remote.category != current.category) {
            ActivityTimerNotifier(context).apply {
                clearRunningTimer()
                if (remote.category != "SLEEP") showRunningTimer(remote.category, current.startTime)
            }
        }
    }

    /** Ends the phone's timer without saving a record (the web saved it, or replaced it). */
    private fun endLocalWithoutRecord(context: Context) {
        runCatching { RoutineGoalAlarmScheduler(context).cancel() }
        TimerStateStore.clearActiveTimer(context, publish = false)
        ActivityTimerNotifier(context).clearRunningTimer()
        FlowStatusWidgetProvider.updateAll(context)
    }

    private fun republish(uid: String, context: Context, local: ActiveTimerState?, remote: RemoteTimer?) {
        val fields = when {
            local != null -> TimerSyncPolicy.documentFields(toLocal(local), stopped = false, deviceId(context))
            // The server still shows a timer this phone no longer runs: mark that one stopped.
            remote != null && remote.status != TimerSyncPolicy.STATUS_STOPPED -> TimerSyncPolicy.documentFields(
                LocalTimer(remote.category, remote.title, remote.startTime, linkedTodoId = remote.linkedTodoId),
                stopped = true,
                deviceId(context)
            )
            else -> return
        }
        write(uid, fields)
    }

    private fun write(uid: String, fields: Map<String, Any?>) {
        timerDocument(uid)
            .set(fields + ("updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "timer write failed: ${it.javaClass.simpleName}") }
    }

    private fun timerDocument(uid: String) =
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("flowlog").document("timer")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
