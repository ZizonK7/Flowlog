package com.example.flowlog.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.flowlog.data.local.InactivityReminderStore
import java.util.Calendar

class InactivityReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAfterActivityEnded(endedAtMillis: Long) {
        InactivityReminderStore.saveLastActivityEndedAt(appContext, endedAtMillis)
        if (!InactivityReminderStore.isEnabled(appContext)) {
            cancel()
            return
        }
        scheduleAt(nextAllowedTriggerAt(endedAtMillis + DELAY_AFTER_ACTIVITY_END_MILLIS))
    }

    fun rescheduleFromLastActivityIfNeeded() {
        if (!InactivityReminderStore.isEnabled(appContext)) {
            cancel()
            return
        }
        val lastEndedAt = InactivityReminderStore.getLastActivityEndedAt(appContext)
        if (lastEndedAt <= 0L) return
        val candidate = lastEndedAt + DELAY_AFTER_ACTIVITY_END_MILLIS
        val base = candidate.coerceAtLeast(System.currentTimeMillis() + MIN_RECHECK_DELAY_MILLIS)
        scheduleAt(nextAllowedTriggerAt(base))
    }

    fun cancel() {
        val pendingIntent = pendingIntent(PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun scheduleAt(triggerAtMillis: Long) {
        val pendingIntent = pendingIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }.onFailure {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun pendingIntent(flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            Intent(appContext, InactivityReminderReceiver::class.java),
            flags or PendingIntent.FLAG_IMMUTABLE
        )

    private fun nextAllowedTriggerAt(candidateMillis: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = candidateMillis }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when {
            hour < START_HOUR -> {
                calendar.apply {
                    set(Calendar.HOUR_OF_DAY, START_HOUR)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            hour >= END_HOUR -> {
                calendar.apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, START_HOUR)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            else -> candidateMillis
        }
    }

    companion object {
        private const val REQUEST_CODE = 62031
        const val DELAY_AFTER_ACTIVITY_END_MILLIS = 100L * 60L * 1000L
        private const val MIN_RECHECK_DELAY_MILLIS = 60L * 1000L
        private const val START_HOUR = 8
        private const val END_HOUR = 22
    }
}
