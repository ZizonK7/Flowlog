package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.TimerStatus
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.AutoButtonScheduleEntity
import com.example.flowlog.data.local.entity.AutoButtonUndoSnapshotEntity
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.TodoRepository
import com.example.flowlog.ui.component.displayCategory
import com.example.flowlog.widget.FlowStatusWidgetProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class AutoButtonAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: return
        val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_START -> handleStart(context.applicationContext, scheduleId, scheduledAt)
                    ACTION_END -> handleEnd(context.applicationContext, scheduleId, scheduledAt)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto button alarm failed: action=$action scheduleId=$scheduleId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleStart(context: Context, scheduleId: String, scheduledAt: Long) {
        val db = FlowlogDatabase.getInstance(context)
        val schedule = db.autoButtonScheduleDao().getSchedule(scheduleId) ?: return
        if (!schedule.isEnabled || schedule.isDeleted) return

        val dateKey = AutoButtonScheduler.dateKey(scheduledAt.takeIf { it > 0L } ?: System.currentTimeMillis())
        if (db.autoButtonScheduleDao().getSkipDate(scheduleId, dateKey) != null) {
            AutoButtonScheduler(context).reschedule(scheduleId)
            return
        }
        if (!schedule.repeatsOn(dateKey)) {
            AutoButtonScheduler(context).reschedule(scheduleId)
            return
        }
        if (hasAlreadyStarted(context, scheduleId, dateKey)) {
            AutoButtonScheduler(context).reschedule(scheduleId)
            return
        }

        val now = System.currentTimeMillis()
        val plannedStart = scheduledAt.takeIf { it > 0L } ?: now
        if (now - plannedStart > MAX_PLANNED_ALARM_LAG_MILLIS) {
            AutoButtonScheduler(context).reschedule(scheduleId)
            return
        }
        val startAt = plannedStart.coerceAtMost(now)
        val previous = TimerStateStore.getActiveTimer(context)
        var previousTitle: String? = null
        var undoSnapshotId: String? = null
        if (previous != null && previous.status == TimerStatus.RUNNING) {
            previousTitle = displayCategory(previous.category)
            val endAt = startAt.coerceAtLeast(previous.startTime + 1L)
            val previousSavedActivityId = saveCompletedActivity(
                context = context,
                category = previous.category,
                title = previous.linkedTodoTitle ?: displayCategory(previous.category),
                startTime = previous.startTime,
                endTime = endAt,
                linkedTodoId = previous.linkedTodoId,
                sourceType = previous.sourceType,
                sourceId = previous.sourceId
            )
            undoSnapshotId = saveUndoSnapshot(
                context = context,
                scheduleId = scheduleId,
                autoActivityId = activeAutoActivityId(scheduleId, dateKey),
                previousActivityId = previousSavedActivityId.toString(),
                previousActivityTitle = previous.linkedTodoTitle ?: displayCategory(previous.category),
                previousActivityCategory = previous.category,
                previousActivityStartTime = previous.startTime,
                previousActivityEndTimeBeforeAuto = endAt,
                previousGoalMillis = previous.goalMillis,
                previousLinkedTodoId = previous.linkedTodoId,
                previousLinkedTodoTitle = previous.linkedTodoTitle,
                previousSourceType = previous.sourceType,
                previousSourceId = previous.sourceId,
                triggeredAt = now
            )
        }

        TimerStateStore.saveActiveTimer(
            context = context,
            category = schedule.category,
            startTime = startAt,
            goalMillis = (schedule.endMinuteOfDay - schedule.startMinuteOfDay).coerceAtLeast(1) * 60_000L,
            sourceType = ActivitySourceType.AUTO_BUTTON,
            sourceId = schedule.scheduleId
        )
        markStarted(context, scheduleId, dateKey)
        FlowStatusWidgetProvider.updateAll(context)
        ActivityTimerNotifier(context).apply {
            clearRunningTimer()
            showRunningTimer(schedule.category, startAt)
            showAutoButtonStarted(schedule.title, schedule.category, previousTitle, undoSnapshotId)
        }
        AutoButtonScheduler(context).reschedule(scheduleId)
    }

    private suspend fun handleEnd(context: Context, scheduleId: String, scheduledAt: Long) {
        val db = FlowlogDatabase.getInstance(context)
        val schedule = db.autoButtonScheduleDao().getSchedule(scheduleId) ?: return
        if (schedule.isDeleted) return
        val dateKey = AutoButtonScheduler.dateKey(scheduledAt.takeIf { it > 0L } ?: System.currentTimeMillis())
        if (db.autoButtonScheduleDao().getSkipDate(scheduleId, dateKey) != null) {
            AutoButtonScheduler(context).reschedule(scheduleId)
            return
        }

        val active = TimerStateStore.getActiveTimer(context) ?: run {
            AutoButtonScheduler(context).reschedule(scheduleId)
            return
        }
        if (active.sourceType != ActivitySourceType.AUTO_BUTTON || active.sourceId != scheduleId) {
            AutoButtonScheduler(context).reschedule(scheduleId)
            return
        }

        val now = System.currentTimeMillis()
        val plannedEnd = scheduledAt.takeIf { it > 0L } ?: now
        val endAt = plannedEnd
            .coerceAtLeast(active.startTime + 1L)
            .coerceAtMost(now)
        saveCompletedActivity(
            context = context,
            category = active.category,
            title = schedule.title.ifBlank { displayCategory(active.category) },
            startTime = active.startTime,
            endTime = endAt.coerceAtLeast(active.startTime + 1L),
            linkedTodoId = active.linkedTodoId,
            sourceType = active.sourceType,
            sourceId = active.sourceId
        )
        TimerStateStore.clearActiveTimer(context)
        FlowStatusWidgetProvider.updateAll(context)
        ActivityTimerNotifier(context).apply {
            clearRunningTimer()
            showAutoButtonEnded(schedule.title, schedule.category)
        }
        AutoButtonScheduler(context).reschedule(scheduleId)
    }

    private suspend fun saveCompletedActivity(
        context: Context,
        category: String,
        title: String,
        startTime: Long,
        endTime: Long,
        linkedTodoId: Long?,
        sourceType: String,
        sourceId: String?
    ): Long {
        val duration = (endTime - startTime).coerceAtLeast(1L)
        val activity = ActivitySession(
            category = category,
            title = title,
            startTime = startTime,
            endTime = endTime,
            durationMillis = duration,
            linkedTodoId = linkedTodoId,
            sourceType = sourceType,
            sourceId = sourceId,
            modifiedTime = System.currentTimeMillis()
        )
        val savedId = ActivityRepository(context).insertActivity(activity)
        linkedTodoId?.let { todoId ->
            TodoRepository(context).addAccumulatedSeconds(todoId, duration / 1000L)
        }
        if (category == "MEAL") {
            runCatching {
                ReminderScheduler(context).scheduleToothbrushReminder(activity.copy(id = savedId))
            }
        }
        return savedId
    }

    private suspend fun saveUndoSnapshot(
        context: Context,
        scheduleId: String,
        autoActivityId: String,
        previousActivityId: String,
        previousActivityTitle: String,
        previousActivityCategory: String,
        previousActivityStartTime: Long,
        previousActivityEndTimeBeforeAuto: Long,
        previousGoalMillis: Long,
        previousLinkedTodoId: Long?,
        previousLinkedTodoTitle: String?,
        previousSourceType: String,
        previousSourceId: String?,
        triggeredAt: Long
    ): String {
        val dao = FlowlogDatabase.getInstance(context).autoButtonScheduleDao()
        val now = System.currentTimeMillis()
        dao.markExpiredUndoSnapshotsUsed(now)
        dao.markOpenUndoSnapshotsUsed()
        val snapshotId = UUID.randomUUID().toString()
        dao.insertUndoSnapshot(
            AutoButtonUndoSnapshotEntity(
                id = snapshotId,
                scheduleId = scheduleId,
                autoActivityId = autoActivityId,
                previousActivityId = previousActivityId,
                previousActivityTitle = previousActivityTitle,
                previousActivityCategory = previousActivityCategory,
                previousActivityStartTime = previousActivityStartTime,
                previousActivityEndTimeBeforeAuto = previousActivityEndTimeBeforeAuto,
                previousGoalMillis = previousGoalMillis,
                previousLinkedTodoId = previousLinkedTodoId,
                previousLinkedTodoTitle = previousLinkedTodoTitle,
                previousSourceType = previousSourceType,
                previousSourceId = previousSourceId,
                triggeredAt = triggeredAt,
                expiresAt = triggeredAt + UNDO_EXPIRES_MILLIS,
                isUsed = false
            )
        )
        return snapshotId
    }

    private suspend fun hasAlreadyStarted(context: Context, scheduleId: String, dateKey: Long): Boolean {
        val active = TimerStateStore.getActiveTimer(context)
        if (active?.sourceType == ActivitySourceType.AUTO_BUTTON &&
            active.sourceId == scheduleId &&
            active.startTime >= dateKey &&
            active.startTime < dateKey + DAY_MILLIS
        ) {
            return true
        }
        if (wasStarted(context, scheduleId, dateKey)) return true

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val count = FlowlogDatabase.getInstance(context).activityDao().countActivitiesBySourceForDate(
            userId = userId,
            sourceType = ActivitySourceType.AUTO_BUTTON,
            sourceId = scheduleId,
            startOfDay = dateKey,
            endOfDay = dateKey + DAY_MILLIS
        )
        return count > 0
    }

    private fun wasStarted(context: Context, scheduleId: String, dateKey: Long): Boolean {
        return context.getSharedPreferences(PREFS_AUTO_BUTTON_EXECUTION, Context.MODE_PRIVATE)
            .getBoolean(startedKey(scheduleId, dateKey), false)
    }

    private fun markStarted(context: Context, scheduleId: String, dateKey: Long) {
        val preferences = context.getSharedPreferences(PREFS_AUTO_BUTTON_EXECUTION, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        val cutoffDateKey = AutoButtonScheduler.dateKey(System.currentTimeMillis() - MARKER_RETENTION_MILLIS)
        preferences.all.keys.forEach { key ->
            val markerDateKey = key.substringAfterLast(':', missingDelimiterValue = "")
                .toLongOrNull()
            if (markerDateKey != null && markerDateKey < cutoffDateKey) {
                editor.remove(key)
            }
        }
        editor
            .putBoolean(startedKey(scheduleId, dateKey), true)
            .apply()
    }

    private fun startedKey(scheduleId: String, dateKey: Long): String = "$scheduleId:$dateKey"

    private fun activeAutoActivityId(scheduleId: String, dateKey: Long): String = "active:$scheduleId:$dateKey"

    private fun AutoButtonScheduleEntity.repeatsOn(dateKey: Long): Boolean {
        val dayOfWeek = Calendar.getInstance().apply { timeInMillis = dateKey }.get(Calendar.DAY_OF_WEEK)
        return repeatDaysMask and (1 shl dayOfWeek) != 0
    }

    companion object {
        const val ACTION_START = "com.example.flowlog.action.AUTO_BUTTON_START"
        const val ACTION_END = "com.example.flowlog.action.AUTO_BUTTON_END"
        const val EXTRA_SCHEDULE_ID = "com.example.flowlog.extra.AUTO_BUTTON_SCHEDULE_ID"
        const val EXTRA_SCHEDULED_AT = "com.example.flowlog.extra.AUTO_BUTTON_SCHEDULED_AT"
        private const val TAG = "AutoButtonAlarm"
        private const val PREFS_AUTO_BUTTON_EXECUTION = "auto_button_execution"
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        private const val MAX_PLANNED_ALARM_LAG_MILLIS = 2L * 60L * 60L * 1000L
        private const val MARKER_RETENTION_MILLIS = 14L * DAY_MILLIS
        private const val UNDO_EXPIRES_MILLIS = 2L * 60L * 60L * 1000L
    }
}
