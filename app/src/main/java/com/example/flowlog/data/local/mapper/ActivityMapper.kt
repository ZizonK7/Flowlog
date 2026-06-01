package com.example.flowlog.data.local.mapper

import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.entity.ActivityEntity
import com.example.flowlog.data.model.ActivitySession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val mapperJson = Json { ignoreUnknownKeys = true }

/**
 * ActivitySession → ActivityEntity
 *
 * activityId 생성 규칙:
 * - session.id != 0L (기존 SharedPrefs 데이터) → "legacy_activity_${session.id}"
 * - session.id == 0L (새로 생성되는 Room 전용 데이터) → UUID 신규 발급
 *
 * [userId] 는 현재 로그인한 uid 또는 "anonymous" 를 호출부에서 전달.
 * TODO(Room 전환 완료 후): "anonymous" 데이터를 실제 uid로 reassign하는 단계 필요.
 */
fun ActivitySession.toActivityEntity(userId: String): ActivityEntity {
    val activityId = if (id != 0L) "legacy_activity_$id" else UUID.randomUUID().toString()
    return ActivityEntity(
        activityId = activityId,
        userId = userId,
        title = title,
        category = category,
        note = note,
        startTime = startTime,
        endTime = endTime,
        durationMillis = durationMillis,
        isFavorite = isFavorite,
        linkedTodoId = linkedTodoId?.let { "legacy_todo_$it" },
        legacyLinkedTodoId = linkedTodoId,
        legacyId = if (id != 0L) id else null,
        tagsJson = if (tags.isNotEmpty()) mapperJson.encodeToString(tags) else null,
        sourceType = sourceType,
        sourceId = sourceId,
        createdAt = startTime,
        updatedAt = modifiedTime,
        isDeleted = false,
        deletedAt = null,
        syncStatus = SyncStatus.PENDING
    )
}

/**
 * ActivityEntity → ActivitySession
 *
 * id 복원 규칙:
 * - legacyId != null → 기존 Long id 복원 (SharedPrefs 시절 데이터)
 * - legacyId == null → 0L fallback (순수 Room 신규 데이터, 전환 완료 전까지 임시)
 *   TODO(Room 전환 완료 후): Room 전용 데이터는 String activityId 기반으로 동작하도록 변경.
 *
 * endTime 복원:
 * - Entity의 endTime은 nullable (진행 중인 타이머 지원용)
 * - ActivitySession.endTime은 non-null → null이면 0L fallback
 *   실제 저장된 활동은 항상 endTime이 있으므로 0L은 데이터 이슈 신호.
 */
fun ActivityEntity.toActivitySession(): ActivitySession {
    return ActivitySession(
        id = legacyId ?: 0L,
        category = category,
        title = title,
        startTime = startTime,
        endTime = endTime ?: 0L,
        durationMillis = durationMillis,
        note = note,
        tags = tagsJson?.let {
            runCatching { mapperJson.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
        } ?: emptyList(),
        isFavorite = isFavorite,
        linkedTodoId = legacyLinkedTodoId,
        sourceType = sourceType,
        sourceId = sourceId,
        modifiedTime = updatedAt
    )
}
