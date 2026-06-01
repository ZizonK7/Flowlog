package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auto_button_undo_snapshots",
    indices = [
        Index("scheduleId"),
        Index("autoActivityId"),
        Index("expiresAt"),
        Index("isUsed")
    ]
)
data class AutoButtonUndoSnapshotEntity(
    @PrimaryKey val id: String,
    val scheduleId: String,
    val autoActivityId: String,
    val previousActivityId: String?,
    val previousActivityTitle: String?,
    val previousActivityCategory: String?,
    val previousActivityStartTime: Long?,
    val previousActivityEndTimeBeforeAuto: Long?,
    val triggeredAt: Long,
    val expiresAt: Long,
    val isUsed: Boolean = false
)
