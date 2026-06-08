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
        SELECT * FROM daily_goal_recommendations
        WHERE userId = :userId AND dateKey < :dateKey
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun getRecommendationsBeforeDate(
        userId: String,
        dateKey: String,
        limit: Int = 7
    ): List<DailyGoalRecommendationEntity>

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
        SELECT * FROM daily_goal_items
        WHERE itemId = :itemId
        LIMIT 1
    """)
    suspend fun getGoalItem(itemId: String): DailyGoalItemEntity?

    @Query("""
        SELECT * FROM daily_goal_items
        WHERE userId = :userId
            AND notificationScheduledAtMillis IS NOT NULL
            AND notificationScheduledAtMillis > :now
            AND userActionStatus IN ('PLANNED', 'RESCHEDULED')
            AND wasCompleted = 0
            AND wasSkipped = 0
            AND wasDeleted = 0
        ORDER BY notificationScheduledAtMillis ASC
    """)
    suspend fun getUpcomingPlannedItemsForReminder(
        userId: String,
        now: Long
    ): List<DailyGoalItemEntity>

    @Query("""
        SELECT item.* FROM daily_goal_items AS item
        INNER JOIN daily_goal_recommendations AS recommendation
            ON item.recommendationId = recommendation.recommendationId
        WHERE recommendation.userId = :userId
            AND recommendation.dateKey = :dateKey
            AND item.plannedStartMillis IS NOT NULL
            AND item.plannedEndMillis IS NOT NULL
            AND recommendation.recommendationId = (
                SELECT recommendationId FROM daily_goal_recommendations
                WHERE userId = :userId AND dateKey = :dateKey
                ORDER BY createdAt DESC LIMIT 1
            )
        ORDER BY item.plannedStartMillis ASC, item.rank ASC
    """)
    fun observePlannedItemsForDate(userId: String, dateKey: String): Flow<List<DailyGoalItemEntity>>

    @Query("""
        SELECT item.* FROM daily_goal_items AS item
        INNER JOIN daily_goal_recommendations AS recommendation
            ON item.recommendationId = recommendation.recommendationId
        WHERE recommendation.userId = :userId
            AND recommendation.dateKey = :dateKey
            AND item.userActionStatus != 'DISMISSED'
            AND item.wasCompleted = 0
            AND item.wasSkipped = 0
            AND item.wasDeleted = 0
            AND recommendation.recommendationId = (
                SELECT recommendationId FROM daily_goal_recommendations
                WHERE userId = :userId AND dateKey = :dateKey
                ORDER BY createdAt DESC LIMIT 1
            )
        ORDER BY item.rank ASC
    """)
    fun observeActiveItemsForDate(userId: String, dateKey: String): Flow<List<DailyGoalItemEntity>>

    @Query("""
        SELECT item.* FROM daily_goal_items AS item
        INNER JOIN daily_goal_recommendations AS recommendation
            ON item.recommendationId = recommendation.recommendationId
        WHERE recommendation.userId = :userId
            AND recommendation.dateKey = :dateKey
        ORDER BY item.rank ASC
    """)
    suspend fun getItemsForDate(userId: String, dateKey: String): List<DailyGoalItemEntity>

    @Query("""
        UPDATE daily_goal_recommendations
        SET updatedAt = :updatedAt,
            recommendationMode = :recommendationMode,
            workplaceDetected = :workplaceDetected,
            workplaceBlocksJson = :workplaceBlocksJson,
            selectedTodoIdsJson = :selectedTodoIdsJson,
            heavyTodoId = :heavyTodoId,
            heavyBurdenLevel = :heavyBurdenLevel,
            heavyReason = :heavyReason,
            heavyDistributionSnapshotJson = :heavyDistributionSnapshotJson,
            lightTodoId = :lightTodoId,
            lightBurdenLevel = :lightBurdenLevel,
            lightReason = :lightReason,
            lightDistributionSnapshotJson = :lightDistributionSnapshotJson,
            plannedItemsJson = :plannedItemsJson,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE recommendationId = :recommendationId
            AND userId = :userId
    """)
    suspend fun updateRecommendationTimePlan(
        userId: String,
        recommendationId: String,
        updatedAt: Long,
        recommendationMode: String,
        workplaceDetected: Boolean,
        workplaceBlocksJson: String?,
        selectedTodoIdsJson: String?,
        heavyTodoId: String?,
        heavyBurdenLevel: String?,
        heavyReason: String?,
        heavyDistributionSnapshotJson: String?,
        lightTodoId: String?,
        lightBurdenLevel: String?,
        lightReason: String?,
        lightDistributionSnapshotJson: String?,
        plannedItemsJson: String?
    )

    @Query("""
        UPDATE daily_goal_items
        SET burdenLevel = :burdenLevel,
            plannedStartMillis = :plannedStartMillis,
            plannedEndMillis = :plannedEndMillis,
            recommendedDurationMinutes = :recommendedDurationMinutes,
            notificationScheduledAtMillis = :notificationScheduledAtMillis,
            userActionStatus = 'PLANNED',
            actualStartedAt = NULL,
            actualCompletedAt = NULL,
            linkedActivityId = NULL,
            completedTodoId = NULL,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE itemId = :itemId
            AND userId = :userId
    """)
    suspend fun updateItemTimePlan(
        userId: String,
        itemId: String,
        burdenLevel: String,
        plannedStartMillis: Long,
        plannedEndMillis: Long,
        recommendedDurationMinutes: Int,
        notificationScheduledAtMillis: Long?,
        updatedAt: Long
    )

    @Query("""
        UPDATE daily_goal_items
        SET userActionStatus = :status,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE itemId = :itemId
            AND userId = :userId
    """)
    suspend fun updateItemActionStatus(userId: String, itemId: String, status: String, updatedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET plannedStartMillis = :plannedStartMillis,
            plannedEndMillis = :plannedEndMillis,
            recommendedDurationMinutes = :durationMinutes,
            notificationScheduledAtMillis = :notificationScheduledAtMillis,
            userActionStatus = 'RESCHEDULED',
            actualStartedAt = NULL,
            actualCompletedAt = NULL,
            linkedActivityId = NULL,
            completedTodoId = NULL,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE itemId = :itemId
            AND userId = :userId
    """)
    suspend fun updateItemTimeManually(
        userId: String,
        itemId: String,
        plannedStartMillis: Long,
        plannedEndMillis: Long,
        durationMinutes: Int,
        notificationScheduledAtMillis: Long?,
        updatedAt: Long
    )

    @Query("""
        UPDATE daily_goal_items
        SET userActionStatus = 'STARTED',
            wasClicked = 1,
            actualStartedAt = :actualStartedAt,
            updatedAt = :actualStartedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE itemId = :itemId
            AND userId = :userId
    """)
    suspend fun markPlannedItemStarted(userId: String, itemId: String, actualStartedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET userActionStatus = 'RESCHEDULED',
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE itemId = :itemId
            AND userId = :userId
            AND userActionStatus = 'STARTED'
    """)
    suspend fun revertStartedItem(userId: String, itemId: String, updatedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET userActionStatus = 'COMPLETED',
            wasCompleted = 1,
            actualCompletedAt = :actualCompletedAt,
            linkedActivityId = :linkedActivityId,
            completedTodoId = :completedTodoId,
            updatedAt = :actualCompletedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE userId = :userId
            AND todoId = :todoId
            AND actualStartedAt IS NOT NULL
            AND actualCompletedAt IS NULL
    """)
    suspend fun markOpenPlannedItemCompleted(
        userId: String,
        todoId: String,
        actualCompletedAt: Long,
        linkedActivityId: String?,
        completedTodoId: String?
    )

    @Query("""
        UPDATE daily_goal_items
        SET userActionStatus = 'COMPLETED',
            wasCompleted = 1,
            actualCompletedAt = :actualCompletedAt,
            linkedActivityId = :linkedActivityId,
            completedTodoId = :completedTodoId,
            updatedAt = :actualCompletedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE userId = :userId
            AND itemId = :itemId
    """)
    suspend fun markPlannedItemCompleted(
        userId: String,
        itemId: String,
        actualCompletedAt: Long,
        linkedActivityId: String?,
        completedTodoId: String?
    )

    @Query("""
        UPDATE daily_goal_items
        SET userActionStatus = :restoredStatus,
            wasCompleted = 0,
            actualCompletedAt = NULL,
            linkedActivityId = NULL,
            completedTodoId = NULL,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE userId = :userId
            AND itemId = :itemId
    """)
    suspend fun revertPlannedItemCompleted(
        userId: String,
        itemId: String,
        restoredStatus: String,
        updatedAt: Long
    )

    @Query("""
        UPDATE daily_goal_items
        SET wasCompleted = 1, updatedAt = :updatedAt, syncStatus = '${SyncStatus.PENDING}'
        WHERE recommendationId = :recommendationId AND todoId = :todoId
    """)
    suspend fun markItemCompleted(recommendationId: String, todoId: String, updatedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET wasCompleted = 1, updatedAt = :updatedAt, syncStatus = '${SyncStatus.PENDING}'
        WHERE recommendationId = :recommendationId AND todoId = :todoId
    """)
    suspend fun markItemCompletedPending(recommendationId: String, todoId: String, updatedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET wasClicked = 1, updatedAt = :updatedAt, syncStatus = '${SyncStatus.PENDING}'
        WHERE recommendationId = :recommendationId AND todoId = :todoId
    """)
    suspend fun markItemClicked(recommendationId: String, todoId: String, updatedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET notificationDeliveredAt = :deliveredAt, updatedAt = :deliveredAt, syncStatus = '${SyncStatus.PENDING}'
        WHERE itemId = :itemId
    """)
    suspend fun markNotificationDelivered(itemId: String, deliveredAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET notificationClickedAt = :clickedAt, updatedAt = :clickedAt, syncStatus = '${SyncStatus.PENDING}'
        WHERE itemId = :itemId
    """)
    suspend fun markNotificationClicked(itemId: String, clickedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET wasSkipped = 1, updatedAt = :updatedAt, syncStatus = '${SyncStatus.PENDING}'
        WHERE recommendationId = :recommendationId AND todoId = :todoId
    """)
    suspend fun markItemSkipped(recommendationId: String, todoId: String, updatedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET wasSkipped = 1, updatedAt = :updatedAt, syncStatus = '${SyncStatus.PENDING}'
        WHERE recommendationId = :recommendationId AND todoId = :todoId
    """)
    suspend fun markItemSkippedPending(recommendationId: String, todoId: String, updatedAt: Long)

    @Query("""
        UPDATE daily_goal_items
        SET wasDeleted = 1, updatedAt = :updatedAt, syncStatus = '${SyncStatus.PENDING}'
        WHERE recommendationId = :recommendationId AND todoId = :todoId
    """)
    suspend fun markItemDeleted(recommendationId: String, todoId: String, updatedAt: Long)
}
