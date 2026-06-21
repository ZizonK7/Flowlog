package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.flowlog.MainActivity
import com.example.flowlog.data.constants.EventSource
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.local.InactivityReminderStore
import com.example.flowlog.data.repository.EventLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class InactivityReminderClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return
        val notificationIntId = intent.getIntExtra(EXTRA_NOTIFICATION_INT_ID, -1)
        val clickedAt = System.currentTimeMillis()
        InactivityReminderStore.markClicked(context, notificationId, clickedAt)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                EventLogRepository(context).log(
                    eventType = EventType.NOTIFICATION_CLICKED,
                    source = EventSource.NOTIFICATION,
                    metadataJson = JSONObject()
                        .put("type", InactivityReminderReceiver.TYPE)
                        .put("notificationId", notificationId)
                        .put("clickedAt", clickedAt)
                        .toString()
                )
                if (notificationIntId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationIntId)
                }
                Log.i(TAG, "clicked: notificationId=$notificationId")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording inactivity reminder click", e)
            } finally {
                pendingResult.finish()
            }
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_SCREEN, MainActivity.SCREEN_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(mainIntent)
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "com.example.flowlog.extra.INACTIVITY_NOTIFICATION_ID"
        const val EXTRA_NOTIFICATION_INT_ID = "com.example.flowlog.extra.INACTIVITY_NOTIFICATION_INT_ID"
        private const val TAG = "InactivityClick"
    }
}
