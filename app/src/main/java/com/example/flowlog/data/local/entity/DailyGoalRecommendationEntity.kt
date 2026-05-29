package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flowlog.data.constants.SyncStatus

@Entity(
    tableName = "daily_goal_recommendations",
    indices = [
        Index("userId"),
        Index("dateKey"),
        Index(value = ["userId", "dateKey"])
    ]
)
data class DailyGoalRecommendationEntity(
    @PrimaryKey val recommendationId: String,
    val userId: String,
    val dateKey: String,
    val algorithmVersion: String,
    val generatedAt: Long,
    val reasonSummary: String? = null,
    val candidateSnapshotJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.PENDING
)
