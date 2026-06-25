package com.example.flowlog.data.model

data class AutoButtonSchedule(
    val scheduleId: String = "",
    val title: String,
    val category: String,
    val repeatDays: Set<Int>,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val isEnabled: Boolean = true,
    val notifyOnStart: Boolean = true,
    val notifyOnEnd: Boolean = true,
    val isSkippedToday: Boolean = false,
    val source: String = "MANUAL",
    val sourceDateKey: Long? = null,
    val sourceDateKeys: Set<Long> = emptySet(),
    val sourceEventIds: Set<String> = emptySet()
)

data class ScheduledAutoButtonBlock(
    val scheduleId: String,
    val title: String,
    val category: String,
    val startTime: Long,
    val endTime: Long,
    val isSkippedToday: Boolean,
    val isCalendarPetite: Boolean = false
)
