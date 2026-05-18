package com.example.flowlog.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.flowlog.MainActivity
import com.example.flowlog.R

class BrushAlarmService : Service() {
    private var ringtone: Ringtone? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel(this)
        startForeground(NOTIFICATION_ID, buildNotification())
        startAlarm()
        return START_STICKY
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAlarm() {
        if (ringtone?.isPlaying == true) return

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            play()
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
        ringtone = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_timer_notification)
        .setContentTitle("\uC591\uCE58 3\uBD84 \uC54C\uB78C")
        .setContentText("\uB04C \uB54C\uAE4C\uC9C0 \uC54C\uB78C\uC774 \uC6B8\uB824\uC694.")
        .setContentIntent(openAppPendingIntent())
        .setFullScreenIntent(alarmScreenPendingIntent(), true)
        .addAction(
            R.drawable.ic_timer_notification,
            "\uB044\uAE30",
            stopPendingIntent()
        )
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    private fun alarmScreenPendingIntent(): PendingIntent {
        val intent = Intent(this, BrushAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_ALARM_SCREEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            REQUEST_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopPendingIntent(): PendingIntent {
        val intent = Intent(this, BrushAlarmService::class.java).apply {
            action = ACTION_STOP
        }
        return PendingIntent.getService(
            this,
            REQUEST_STOP_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_STOP = "com.example.flowlog.action.STOP_BRUSH_ALARM"
        const val CHANNEL_ID = "flowlog_brush_alarm"
        private const val NOTIFICATION_ID = 4001
        private const val REQUEST_OPEN_APP = 4002
        private const val REQUEST_STOP_ALARM = 4003
        private const val REQUEST_ALARM_SCREEN = 4004

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "\uC591\uCE58 \uC54C\uB78C",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "\uC591\uCE58 3\uBD84 \uC54C\uB78C"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
