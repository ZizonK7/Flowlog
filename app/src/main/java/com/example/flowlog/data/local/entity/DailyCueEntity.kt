package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "daily_cues",
    primaryKeys = ["userId", "cueId"],
    indices = [
        Index("userId"),
        Index("archivedAt"),
        Index(value = ["userId", "archivedAt"])
    ]
)
data class DailyCueEntity(
    val userId: String,
    val cueId: Long,
    val label: String,
    val title: String,
    val timerDurationMillis: Long?,
    val timerCategory: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long? = null
)
