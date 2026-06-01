package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.repository.ActivityRepository
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
        val restoreTitle = previousSavedActivity?.title ?: snapshot.previousActivityTitle
        val restoreSourceType = previousSavedActivity?.sourceType ?: ActivitySourceType.MANUAL
        val restoreSourceId = previousSavedActivity?.sourceId

        TimerStateStore.saveActiveTimer(
            context = context,
            category = restoreCategory,
            startTime = previousStartTime,
            linkedTodoId = previousSavedActivity?.linkedTodoId,
            linkedTodoTitle = restoreTitle,
            sourceType = restoreSourceType,
            sourceId = restoreSourceId
        )
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
