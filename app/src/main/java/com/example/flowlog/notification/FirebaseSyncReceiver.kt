package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.flowlog.data.sync.FirebaseSyncAlarmScheduler
import com.example.flowlog.data.sync.FirebaseSyncCoordinator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FirebaseSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        FirebaseSyncAlarmScheduler.scheduleNextMidnightSync(appContext)

        CoroutineScope(Dispatchers.IO + Job()).launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    FirebaseSyncCoordinator(appContext).syncEligibleWithTodayCalendar(userId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
