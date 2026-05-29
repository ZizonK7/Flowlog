package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "migration_errors")
data class MigrationErrorEntity(
    @PrimaryKey val errorId: String,
    val sourceType: String,
    val sourceId: String? = null,
    val rawJson: String? = null,
    val errorMessage: String,
    val createdAt: Long = System.currentTimeMillis()
)
