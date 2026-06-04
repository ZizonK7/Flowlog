package com.example.flowlog.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.flowlog.R
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.model.TodoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlannedTodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L)
        if (scheduledAt == 0L) {
            Log.w(TAG, "receive blocked: itemId=$itemId intentScheduledAt=0 reason=missing_intent_scheduled_at")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = FlowlogDatabase.getInstance(context)
                val item = db.dailyGoalDao().getGoalItem(itemId) ?: run {
                    Log.w(TAG, "receive blocked: itemId=$itemId intentScheduledAt=$scheduledAt reason=item_not_found")
                    return@launch
                }

                val snapshotTitle = item.titleFromSnapshot() ?: item.todoId

                if (item.userActionStatus !in ACTIVE_STATUSES) {
                    Log.w(TAG, "receive blocked: itemId=$itemId title=$snapshotTitle intentScheduledAt=$scheduledAt dbScheduledAt=${item.notificationScheduledAtMillis} actionStatus=${item.userActionStatus} reason=status_not_active")
                    return@launch
                }
                if (item.wasCompleted || item.actualCompletedAt != null) {
                    Log.w(TAG, "receive blocked: itemId=$itemId title=$snapshotTitle intentScheduledAt=$scheduledAt reason=completed")
                    return@launch
                }
                if (item.wasSkipped) {
                    Log.w(TAG, "receive blocked: itemId=$itemId title=$snapshotTitle intentScheduledAt=$scheduledAt reason=skipped")
                    return@launch
                }
                if (item.wasDeleted) {
                    Log.w(TAG, "receive blocked: itemId=$itemId title=$snapshotTitle intentScheduledAt=$scheduledAt reason=deleted")
                    return@launch
                }
                if (item.actualStartedAt != null) {
                    Log.w(TAG, "receive blocked: itemId=$itemId title=$snapshotTitle intentScheduledAt=$scheduledAt reason=already_started")
                    return@launch
                }

                val expectedAt = item.notificationScheduledAtMillis ?: run {
                    Log.w(TAG, "receive blocked: itemId=$itemId title=$snapshotTitle intentScheduledAt=${dateFormat.format(Date(scheduledAt))} reason=no_scheduled_at_in_db")
                    return@launch
                }
                if (scheduledAt != expectedAt) {
                    Log.w(TAG, "receive blocked: itemId=$itemId title=$snapshotTitle intentScheduledAt=${dateFormat.format(Date(scheduledAt))} dbScheduledAt=${dateFormat.format(Date(expectedAt))} actionStatus=${item.userActionStatus} reason=scheduled_at_mismatch")
                    return@launch
                }
                Log.d(TAG, "receive match: itemId=$itemId intentScheduledAt=${dateFormat.format(Date(scheduledAt))} dbScheduledAt=${dateFormat.format(Date(expectedAt))}")
                if (System.currentTimeMillis() - expectedAt > MAX_STALE_ALARM_MILLIS) {
                    Log.w(TAG, "receive blocked: itemId=$itemId title=$snapshotTitle intentScheduledAt=${dateFormat.format(Date(scheduledAt))} age=${System.currentTimeMillis() - expectedAt}ms reason=stale_alarm")
                    return@launch
                }

                val todoId = item.todoId.removePrefix("legacy_todo_").toLongOrNull()
                val todo = todoId?.let { db.todoDao().getTodoByLegacyId(it) }
                if (todo != null && (todo.isCompleted || todo.isDeleted)) {
                    Log.w(TAG, "receive blocked: itemId=$itemId title=$snapshotTitle intentScheduledAt=$scheduledAt reason=todo_completed_or_deleted")
                    return@launch
                }

                if (!canPostNotifications(context)) return@launch
                val shouldSilence = SleepAlarmGuard.shouldSilenceAlerts(context)
                if (shouldSilence) {
                    SleepAlarmGuard.ensureSilentNotificationChannel(context)
                }

                val title = todo?.title ?: item.titleFromSnapshot() ?: "\uD560 \uC77C"
                val notificationId = PlannedTodoReminderScheduler.notificationIdFor(itemId)

                // Final DB re-check to catch race conditions between initial checks and notify()
                val freshItem = db.dailyGoalDao().getGoalItem(itemId)
                val finalBlockReason = when {
                    freshItem == null -> "item_gone"
                    freshItem.userActionStatus !in ACTIVE_STATUSES -> "status_changed"
                    freshItem.wasCompleted || freshItem.actualCompletedAt != null -> "completed"
                    freshItem.wasSkipped -> "skipped"
                    freshItem.wasDeleted -> "deleted"
                    freshItem.actualStartedAt != null -> "started"
                    freshItem.notificationScheduledAtMillis != expectedAt -> "scheduled_at_changed"
                    else -> null
                }
                if (finalBlockReason != null) {
                    Log.w(TAG, "receive finalCheckBlocked: itemId=$itemId notificationId=$notificationId reason=$finalBlockReason")
                    return@launch
                }

                Log.i(TAG, "receive allowed: itemId=$itemId notificationId=$notificationId todoId=${item.todoId} plannedStart=${item.plannedStartMillis?.let { dateFormat.format(Date(it)) } ?: "null"} notifAt=${dateFormat.format(Date(expectedAt))} intentScheduledAt=${dateFormat.format(Date(scheduledAt))} userActionStatus=${item.userActionStatus}")

                val clickIntent = Intent(context, PlannedTodoNotificationClickReceiver::class.java).apply {
                    putExtra(PlannedTodoNotificationClickReceiver.EXTRA_ITEM_ID, itemId)
                    putExtra(PlannedTodoNotificationClickReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val clickPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val channelId = if (shouldSilence) {
                    SleepAlarmGuard.SILENT_CHANNEL_ID
                } else {
                    ToothbrushReminderReceiver.DING_CHANNEL_ID
                }
                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(NOTIFICATION_ICON_COLOR)
                    .setContentTitle("\uC9C0\uAE08 \uC2DC\uC791\uD560 \uD560 \uC77C")
                    .setContentText(title)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(title))
                    .setContentIntent(clickPendingIntent)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)

                if (shouldSilence) {
                    builder
                        .setSilent(true)
                        .setDefaults(0)
                        .setVibrate(null)
                        .setSound(null)
                } else {
                    builder
                        .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                        .setSound(KakaoStyleAlertPlayer.soundUri(context))
                        .setVibrate(longArrayOf(0L, 400L, 200L, 400L))
                }

                notifySafely(context, notificationId, builder.build())

                val deliveredAt = System.currentTimeMillis()
                db.dailyGoalDao().markNotificationDelivered(itemId = itemId, deliveredAt = deliveredAt)
                Log.i(TAG, "delivered: itemId=$itemId deliveredAt=$deliveredAt")
            } catch (e: Exception) {
                Log.e(TAG, "Error in PlannedTodoReminderReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun com.example.flowlog.data.local.entity.DailyGoalItemEntity.titleFromSnapshot(): String? =
        todoSnapshotJson?.let { raw ->
            runCatching { json.decodeFromString<TodoItem>(raw).title }.getOrNull()
        }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(context: Context, notificationId: Int, notification: android.app.Notification) {
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "com.example.flowlog.extra.PLANNED_TODO_ITEM_ID"
        const val EXTRA_SCHEDULED_AT = "com.example.flowlog.extra.PLANNED_TODO_SCHEDULED_AT"
        private const val MAX_STALE_ALARM_MILLIS = 30L * 60L * 1000L
        private const val TAG = "PlannedTodoReminder"
        private val ACTIVE_STATUSES = setOf("PLANNED", "RESCHEDULED")
        private val NOTIFICATION_ICON_COLOR = 0xFF4F5060.toInt()
        private val json = Json { ignoreUnknownKeys = true }
        private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }
}
