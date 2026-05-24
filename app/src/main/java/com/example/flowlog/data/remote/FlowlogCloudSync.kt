package com.example.flowlog.data.remote

import android.content.Context
import com.example.flowlog.data.local.ActivityLocalDataSource
import com.example.flowlog.data.local.TodoLocalDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FlowlogCloudSync(
    private val context: Context,
    private val syncRepository: FirestoreSyncRepository = FirestoreSyncRepository()
) {
    suspend fun uploadLocalSnapshot() = withContext(Dispatchers.IO) {
        syncRepository.syncActivities(ActivityLocalDataSource.loadSnapshot(context))
        syncRepository.syncTodos(TodoLocalDataSource.loadSnapshot(context))
    }
}
