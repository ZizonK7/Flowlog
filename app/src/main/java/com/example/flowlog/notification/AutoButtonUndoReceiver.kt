package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventSource
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.EventLogRepository
import com.example.flowlog.data.repository.EventMetadata
import com.example.flowlog.data.repository.TodoRepository
import com.example.flowlog.widget.FlowStatusWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoButtonUndoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val snapshotId = intent.getStringExtra(EXTRA_SNAPSHOT_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                undo(context.applicationContext, snapshotId)
            } catch (e: Exception) {
                Log.e(TAG, "Auto button undo failed: snapshotId=$snapshotId", e)
                ActivityTimerNotifier(context.applicationContext).showAutoButtonUndoUnavailable()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun undo(context: Context, snapshotId: String) {
        val db = FlowlogDatabase.getInstance(context)
        val dao = db.autoButtonScheduleDao()
        val snapshot = dao.getUndoSnapshot(snapshotId)
        val now = System.currentTimeMillis()
        if (snapshot == null || snapshot.isUsed || snapshot.expiresAt < now) {
            ActivityTimerNotifier(context).showAutoButtonUndoUnavailable()
            return
        }

        val active = TimerStateStore.getActiveTimer(context)
            ?: TimerStateStore.getPinnedTimer(context)
        if (active?.sourceType != ActivitySourceType.AUTO_BUTTON ||
            active.sourceId != snapshot.scheduleId
        ) {
            ActivityTimerNotifier(context).showAutoButtonUndoUnavailable()
            return
        }

        val previousCategory = snapshot.previousActivityCategory
        val previousStartTime = snapshot.previousActivityStartTime
        if (previousCategory.isNullOrBlank() || previousStartTime == null) {
            ActivityTimerNotifier(context).showAutoButtonUndoUnavailable()
            return
        }

        dao.markUndoSnapshotUsed(snapshotId)
        val activityRepository = ActivityRepository(context)
        val previousSavedActivity = snapshot.previousActivityId
            ?.toLongOrNull()
            ?.let { previousId -> runCatching { activityRepository.getActivityById(previousId) }.getOrNull() }

        previousSavedActivity?.let { activity ->
            runCatching { activityRepository.deleteActivityById(activity.id) }
            activity.linkedTodoId?.let { todoId ->
                runCatching {
                    TodoRepository(context).addAccumulatedSeconds(todoId, -activity.durationMillis / 1000L)
                }
            }
        }

        val restoreCategory = previousSavedActivity?.category ?: previousCategory
        val restoreTitle = previousSavedActivity?.title
            ?: snapshot.previousLinkedTodoTitle
            ?: snapshot.previousActivityTitle
        val restoreGoalMillis = snapshot.previousGoalMillis ?: TimerStateStore.DEFAULT_GOAL_MILLIS
        val restoreLinkedTodoId = previousSavedActivity?.linkedTodoId ?: snapshot.previousLinkedTodoId
        val restoreSourceType = previousSavedActivity?.sourceType
            ?: snapshot.previousSourceType
            ?: ActivitySourceType.MANUAL
        val restoreSourceId = previousSavedActivity?.sourceId ?: snapshot.previousSourceId

        val discardedElapsedMs = active.elapsedMillis
        if (active.category == "SCHOOL" || active.category == "COMPANY") {
            TimerStateStore.clearPinnedTimer(context)
        } else {
            TimerStateStore.clearActiveTimer(context)
        }
        // The auto-started timer ends here without a record (the previous timer comes back).
        runCatching {
            EventLogRepository(context).log(
                eventType = EventType.ACTIVITY_STOPPED,
                entityType = EntityType.ACTIVITY,
                source = EventSource.NOTIFICATION,
                metadataJson = EventMetadata.activityStopped(
                    reason = "auto_button_undo",
                    category = active.category,
                    elapsedMs = discardedElapsedMs
                )
            )
        }
        TimerStateStore.saveActiveTimer(
            context = context,
            category = restoreCategory,
            startTime = previousStartTime,
            goalMillis = restoreGoalMillis,
            linkedTodoId = restoreLinkedTodoId,
            linkedTodoTitle = restoreTitle,
            sourceType = restoreSourceType,
            sourceId = restoreSourceId
        )
        // Not a new start: the timer the auto button replaced comes back with its old start time.
        runCatching {
            EventLogRepository(context).log(
                eventType = EventType.ACTIVITY_STARTED,
                entityType = EntityType.ACTIVITY,
                source = EventSource.NOTIFICATION,
                metadataJson = EventMetadata.activityStarted(
                    via = EventMetadata.StartVia.AUTO_BUTTON_UNDO_RESTORE,
                    category = restoreCategory,
                    startTime = previousStartTime,
                    sourceType = restoreSourceType,
                    sourceId = restoreSourceId,
                    linkedTodoId = restoreLinkedTodoId,
                    pendingTitle = restoreTitle
                )
            )
        }
        FlowStatusWidgetProvider.updateAll(context)
        ActivityTimerNotifier(context).apply {
            clearRunningTimer()
            showRunningTimer(restoreCategory, previousStartTime)
            showAutoButtonUndoRestored(
                restoreTitle ?: restoreCategory,
                restoreCategory
            )
        }
    }

    companion object {
        const val EXTRA_SNAPSHOT_ID = "com.example.flowlog.extra.AUTO_BUTTON_UNDO_SNAPSHOT_ID"
        private const val TAG = "AutoButtonUndo"
    }
}
