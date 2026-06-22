package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.TimerStatus
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.ActivityEntity
import com.example.flowlog.widget.FlowStatusWidgetProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class StudyPlanAutoStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L)
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    StudyPlanAutoStartScheduler.ACTION_START -> handleStart(context.applicationContext, itemId, scheduledAt)
                    StudyPlanAutoStartScheduler.ACTION_END -> handleEnd(context.applicationContext, itemId, scheduledAt)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Study plan auto start failed: action=$action itemId=$itemId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleStart(context: Context, itemId: String, scheduledAt: Long) {
        val db = FlowlogDatabase.getInstance(context)
        val item = db.organizedPetiteDao().getById(itemId) ?: return
        if (item.isDismissed || item.isCompleted) return
        val plannedStart = item.dateMillis ?: return
        if (scheduledAt != 0L && scheduledAt != plannedStart) return
        if (System.currentTimeMillis() - plannedStart > MAX_PLANNED_ALARM_LAG_MILLIS) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val dayStart = dayStart(plannedStart)
        val alreadyStarted = db.activityDao().countActivitiesBySourceForDate(
            userId = userId,
            sourceType = ActivitySourceType.STUDY_PLAN,
            sourceId = item.id,
            startOfDay = dayStart,
            endOfDay = dayStart + ONE_DAY_MILLIS
        ) > 0
        if (alreadyStarted) return

        val active = TimerStateStore.getActiveTimer(context)
        if (active?.status == TimerStatus.RUNNING) return

        val startAt = plannedStart.coerceAtMost(System.currentTimeMillis())
        val category = item.routineTimerCategory ?: item.activityCategory ?: "STUDY"
        TimerStateStore.saveActiveTimer(
            context = context,
            category = category,
            startTime = startAt,
            goalMillis = item.routineTimerDurationMillis ?: TimerStateStore.DEFAULT_GOAL_MILLIS,
            pendingTitle = item.title,
            sourceType = ActivitySourceType.STUDY_PLAN,
            sourceId = item.id
        )
        FlowStatusWidgetProvider.updateAll(context)
        ActivityTimerNotifier(context).apply {
            clearRunningTimer()
            showRunningTimer(category, startAt)
        }
    }

    private suspend fun handleEnd(context: Context, itemId: String, scheduledAt: Long) {
        val active = TimerStateStore.getActiveTimer(context) ?: return
        if (active.sourceType != ActivitySourceType.STUDY_PLAN || active.sourceId != itemId) return

        val db = FlowlogDatabase.getInstance(context)
        val item = db.organizedPetiteDao().getById(itemId) ?: return
        val plannedEnd = scheduledAt.takeIf { it > 0L }
            ?: (item.dateMillis?.plus(item.routineTimerDurationMillis ?: 0L) ?: System.currentTimeMillis())
        val endAt = plannedEnd
            .coerceAtLeast(active.startTime + 1L)
            .coerceAtMost(System.currentTimeMillis())
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        db.activityDao().insertActivity(
            ActivityEntity(
                activityId = UUID.randomUUID().toString(),
                userId = userId,
                title = item.title,
                category = active.category,
                note = null,
                startTime = active.startTime,
                endTime = endAt,
                durationMillis = (endAt - active.startTime).coerceAtLeast(1L),
                isFavorite = false,
                linkedTodoId = null,
                legacyId = null,
                legacyLinkedTodoId = null,
                tagsJson = null,
                exerciseSetsJson = null,
                sourceType = ActivitySourceType.STUDY_PLAN,
                sourceId = item.id,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                deletedAt = null,
                isDeleted = false,
                syncStatus = SyncStatus.PENDING
            )
        )
        TimerStateStore.clearActiveTimer(context)
        FlowStatusWidgetProvider.updateAll(context)
        ActivityTimerNotifier(context).clearRunningTimer()
    }

    private fun dayStart(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        const val EXTRA_ITEM_ID = "com.example.flowlog.extra.STUDY_PLAN_ITEM_ID"
        const val EXTRA_SCHEDULED_AT = "com.example.flowlog.extra.STUDY_PLAN_SCHEDULED_AT"
        private const val TAG = "StudyPlanAutoStart"
        private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
        private const val MAX_PLANNED_ALARM_LAG_MILLIS = 10L * 60L * 1000L
    }
}
