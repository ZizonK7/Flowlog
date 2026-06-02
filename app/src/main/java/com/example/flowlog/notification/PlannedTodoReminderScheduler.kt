package com.example.flowlog.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlannedTodoReminderScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dao = FlowlogDatabase.getInstance(appContext).dailyGoalDao()

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    suspend fun rescheduleAll() {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            dao.getUpcomingPlannedItemsForReminder(userId, now).forEach { item ->
                cancel(item.itemId)
                scheduleItem(item, now)
            }
        }
    }

    suspend fun reschedule(itemId: String) {
        withContext(Dispatchers.IO) {
            cancel(itemId)
            val item = dao.getGoalItem(itemId) ?: return@withContext
            scheduleItem(item, System.currentTimeMillis())
        }
    }

    fun cancel(itemId: String) {
        val pendingIntent = buildPendingIntent(itemId, scheduledAt = 0L, flags = PendingIntent.FLAG_NO_CREATE)
            ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun scheduleItem(item: DailyGoalItemEntity, now: Long) {
        val triggerAt = item.notificationScheduledAtMillis ?: return
        if (triggerAt <= now ||
            item.userActionStatus !in ACTIVE_STATUSES ||
            item.wasCompleted ||
            item.wasSkipped ||
            item.wasDeleted
        ) {
            return
        }

        val pendingIntent = buildPendingIntent(
            itemId = item.itemId,
            scheduledAt = triggerAt,
            flags = PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return

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

    private fun buildPendingIntent(itemId: String, scheduledAt: Long, flags: Int): PendingIntent? {
        val intent = Intent(appContext, PlannedTodoReminderReceiver::class.java).apply {
            putExtra(PlannedTodoReminderReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(PlannedTodoReminderReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(itemId),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(itemId: String): Int {
        return 41 * itemId.hashCode() + REQUEST_PLANNED_TODO_REMINDER
    }

    companion object {
        private const val REQUEST_PLANNED_TODO_REMINDER = 42_000
        private val ACTIVE_STATUSES = setOf("PLANNED", "RESCHEDULED")
    }
}
