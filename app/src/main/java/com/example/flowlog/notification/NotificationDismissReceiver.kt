package com.example.flowlog.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventSource
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.repository.EventLogRepository
import com.example.flowlog.data.repository.EventMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Records NOTIFICATION_DISMISSED when a reminder notification is cleared without being
 * tapped. Attached with `setDeleteIntent` by [deleteIntent].
 *
 * The system sends the delete intent when the user swipes the notification away or
 * clears all; it is not sent for a tap (auto-cancel) or the app's own cancel(). A
 * notification built with `setTimeoutAfter` may also arrive here when it times out
 * (not checked on a device), so `shownAt` is kept: `dismissedAt - shownAt` close to
 * the timeout means the system removed it.
 */
class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE) ?: return
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return
        val shownAt = intent.getLongExtra(EXTRA_SHOWN_AT, 0L).takeIf { it > 0L }
        val entityId = intent.getStringExtra(EXTRA_ENTITY_ID)
        val dismissedAt = System.currentTimeMillis()

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                EventLogRepository(context).log(
                    eventType = EventType.NOTIFICATION_DISMISSED,
                    entityType = EntityType.NOTIFICATION,
                    entityId = entityId,
                    source = EventSource.NOTIFICATION,
                    metadataJson = EventMetadata.notificationDismissed(
                        type = type,
                        notificationId = notificationId,
                        dismissedAt = dismissedAt,
                        shownAt = shownAt
                    )
                )
                Log.i(TAG, "dismissed: type=$type notificationId=$notificationId")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording notification dismiss", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NotificationDismiss"
        private const val ACTION_DISMISSED = "com.example.flowlog.action.NOTIFICATION_DISMISSED"
        private const val EXTRA_TYPE = "com.example.flowlog.extra.DISMISS_TYPE"
        private const val EXTRA_NOTIFICATION_ID = "com.example.flowlog.extra.DISMISS_NOTIFICATION_ID"
        private const val EXTRA_SHOWN_AT = "com.example.flowlog.extra.DISMISS_SHOWN_AT"
        private const val EXTRA_ENTITY_ID = "com.example.flowlog.extra.DISMISS_ENTITY_ID"

        /**
         * Delete intent for one shown notification. The data URI carries [type] and
         * [notificationIntId], so two notifications never share (and overwrite) one
         * PendingIntent even when their request codes collide.
         */
        fun deleteIntent(
            context: Context,
            type: String,
            notificationId: String,
            notificationIntId: Int,
            shownAt: Long,
            entityId: String? = null
        ): PendingIntent {
            val intent = Intent(context, NotificationDismissReceiver::class.java).apply {
                action = ACTION_DISMISSED
                data = Uri.parse("flowlog-notification://dismiss/$type/$notificationIntId")
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_SHOWN_AT, shownAt)
                entityId?.let { putExtra(EXTRA_ENTITY_ID, it) }
            }
            return PendingIntent.getBroadcast(
                context,
                notificationIntId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
