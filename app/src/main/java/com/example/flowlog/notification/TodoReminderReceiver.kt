package com.example.flowlog.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.flowlog.MainActivity
import com.example.flowlog.R
import com.example.flowlog.data.local.TodoLocalDataSource

class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, 0L)
        if (todoId == 0L) return

        val todo = TodoLocalDataSource.loadSnapshot(context)
            .firstOrNull { it.id == todoId && !it.isDone }
            ?: return

        TodoReminderScheduler(context).scheduleNextRandomReminder(todoId)
        if (!canPostNotifications(context)) return

        val openPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_TODO,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_SCREEN, MainActivity.SCREEN_TODO)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ToothbrushReminderReceiver.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle("할 일이 아직 남아 있어요")
            .setContentText(todo.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(todo.title))
            .setContentIntent(openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        NotificationManagerCompat.from(context).notify(todoId.toInt(), notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val EXTRA_TODO_ID = "com.example.flowlog.extra.TODO_ID"
        private const val REQUEST_OPEN_TODO = 5101
    }
}
