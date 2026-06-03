package com.example.flowlog.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    suspend fun cancel(itemId: String) {
        val rc = requestCode(itemId)
        val item = withContext(Dispatchers.IO) { runCatching { dao.getGoalItem(itemId) }.getOrNull() }
        Log.d(TAG, "cancel: itemId=$itemId todoId=${item?.todoId} plannedStart=${item?.plannedStartMillis?.let { dateFormat.format(Date(it)) } ?: "null"} notifAt=${item?.notificationScheduledAtMillis?.let { dateFormat.format(Date(it)) } ?: "null"} userActionStatus=${item?.userActionStatus} requestCode=$rc")
        // FLAG_UPDATE_CURRENT: scheduleItem과 동일한 PendingIntent identity를 보장.
        // FLAG_NO_CREATE는 앱 재시작 후 메모리 캐시가 없으면 null을 반환해 cancel이 no-op이 되는 문제가 있다.
        val pendingIntent = buildPendingIntent(itemId, scheduledAt = 0L, flags = PendingIntent.FLAG_UPDATE_CURRENT)
        if (pendingIntent == null) {
            Log.w(TAG, "cancel: no-op — itemId=$itemId requestCode=$rc pendingIntent=null")
            return
        }
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        cancelDeliveredNotification(itemId, reason = "alarm_cancelled")
    }

    fun cancelDeliveredNotification(itemId: String, reason: String) {
        val notificationId = notificationIdFor(itemId)
        runCatching {
            NotificationManagerCompat.from(appContext).cancel(notificationId)
        }
        Log.d(TAG, "cancelDeliveredNotification: itemId=$itemId notificationId=$notificationId reason=$reason")
    }

    private fun scheduleItem(item: DailyGoalItemEntity, now: Long) {
        Log.i(TAG, "scheduleItem: itemId=${item.itemId} todoId=${item.todoId} plannedStart=${item.plannedStartMillis?.let { dateFormat.format(Date(it)) } ?: "null"} notifAt=${item.notificationScheduledAtMillis?.let { dateFormat.format(Date(it)) } ?: "null"} userActionStatus=${item.userActionStatus} wasCompleted=${item.wasCompleted} wasSkipped=${item.wasSkipped} wasDeleted=${item.wasDeleted}")
        val triggerAt = item.notificationScheduledAtMillis ?: run {
            Log.i(TAG, "skip: itemId=${item.itemId} todoId=${item.todoId} blockReason=notif_at_null")
            return
        }
        val blockReason = when {
            triggerAt <= now -> "trigger_in_past"
            item.userActionStatus !in ACTIVE_STATUSES -> "status_not_active"
            item.wasCompleted -> "completed"
            item.wasSkipped -> "skipped"
            item.wasDeleted -> "deleted"
            else -> null
        }
        if (blockReason != null) {
            Log.i(TAG, "skip: itemId=${item.itemId} todoId=${item.todoId} plannedStart=${item.plannedStartMillis?.let { dateFormat.format(Date(it)) } ?: "null"} notifAt=${dateFormat.format(Date(triggerAt))} userActionStatus=${item.userActionStatus} blockReason=$blockReason")
            return
        }

        val pendingIntent = buildPendingIntent(
            itemId = item.itemId,
            scheduledAt = triggerAt,
            flags = PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return

        Log.d(TAG, "scheduleItem ok: itemId=${item.itemId} todoId=${item.todoId} " +
            "plannedStart=${item.plannedStartMillis?.let { dateFormat.format(Date(it)) } ?: "null"} " +
            "notifAt=${dateFormat.format(Date(triggerAt))} " +
            "userActionStatus=${item.userActionStatus} " +
            "requestCode=${requestCode(item.itemId)}")
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
        private const val TAG = "PlannedTodoScheduler"
        private const val REQUEST_PLANNED_TODO_REMINDER = 42_000
        private val ACTIVE_STATUSES = setOf("PLANNED", "RESCHEDULED")
        private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun notificationIdFor(itemId: String): Int = itemId.hashCode()
    }
}
