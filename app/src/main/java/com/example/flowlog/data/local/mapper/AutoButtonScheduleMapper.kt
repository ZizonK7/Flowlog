package com.example.flowlog.data.local.mapper

import com.example.flowlog.data.local.entity.AutoButtonScheduleEntity
import com.example.flowlog.data.model.AutoButtonSchedule
import java.util.UUID

fun AutoButtonSchedule.toEntity(userId: String): AutoButtonScheduleEntity {
    val now = System.currentTimeMillis()
    return AutoButtonScheduleEntity(
        scheduleId = scheduleId.ifBlank { UUID.randomUUID().toString() },
        userId = userId,
        title = title,
        category = category,
        repeatDaysMask = repeatDays.toMask(),
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = endMinuteOfDay,
        isEnabled = isEnabled,
        notifyOnStart = notifyOnStart,
        notifyOnEnd = notifyOnEnd,
        createdAt = now,
        updatedAt = now,
        isDeleted = false
    )
}

fun AutoButtonScheduleEntity.toModel(isSkippedToday: Boolean = false): AutoButtonSchedule {
    return AutoButtonSchedule(
        scheduleId = scheduleId,
        title = title,
        category = category,
        repeatDays = repeatDaysMask.toDays(),
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = endMinuteOfDay,
        isEnabled = isEnabled,
        notifyOnStart = notifyOnStart,
        notifyOnEnd = notifyOnEnd,
        isSkippedToday = isSkippedToday
    )
}

fun Set<Int>.toMask(): Int = fold(0) { mask, day -> mask or (1 shl day) }

fun Int.toDays(): Set<Int> = (1..7).filter { day -> this and (1 shl day) != 0 }.toSet()
