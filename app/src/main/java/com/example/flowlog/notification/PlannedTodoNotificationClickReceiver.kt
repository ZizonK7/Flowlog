package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.flowlog.MainActivity
import com.example.flowlog.data.local.db.FlowlogDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlannedTodoNotificationClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val dao = FlowlogDatabase.getInstance(context).dailyGoalDao()
                dao.markNotificationClicked(itemId = itemId, clickedAt = now)
                Log.i(TAG, "clicked: itemId=$itemId clickedAt=$now")

                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recording notification click", e)
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
        const val EXTRA_ITEM_ID = "com.example.flowlog.extra.CLICK_ITEM_ID"
        const val EXTRA_NOTIFICATION_ID = "com.example.flowlog.extra.CLICK_NOTIFICATION_ID"
        private const val TAG = "PlannedTodoClick"
    }
}
