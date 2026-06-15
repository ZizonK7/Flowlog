package com.example.flowlog.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudyPlanAutoStartScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dao = FlowlogDatabase.getInstance(appContext).organizedPetiteDao()

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    suspend fun rescheduleAll() {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            dao.getActiveBySource(userId, PetiteSourceType.STUDY_PLAN.name)
                .filter { it.routineTimerCategory != null && it.dateMillis != null && it.routineTimerDurationMillis != null }
                .forEach { item ->
                    cancel(item.id)
                    val startAt = item.dateMillis ?: return@forEach
                    val endAt = startAt + (item.routineTimerDurationMillis ?: return@forEach)
                    if (startAt > now) scheduleAlarm(item.id, ACTION_START, startAt)
                    if (endAt > now) scheduleAlarm(item.id, ACTION_END, endAt)
                }
        }
    }

    fun cancel(itemId: String) {
        cancelPendingIntent(itemId, ACTION_START)
        cancelPendingIntent(itemId, ACTION_END)
    }

    private fun scheduleAlarm(itemId: String, action: String, triggerAt: Long) {
        val pendingIntent = buildPendingIntent(itemId, action, triggerAt, PendingIntent.FLAG_UPDATE_CURRENT)
            ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                throw SecurityException("Exact alarms are not allowed")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }.recoverCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }.recoverCatching {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancelPendingIntent(itemId: String, action: String) {
        val pendingIntent = buildPendingIntent(itemId, action, scheduledAt = 0L, flags = PendingIntent.FLAG_NO_CREATE)
            ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(
        itemId: String,
        action: String,
        scheduledAt: Long,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(appContext, StudyPlanAutoStartReceiver::class.java).apply {
            this.action = action
            putExtra(StudyPlanAutoStartReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(StudyPlanAutoStartReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(itemId, action),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(itemId: String, action: String): Int {
        val type = if (action == ACTION_START) 1 else 2
        return 43 * itemId.hashCode() + type
    }

    companion object {
        const val ACTION_START = "com.example.flowlog.action.STUDY_PLAN_START"
        const val ACTION_END = "com.example.flowlog.action.STUDY_PLAN_END"
    }
}
