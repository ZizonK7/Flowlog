package com.example.flowlog.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Todo/Activity 삭제 직후 호출하는 진입점.
 *
 * 로컬 soft-delete(isDeleted=1, syncStatus=PENDING) 이후에는 그동안 앱을 열거나
 * 자정 알람이 돌거나 포그라운드에서 네트워크가 재연결될 때까지 Firestore hard delete가
 * 미뤄졌다(길게는 몇 주씩). 여기서는 (1) 온라인이면 즉시 동기화를 한번 시도하고,
 * (2) 그와 무관하게 네트워크 연결 시 재시도하는 WorkManager 잡을 항상 큐잉해서
 * 프로세스가 죽어도 살아남는 안전망을 둔다.
 */
object DeleteSyncTrigger {
    private const val UNIQUE_WORK_NAME = "flowlog-delete-sync-retry"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun trigger(context: Context) {
        val appContext = context.applicationContext
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        scope.launch {
            runCatching {
                val dataSource = FirebaseSyncDataSource(appContext)
                dataSource.syncPendingTodos(userId)
                dataSource.syncPendingActivities(userId)
            }
        }

        enqueueRetry(appContext)
    }

    private fun enqueueRetry(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<DeleteSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}
