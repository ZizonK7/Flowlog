package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installations",
    indices = [Index("userId")]
)
data class InstallationEntity(
    @PrimaryKey val installationId: String,
    val userId: String? = null,
    val deviceName: String? = null,
    val appVersion: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)
