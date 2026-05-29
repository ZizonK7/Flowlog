package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flowlog.data.constants.EventSource
import com.example.flowlog.data.constants.SyncStatus

@Entity(
    tableName = "event_logs",
    indices = [
        Index("userId"),
        Index("timestamp"),
        Index("syncStatus"),
        Index("eventType"),
        Index(value = ["userId", "timestamp"])
    ]
)
data class EventLogEntity(
    @PrimaryKey val eventId: String,
    val userId: String,
    val installationId: String? = null,
    val eventType: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = EventSource.APP,
    val metadataJson: String? = null,
    val appVersion: String? = null,
    val algorithmVersion: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
