package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.local.DailyCueCompletionStore
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.TimerStatus
import java.util.Calendar

class RoutineGoalAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cueId = intent.getLongExtra(EXTRA_CUE_ID, 0L)
        val startedAt = intent.getLongExtra(EXTRA_STARTED_AT, 0L)
        val category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val activeTimer = TimerStateStore.getActiveTimer(context) ?: return

        val isSameRoutine = activeTimer.status == TimerStatus.RUNNING &&
            activeTimer.sourceType == ActivitySourceType.DAILY_CUE_ROUTINE &&
            activeTimer.dailyCueId == cueId &&
            activeTimer.startTime == startedAt &&
            activeTimer.routineGoalMillis > 0L
        if (!isSameRoutine) return

        val targetDay = activeTimer.dailyCueTargetDateKey ?: startOfDay(activeTimer.startTime)
        if (targetDay == startOfDay(System.currentTimeMillis())) {
            DailyCueCompletionStore.markCompletedToday(context, cueId)
        } else {
            DailyCueCompletionStore.markCompletedForDate(context, cueId, targetDay)
        }
        ActivityTimerNotifier(context).showRoutineGoalAlert(
            title = title.ifBlank { activeTimer.pendingTitle.orEmpty() },
            category = category.ifBlank { activeTimer.category }
        )
    }

    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        const val EXTRA_CUE_ID = "extra_cue_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_STARTED_AT = "extra_started_at"
    }
}
