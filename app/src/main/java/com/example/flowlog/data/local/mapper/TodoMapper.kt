package com.example.flowlog.data.local.mapper

import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.entity.TodoEntity
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import java.util.UUID

/**
 * TodoItem → TodoEntity
 *
 * todoId 생성 규칙:
 * - item.id != 0L (기존 SharedPrefs 데이터) → "legacy_todo_${item.id}"
 * - item.id == 0L (새로 생성되는 Room 전용 데이터) → UUID 신규 발급
 *
 * accumulatedSeconds → accumulatedWorkMillis: 초 단위를 밀리초 단위로 변환 (× 1000).
 */
fun TodoItem.toTodoEntity(userId: String): TodoEntity {
    val todoId = if (id != 0L) "legacy_todo_$id" else UUID.randomUUID().toString()
    return TodoEntity(
        todoId = todoId,
        userId = userId,
        title = title,
        category = category.name,
        selectedDate = selectedDate,
        isCompleted = isCompleted,
        completedAt = completedAt,
        isDeleted = false,
        deletedAt = null,
        accumulatedWorkMillis = accumulatedSeconds * 1000L,
        burdenLevel = burdenLevel,
        burdenGroupKey = burdenGroupKey,
        burdenScore = burdenScore,
        burdenReasonJson = burdenReasonJson,
        reviewStage = reviewStage,
        reviewStage1CompletedAt = reviewStage1CompletedAt,
        legacyId = if (id != 0L) id else null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = SyncStatus.PENDING
    )
}

/**
 * TodoEntity → TodoItem
 *
 * id 복원 규칙:
 * - legacyId != null → 기존 Long id 복원 (SharedPrefs 시절 데이터)
 * - legacyId == null → 0L fallback (순수 Room 신규 데이터, 전환 완료 전까지 임시)
 *   TODO(Room 전환 완료 후): Room 전용 데이터는 String todoId 기반으로 동작하도록 변경.
 *
 * accumulatedWorkMillis → accumulatedSeconds: 밀리초를 초로 변환 (÷ 1000).
 *
 * category 복원:
 * - String → TodoCategory.valueOf() 로 변환.
 * - 알 수 없는 값이면 NORMAL로 fallback (데이터 이슈 방어).
 */
fun TodoEntity.toTodoItem(): TodoItem {
    return TodoItem(
        id = legacyId ?: 0L,
        title = title,
        category = runCatching { TodoCategory.valueOf(category) }.getOrDefault(TodoCategory.NORMAL),
        createdAt = createdAt,
        selectedDate = selectedDate,
        isCompleted = isCompleted,
        completedAt = completedAt,
        accumulatedSeconds = accumulatedWorkMillis / 1000L,
        burdenLevel = burdenLevel,
        burdenGroupKey = burdenGroupKey,
        burdenScore = burdenScore,
        burdenReasonJson = burdenReasonJson,
        updatedAt = updatedAt,
        reviewStage = reviewStage,
        reviewStage1CompletedAt = reviewStage1CompletedAt,
        calendarSourceId = calendarSourceId
    )
}
