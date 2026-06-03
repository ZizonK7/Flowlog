package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flowlog.data.constants.SyncStatus

@Entity(
    tableName = "daily_goal_items",
    indices = [
        Index("recommendationId"),
        Index("userId"),
        Index("todoId")
    ]
)
data class DailyGoalItemEntity(
    @PrimaryKey val itemId: String,
    val recommendationId: String,
    val userId: String,
    val todoId: String,
    val rank: Int,
    val reason: String? = null,
    val todoSnapshotJson: String? = null,
    val burdenLevel: String? = null,
    val burdenReasonJson: String? = null,
    val plannedStartMillis: Long? = null,
    val plannedEndMillis: Long? = null,
    val recommendedDurationMinutes: Int? = null,
    val notificationScheduledAtMillis: Long? = null,
    val userActionStatus: String = "PLANNED",
    val actualStartedAt: Long? = null,
    val actualCompletedAt: Long? = null,
    val linkedActivityId: String? = null,
    val completedTodoId: String? = null,
    val notificationDeliveredAt: Long? = null,
    val notificationClickedAt: Long? = null,
    val wasClicked: Boolean = false,
    val wasCompleted: Boolean = false,
    val wasSkipped: Boolean = false,
    val wasDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.PENDING
)
