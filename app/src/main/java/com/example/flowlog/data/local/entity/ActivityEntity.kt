package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.constants.SyncStatus

@Entity(
    tableName = "activities",
    indices = [
        Index("userId"),
        Index("startTime"),
        Index("isDeleted"),
        Index("syncStatus"),
        Index(value = ["userId", "isDeleted"])
    ]
)
data class ActivityEntity(
    @PrimaryKey val activityId: String,
    val userId: String,
    val title: String,
    val category: String,
    val note: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMillis: Long,
    val isFavorite: Boolean = false,
    val linkedTodoId: String? = null,       // "legacy_todo_${legacyLinkedTodoId}" 형태
    val legacyId: Long? = null,
    val legacyLinkedTodoId: Long? = null,   // 원본 Long linkedTodoId 보존용
    val tagsJson: String? = null,           // ActivitySession.tags → JSON 문자열로 보존
    @ColumnInfo(defaultValue = "'MANUAL'")
    val sourceType: String = ActivitySourceType.MANUAL,
    val sourceId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val isDeleted: Boolean = false,
    // TODO: 스키마 변경 시 version 올리고 Room Migration 추가 필요
    //   현재 version = 1, 개발 단계에서는 앱 데이터 초기화로 처리
    val syncStatus: String = SyncStatus.PENDING
)
