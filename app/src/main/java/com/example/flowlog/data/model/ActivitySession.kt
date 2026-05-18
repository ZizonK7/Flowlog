package com.example.flowlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ActivitySession(
    val id: Long = 0,
    val category: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val linkedTodoId: Long? = null,
    val modifiedTime: Long = System.currentTimeMillis()
)

