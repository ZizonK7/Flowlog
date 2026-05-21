package com.example.flowlog.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import kotlin.random.Random

class TodoReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleInitialReminder(todoId: Long, createdAtMillis: Long) {
        val triggerAtMillis = createdAtMillis + INITIAL_DELAY_MILLIS
        scheduleReminder(todoId, triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + 1L))
    }

    fun scheduleNextRandomReminder(todoId: Long) {
        scheduleReminder(todoId, nextRandomDaytimeMillis())
    }

    fun cancelReminder(todoId: Long) {
        val pendingIntent = pendingIntent(todoId, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun scheduleReminder(todoId: Long, triggerAtMillis: Long) {
        val pendingIntent = pendingIntent(todoId, PendingIntent.FLAG_UPDATE_CURRENT) ?: return

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }.recoverCatching {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun pendingIntent(todoId: Long, flag: Int): PendingIntent? {
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            putExtra(TodoReminderReceiver.EXTRA_TODO_ID, todoId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(todoId),
            intent,
            flag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextRandomDaytimeMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, RANDOM_START_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = calendar.timeInMillis
        val randomOffset = Random.nextLong(RANDOM_WINDOW_MILLIS)
        return startMillis + randomOffset
    }

    private fun requestCode(todoId: Long): Int {
        return (REQUEST_TODO_REMINDER_BASE + (todoId % REQUEST_TODO_REMINDER_RANGE)).toInt()
    }

    companion object {
        const val INITIAL_DELAY_MILLIS = 24L * 60L * 60L * 1000L
        private const val RANDOM_START_HOUR = 9
        private const val RANDOM_WINDOW_MILLIS = 12L * 60L * 60L * 1000L
        private const val REQUEST_TODO_REMINDER_BASE = 10_000L
        private const val REQUEST_TODO_REMINDER_RANGE = 1_000_000L
    }
}
