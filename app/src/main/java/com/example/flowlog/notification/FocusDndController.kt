package com.example.flowlog.notification

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.flowlog.data.local.FocusModeStore

object FocusDndController {

    fun hasPolicyAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun openPolicyAccessSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun getCurrentFilter(context: Context): Int {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.currentInterruptionFilter
    }

    /**
     * Enables DND (PRIORITY mode). Saves the previous filter so it can be restored later.
     * If DND was already PRIORITY, marks [FocusModeStore.isDndChangedByFocus] as false so
     * restore is a no-op (we won't turn off something the user had on themselves).
     *
     * @return true if policy access was available and DND logic ran, false otherwise.
     */
    fun enableDnd(context: Context): Boolean {
        if (!hasPolicyAccess(context)) return false
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val currentFilter = nm.currentInterruptionFilter
        FocusModeStore.savePreviousDndFilter(context, currentFilter)
        if (currentFilter != NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
            runCatching { nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY) }
            FocusModeStore.setDndChangedByFocus(context, true)
        } else {
            // Already in DND — don't touch it, don't restore it on exit
            FocusModeStore.setDndChangedByFocus(context, false)
        }
        return true
    }

    /**
     * Restores the interruption filter to the value captured in [enableDnd].
     * Safe to call multiple times (idempotent: clears state on first call).
     * If permission was revoked between enable and restore, clears state silently.
     */
    fun restoreDnd(context: Context) {
        if (!FocusModeStore.isDndChangedByFocus(context)) {
            FocusModeStore.clearDndState(context)
            return
        }
        val previousFilter = FocusModeStore.getPreviousDndFilter(context) ?: run {
            FocusModeStore.clearDndState(context)
            return
        }
        runCatching {
            if (hasPolicyAccess(context)) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.setInterruptionFilter(previousFilter)
            }
        }
        FocusModeStore.clearDndState(context)
    }
}
