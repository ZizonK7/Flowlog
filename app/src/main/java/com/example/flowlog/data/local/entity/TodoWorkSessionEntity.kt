package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flowlog.data.constants.SyncStatus

@Entity(
    tableName = "todo_work_sessions",
    indices = [
        Index("userId"),
        Index("todoId"),
        Index("isDeleted"),
        Index(value = ["todoId", "isDeleted"])
    ]
)
data class TodoWorkSessionEntity(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val todoId: String,
    val activityId: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMillis: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val isDeleted: Boolean = false,
    val syncStatus: String = SyncStatus.PENDING
)
