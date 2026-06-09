package com.example.flowlog.data.model

import com.example.flowlog.data.constants.ActivitySourceType
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseSetRecord(
    val name: String,
    val reps: Int,
    val intensity: String,
    val mode: String = "COUNT",
    val durationMillis: Long? = null
)

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
    val exerciseSets: List<ExerciseSetRecord> = emptyList(),
    val isFavorite: Boolean = false,
    val linkedTodoId: Long? = null,
    val sourceType: String = ActivitySourceType.MANUAL,
    val sourceId: String? = null,
    val modifiedTime: Long = System.currentTimeMillis()
)

