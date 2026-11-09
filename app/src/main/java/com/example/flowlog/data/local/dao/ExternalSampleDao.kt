package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.entity.ExternalSampleEntity

@Dao
interface ExternalSampleDao {

    @Query("SELECT * FROM external_samples WHERE sampleId IN (:sampleIds)")
    suspend fun getByIds(sampleIds: List<String>): List<ExternalSampleEntity>

    // REPLACE: a re-read sample whose content changed overwrites its row (upsert).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(samples: List<ExternalSampleEntity>)

    @Query("""
        SELECT COUNT(*) FROM external_samples
        WHERE userId = :userId
          AND provider = :provider
          AND startTime >= :sinceMillis
    """)
    suspend fun countSince(userId: String, provider: String, sinceMillis: Long): Int

    @Query("""
        SELECT * FROM external_samples
        WHERE userId = :userId
          AND syncStatus = '${SyncStatus.PENDING}'
        ORDER BY startTime ASC
    """)
    suspend fun getUnsyncedSamples(userId: String): List<ExternalSampleEntity>

    // Eligible = the sample started before today (today's step count is still growing).
    @Query("""
        SELECT * FROM external_samples
        WHERE userId = :userId
          AND syncStatus = '${SyncStatus.PENDING}'
          AND startTime < :cutoffMillis
        ORDER BY startTime ASC
    """)
    suspend fun getEligibleUnsyncedSamples(userId: String, cutoffMillis: Long): List<ExternalSampleEntity>

    // Only rows not rewritten since the upload began: a sample re-read and changed meanwhile
    // (fetchedAt later than uploadStartedAt) stays PENDING and goes up next time.
    @Query("""
        UPDATE external_samples
        SET syncStatus = '${SyncStatus.SYNCED}'
        WHERE sampleId IN (:sampleIds)
          AND fetchedAt <= :uploadStartedAt
    """)
    suspend fun markSamplesSynced(sampleIds: List<String>, uploadStartedAt: Long)

    // 로그인 전 'anonymous' userId로 저장된 샘플을 실제 uid로 재할당
    @Query("UPDATE external_samples SET userId = :newUserId, syncStatus = 'PENDING' WHERE userId = 'anonymous'")
    suspend fun reassignAnonymousUser(newUserId: String): Int
}
