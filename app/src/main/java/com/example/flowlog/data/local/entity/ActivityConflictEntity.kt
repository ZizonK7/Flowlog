package com.example.flowlog.data.local.entity

import androidx.room.Entity

@Entity(tableName = "activity_conflicts", primaryKeys = ["userId", "activityId"])
data class ActivityConflictEntity(
    val userId: String,
    val activityId: String,
    val remoteRevision: Long,
    val localUpdatedAt: Long,
    val localDescription: String,
    val remoteDescription: String,
    val reason: String
)
