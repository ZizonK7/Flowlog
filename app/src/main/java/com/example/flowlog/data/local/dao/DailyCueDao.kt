package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flowlog.data.local.entity.DailyCueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCue(cue: DailyCueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCues(cues: List<DailyCueEntity>)

    @Query("SELECT * FROM daily_cues WHERE userId = :userId AND cueId = :cueId LIMIT 1")
    suspend fun getCue(userId: String, cueId: Long): DailyCueEntity?

    @Query("SELECT * FROM daily_cues WHERE userId = :userId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeCues(userId: String): Flow<List<DailyCueEntity>>

    @Query("SELECT COUNT(*) FROM daily_cues WHERE userId = :userId")
    suspend fun countCues(userId: String): Int

    @Query("""
        UPDATE daily_cues
        SET archivedAt = :archivedAt,
            updatedAt = :updatedAt
        WHERE userId = :userId
          AND cueId = :cueId
          AND archivedAt IS NULL
    """)
    suspend fun archiveCue(userId: String, cueId: Long, archivedAt: Long, updatedAt: Long)
}
