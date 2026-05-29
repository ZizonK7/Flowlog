package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flowlog.data.constants.SyncStatus

@Entity(
    tableName = "notification_logs",
    indices = [
        Index("userId"),
        Index("syncStatus")
    ]
)
data class NotificationLogEntity(
    @PrimaryKey val notificationId: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String? = null,
    val scheduledAt: Long? = null,
    val shownAt: Long? = null,
    val clickedAt: Long? = null,
    val dismissedAt: Long? = null,
    val relatedEntityType: String? = null,
    val relatedEntityId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.PENDING
)
