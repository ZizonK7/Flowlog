package com.example.flowlog.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class RoutineGoalAlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(cueId: Long, title: String, category: String, startedAtMillis: Long, goalMillis: Long) {
        if (cueId <= 0L || startedAtMillis <= 0L || goalMillis <= 0L) return
        val triggerAtMillis = startedAtMillis + goalMillis
        val pendingIntent = buildPendingIntent(
            flags = PendingIntent.FLAG_UPDATE_CURRENT,
            cueId = cueId,
            title = title,
            category = category,
            startedAtMillis = startedAtMillis
        ) ?: return
        scheduleAlarm(triggerAtMillis, pendingIntent)
    }

    fun cancel() {
        val pendingIntent = buildPendingIntent(
            flags = PendingIntent.FLAG_NO_CREATE,
            cueId = 0L,
            title = "",
            category = "",
            startedAtMillis = 0L
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(
        flags: Int,
        cueId: Long,
        title: String,
        category: String,
        startedAtMillis: Long
    ): PendingIntent? {
        val intent = Intent(appContext, RoutineGoalAlarmReceiver::class.java).apply {
            putExtra(RoutineGoalAlarmReceiver.EXTRA_CUE_ID, cueId)
            putExtra(RoutineGoalAlarmReceiver.EXTRA_TITLE, title)
            putExtra(RoutineGoalAlarmReceiver.EXTRA_CATEGORY, category)
            putExtra(RoutineGoalAlarmReceiver.EXTRA_STARTED_AT, startedAtMillis)
        }
        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                throw SecurityException("Exact alarms not allowed")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }.recoverCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }.recoverCatching {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    companion object {
        private const val REQUEST_CODE = 44_000
    }
}
