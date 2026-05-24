package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.local.ActivityLocalDataSource
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.remote.FirestoreSyncRepository
import kotlinx.coroutines.flow.Flow

class ActivityRepository(context: Context) {
    private val localDataSource = ActivityLocalDataSource(context)
    private val syncRepository = FirestoreSyncRepository()

    fun getAllActivities(): Flow<List<ActivitySession>> {
        return localDataSource.getAllActivities()
    }

    fun getTodayActivities(timestamp: Long): Flow<List<ActivitySession>> {
        return localDataSource.getTodayActivities(timestamp)
    }

    suspend fun insertActivity(activity: ActivitySession): Long {
        val id = localDataSource.insert(activity)
        runCatching {
            syncRepository.syncActivity(activity.copy(id = id))
        }
        return id
    }

    suspend fun updateActivity(activity: ActivitySession) {
        localDataSource.update(activity)
        runCatching {
            syncRepository.syncActivity(activity)
        }
    }

    suspend fun deleteActivity(activity: ActivitySession) {
        localDataSource.delete(activity)
        runCatching {
            syncRepository.deleteActivity(activity.id)
        }
    }

    suspend fun deleteActivityById(id: Long) {
        localDataSource.deleteById(id)
        runCatching {
            syncRepository.deleteActivity(id)
        }
    }

    suspend fun getActivityById(id: Long): ActivitySession? {
        return localDataSource.getActivityById(id)
    }

    suspend fun searchActivities(query: String): List<ActivitySession> {
        return localDataSource.searchActivities(query)
    }

    suspend fun filterByCategory(category: String): List<ActivitySession> {
        return localDataSource.filterByCategory(category)
    }

    suspend fun filterByTag(tag: String): List<ActivitySession> {
        return localDataSource.filterByTag(tag)
    }
}
