package com.example.flowlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flowlog.data.constants.SyncStatus

/**
 * Data read from elsewhere on the phone (v25). Health Connect now; usage statistics later.
 * Kept apart from activities: nothing here becomes an activity or feeds a recommendation.
 * sampleId = ExternalSamples.sampleId(...), so a re-read sample overwrites its own row.
 * Uploaded to users/{uid}/flowlog/data/externalSamples/{sampleId}.
 */
@Entity(
    tableName = "external_samples",
    indices = [
        Index("userId"),
        Index(value = ["userId", "provider", "kind", "startTime"]),
        Index("syncStatus")
    ]
)
data class ExternalSampleEntity(
    @PrimaryKey val sampleId: String,
    val userId: String,
    val provider: String,          // ExternalSampleProvider
    val kind: String,              // ExternalSampleKind
    val startTime: Long,
    val endTime: Long,
    val value: Double,             // steps = count, sessions = minutes, usage = millis
    val unit: String,              // ExternalSampleUnit
    val sourcePackage: String? = null,
    val payloadJson: String? = null,
    val fetchedAt: Long,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "'PENDING'")
    val syncStatus: String = SyncStatus.PENDING
)
