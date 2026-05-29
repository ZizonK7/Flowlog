package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.example.flowlog.data.local.entity.DailyGoalRecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(recommendation: DailyGoalRecommendationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalItems(items: List<DailyGoalItemEntity>)

        @Query("""
                SELECT * FROM daily_goal_recommendations
                WHERE userId = :userId
                    AND syncStatus = '${SyncStatus.PENDING}'
                ORDER BY createdAt ASC
        """)
        suspend fun getUnsyncedRecommendations(userId: String): List<DailyGoalRecommendationEntity>

        @Query("""
                SELECT * FROM daily_goal_recommendations
                WHERE userId = :userId
                    AND syncStatus = '${SyncStatus.PENDING}'
                    AND createdAt < :cutoffMillis
                ORDER BY createdAt ASC
        """)
        suspend fun getEligibleUnsyncedRecommendations(
            userId: String,
            cutoffMillis: Long
        ): List<DailyGoalRecommendationEntity>

        @Query("""
                SELECT * FROM daily_goal_items
                WHERE userId = :userId
                    AND syncStatus = '${SyncStatus.PENDING}'
                ORDER BY createdAt ASC, rank ASC
        """)
        suspend fun getUnsyncedItems(userId: String): List<DailyGoalItemEntity>

        @Query("""
                SELECT * FROM daily_goal_items
                WHERE userId = :userId
                    AND syncStatus = '${SyncStatus.PENDING}'
                    AND createdAt < :cutoffMillis
                ORDER BY createdAt ASC, rank ASC
        """)
        suspend fun getEligibleUnsyncedItems(
            userId: String,
            cutoffMillis: Long
        ): List<DailyGoalItemEntity>

        @Query("""
                UPDATE daily_goal_recommendations
                SET syncStatus = '${SyncStatus.SYNCED}'
                WHERE recommendationId IN (:ids)
                    AND userId = :userId
        """)
        suspend fun markRecommendationsSynced(userId: String, ids: List<String>)

        @Query("""
                UPDATE daily_goal_items
                SET syncStatus = '${SyncStatus.SYNCED}'
                WHERE itemId IN (:ids)
                    AND userId = :userId
        """)
        suspend fun markItemsSynced(userId: String, ids: List<String>)

    // 오늘 날짜의 가장 최근 추천 1건 (refresh 시 새 row가 쌓이므로 DESC LIMIT 1)
    @Query("""
        SELECT * FROM daily_goal_recommendations
        WHERE userId = :userId AND dateKey = :dateKey
        ORDER BY createdAt DESC
        LIMIT 1
    """)
    suspend fun getRecommendationByDate(
        userId: String,
        dateKey: String
    ): DailyGoalRecommendationEntity?

    @Query("""
        SELECT * FROM daily_goal_items
        WHERE recommendationId = :recommendationId
        ORDER BY rank ASC
    """)
    fun observeGoalItems(recommendationId: String): Flow<List<DailyGoalItemEntity>>

    @Query("""
        SELECT * FROM daily_goal_items
        WHERE recommendationId = :recommendationId
        ORDER BY rank ASC
    """)
    suspend fun getGoalItems(recommendationId: String): List<DailyGoalItemEntity>

    @Query("""
        UPDATE daily_goal_items
        SET wasCompleted = 1, updatedAt = :updatedAt
        WHERE recommendationId = :recommendationId AND todoId = :todoId
    """)
    suspend fun markItemCompleted(recommendationId: String, todoId: String, updatedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET wasClicked = 1, updatedAt = :updatedAt
        WHERE recommendationId = :recommendationId AND todoId = :todoId
    """)
    suspend fun markItemClicked(recommendationId: String, todoId: String, updatedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET wasSkipped = 1, updatedAt = :updatedAt
        WHERE recommendationId = :recommendationId AND todoId = :todoId
    """)
    suspend fun markItemSkipped(recommendationId: String, todoId: String, updatedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET wasDeleted = 1, updatedAt = :updatedAt
        WHERE recommendationId = :recommendationId AND todoId = :todoId
    """)
    suspend fun markItemDeleted(recommendationId: String, todoId: String, updatedAt: Long)
}
