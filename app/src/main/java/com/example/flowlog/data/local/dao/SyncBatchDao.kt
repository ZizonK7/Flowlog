package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flowlog.data.local.entity.SyncBatchEntity

@Dao
interface SyncBatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: SyncBatchEntity)

    @Query("""
        UPDATE sync_batches
        SET status = :status, syncedAt = :syncedAt
        WHERE batchId = :batchId
    """)
    suspend fun markBatchSuccess(batchId: String, status: String, syncedAt: Long)

    @Query("""
        UPDATE sync_batches
        SET status = :status,
            errorMessage = :errorMessage,
            retryCount = retryCount + 1
        WHERE batchId = :batchId
    """)
    suspend fun markBatchFailed(batchId: String, status: String, errorMessage: String?)

    @Query("""
        SELECT * FROM sync_batches
        WHERE userId = :userId
          AND status IN ('PENDING', 'FAILED')
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingBatches(userId: String): List<SyncBatchEntity>

    @Query("SELECT * FROM sync_batches WHERE batchId = :batchId LIMIT 1")
    suspend fun getBatch(batchId: String): SyncBatchEntity?
}
