package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flowlog.data.constants.SyncStatus

@Entity(
    tableName = "todos",
    indices = [
        Index("userId"),
        Index("isDeleted"),
        Index("isCompleted"),
        Index("syncStatus"),
        Index(value = ["userId", "isDeleted"]),
        Index("calendarSourceId")
    ]
)
data class TodoEntity(
    @PrimaryKey val todoId: String,
    val userId: String,
    val title: String,
    val description: String? = null,
    val category: String = "NORMAL",
    val selectedDate: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val scaleEstimate: String? = null,
    val scaleAlgorithmVersion: String? = null,
    val accumulatedWorkMillis: Long = 0L,
    val burdenLevel: String? = null,
    val burdenGroupKey: String? = null,
    val burdenScore: Int = 0,
    val burdenReasonJson: String? = null,
    val reviewStage: Int = 0,
    val reviewStage1CompletedAt: Long? = null,
    val legacyId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.PENDING,
    val calendarSourceId: String? = null,
    val calendarSourceType: String? = null,
    val calendarPlanId: String? = null
)
