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
import com.example.flowlog.MainActivity
import com.example.flowlog.R
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.model.TodoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class PlannedTodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = FlowlogDatabase.getInstance(context)
                val item = db.dailyGoalDao().getGoalItem(itemId) ?: return@launch
                if (item.userActionStatus !in ACTIVE_STATUSES ||
                    item.wasCompleted ||
                    item.wasSkipped ||
                    item.wasDeleted ||
                    item.actualStartedAt != null ||
                    item.actualCompletedAt != null
                ) {
                    return@launch
                }

                val expectedAt = item.notificationScheduledAtMillis ?: return@launch
                if (scheduledAt > 0L && scheduledAt != expectedAt) return@launch
                if (System.currentTimeMillis() - expectedAt > MAX_STALE_ALARM_MILLIS) return@launch

                val todoId = item.todoId.removePrefix("legacy_todo_").toLongOrNull()
                val todo = todoId?.let { db.todoDao().getTodoByLegacyId(it) }
                if (todo != null && (todo.isCompleted || todo.isDeleted)) return@launch

                if (!canPostNotifications(context)) return@launch
                val shouldSilence = SleepAlarmGuard.shouldSilenceAlerts(context)
                if (shouldSilence) {
                    SleepAlarmGuard.ensureSilentNotificationChannel(context)
                }

                val title = todo?.title ?: item.titleFromSnapshot() ?: "\uD560 \uC77C"
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    REQUEST_OPEN_HOME,
                    Intent(context, MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_OPEN_SCREEN, MainActivity.SCREEN_HOME)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val channelId = if (shouldSilence) {
                    SleepAlarmGuard.SILENT_CHANNEL_ID
                } else {
                    ToothbrushReminderReceiver.DING_CHANNEL_ID
                }
                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_timer_notification)
                    .setColor(NOTIFICATION_ICON_COLOR)
                    .setContentTitle("\uC9C0\uAE08 \uC2DC\uC791\uD560 \uD560 \uC77C")
                    .setContentText(title)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(title))
                    .setContentIntent(openPendingIntent)
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

                notifySafely(context, itemId.hashCode(), builder.build())
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
        private const val REQUEST_OPEN_HOME = 5201
        private const val MAX_STALE_ALARM_MILLIS = 30L * 60L * 1000L
        private const val TAG = "PlannedTodoReminder"
        private val ACTIVE_STATUSES = setOf("PLANNED", "RESCHEDULED")
        private val NOTIFICATION_ICON_COLOR = 0xFF4F5060.toInt()
        private val json = Json { ignoreUnknownKeys = true }
    }
}
