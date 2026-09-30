package com.example.flowlog.data.agent

import com.example.flowlog.data.model.TodoCategory

enum class PetiteSourceType {
    PETITE,
    TODO,
    ROUTINE,
    EXAM,
    CALENDAR,
    STUDY_PLAN
}

data class OrganizedPetite(
    val id: String,
    val title: String,
    val sourceType: PetiteSourceType,
    val sourceId: String?,
    val category: TodoCategory? = null,
    val dateMillis: Long? = null,
    val linkedActivityName: String? = null,
    val activityCategory: String? = null,
    val isCompleted: Boolean = false,
    val priorityScore: Int,
    val burdenScore: Int? = null,
    val isSeverelyBehind: Boolean? = null,
    val totalStudyMinutesSinceD7: Int? = null,
    val studiedDaysSinceD7: Int? = null,
    val missedDaysSinceD7: Int? = null,
    val aiComment: String? = null,
    val estimatedMinutes: Int? = null,
    val steps: List<String> = emptyList(),
    val examDValue: Int? = null,
    val routineTimerDurationMillis: Long? = null,
    val routineTimerCategory: String? = null,
    // 미래 필터링용: "ACADEMIC"(공부/과제) / "DAILY"(일상) / null(미분류)
    val calendarTaskType: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
