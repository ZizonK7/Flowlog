package com.example.flowlog.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.google.firebase.auth.FirebaseAuth

/**
 * [DeleteSyncTrigger]가 네트워크 재연결 시 재시도용으로 큐잉하는 워커.
 *
 * 프로세스가 종료돼도 WorkManager가 상태를 영속시키므로, 삭제 직후 오프라인이었거나
 * 즉시 시도가 실패한 경우에도 네트워크가 돌아오면(설령 그 사이 앱이 완전히 꺼져 있었어도)
 * 반드시 한 번 더 동기화가 시도된다.
 */
class DeleteSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        return runCatching {
            val dataSource = FirebaseSyncDataSource(applicationContext)
            dataSource.syncPendingTodos(userId)
            dataSource.syncPendingActivities(userId)

            val db = FlowlogDatabase.getInstance(applicationContext)
            val stillPending = db.todoDao().getUnsyncedTodos(userId).isNotEmpty() ||
                db.activityDao().getUnsyncedActivities(userId).isNotEmpty()
            check(!stillPending) { "sync pass left PENDING items behind" }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
