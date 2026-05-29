package com.example.flowlog.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.flowlog.MainActivity
import com.example.flowlog.R
import com.example.flowlog.data.local.db.FlowlogDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, 0L)
        if (todoId == 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // TODO(알림 기능 활성화 전 필수 개선):
                //   getTodoByLegacyId는 userId 필터 없이 조회한다.
                //   계정 전환 후에도 이전 계정의 Todo 알림이 표시될 수 있음.
                //   개선 방향: entity.userId 와 현재 FirebaseAuth.currentUser?.uid를
                //   비교해 일치하지 않으면 알림을 표시하지 않도록 추가 필요.
                val entity = runCatching {
                    FlowlogDatabase.getInstance(context).todoDao().getTodoByLegacyId(todoId)
                }.getOrNull()

                // 삭제됐거나 이미 완료된 Todo는 알림 표시 안 함
                if (entity == null || entity.isCompleted || entity.isDeleted) return@launch

                // 다음 랜덤 알림 예약
                TodoReminderScheduler(context).scheduleNextRandomReminder(todoId)

                if (!canPostNotifications(context)) return@launch

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
                    .setColor(NOTIFICATION_ICON_COLOR)
                    .setContentTitle("할 일이 아직 남아 있어요")
                    .setContentText(entity.title)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(entity.title))
                    .setContentIntent(openPendingIntent)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .build()

                NotificationManagerCompat.from(context).notify(todoId.toInt(), notification)
            } catch (e: Exception) {
                Log.e(TAG, "Error in TodoReminderReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
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
        private val NOTIFICATION_ICON_COLOR = 0xFF4F5060.toInt()
        private const val TAG = "TodoReminderReceiver"
    }
}
