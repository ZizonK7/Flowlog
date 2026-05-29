package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_batches",
    indices = [
        Index("userId"),
        Index("status"),
        Index(value = ["userId", "status"])
    ]
)
data class SyncBatchEntity(
    @PrimaryKey val batchId: String,
    val userId: String,
    val dateKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val status: String = SyncBatchStatus.PENDING,
    val retryCount: Int = 0,
    val eventCount: Int = 0,
    val checksum: String? = null,
    val errorMessage: String? = null
)

object SyncBatchStatus {
    const val PENDING = "PENDING"
    const val SYNCING = "SYNCING"
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
}
