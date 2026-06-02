package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.local.RoomTodoLocalDataSource
import com.example.flowlog.data.local.entity.TodoEntity
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.TodoBurdenAnalysis
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class TodoRepository(context: Context) {
    private val appContext = context.applicationContext
    private val eventLogRepository = EventLogRepository(appContext)
    private val roomDataSource = RoomTodoLocalDataSource(appContext)
    private val restoreJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // 신규 Todo ID 생성자. currentTimeMillis로 초기화 후 세션 내 atomic increment.
    // 기존 legacyId(1, 2, 3...)와 충돌 없음 (타임스탬프 영역은 ~1.7×10¹²).
    private val idCounter = AtomicLong(System.currentTimeMillis())

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    // ── 읽기 경로 (Room) ─────────────────────────────────────────────────

    // auth 상태 변화 시 userId를 재평가하고 Room Flow를 재구독.
    private fun userIdFlow(): Flow<String> = callbackFlow {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.uid ?: "anonymous")
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid ?: "anonymous")
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    fun getAllTodos(): Flow<List<TodoItem>> =
        userIdFlow().flatMapLatest { uid -> roomDataSource.observeAllTodos(uid) }

    fun getIncompleteTodos(): Flow<List<TodoItem>> =
        userIdFlow().flatMapLatest { uid -> roomDataSource.observeIncompleteTodos(uid) }

    // ── 쓰기 경로 (Room primary) ──────────────────────────────────────────
    //
    // Room이 유일한 로컬 저장소. per-action Firebase sync 제거됨.
    // ID 정책: AtomicLong 기반 타임스탬프 → legacyId 영역(1,2,3...)과 충돌 없음.
    //
    // syncStatus 정책:
    //   Room write 직후 → PENDING (mapper 기본값)
    //   Firebase sync → FirebaseSyncDataSource.syncAll(uid) 가 PENDING 항목 batch upload
    //   soft delete → isDeleted=1 + PENDING → batch sync에서 Firestore delete 처리

    suspend fun insertTodo(todo: TodoItem): Long {
        val id = idCounter.incrementAndGet()
        // Room primary write — observeAllTodos Flow 즉시 emit → UI 즉각 갱신
        roomDataSource.insert(todo.copy(id = id), userId)
        runCatching {
            eventLogRepository.log(
                eventType = EventType.TODO_CREATED,
                entityType = EntityType.TODO,
                entityId = id.toString()
            )
        }
        return id
    }

    suspend fun updateCompleted(id: Long, isCompleted: Boolean, completedAt: Long?) {
        if (isCompleted) {
            runCatching { roomDataSource.completeTodoByLegacyId(id) }
            runCatching {
                eventLogRepository.log(
                    eventType = EventType.TODO_COMPLETED,
                    entityType = EntityType.TODO,
                    entityId = id.toString()
                )
            }
        } else {
            runCatching { roomDataSource.uncompleteTodoByLegacyId(id) }
            runCatching {
                eventLogRepository.log(
                    eventType = EventType.TODO_UNCOMPLETED,
                    entityType = EntityType.TODO,
                    entityId = id.toString()
                )
            }
        }
    }

    suspend fun updateTodo(todo: TodoItem) {
        runCatching { roomDataSource.update(todo, userId) }
        runCatching {
            eventLogRepository.log(
                eventType = EventType.TODO_UPDATED,
                entityType = EntityType.TODO,
                entityId = todo.id.toString()
            )
        }
    }

    suspend fun deleteTodo(todo: TodoItem) {
        runCatching { roomDataSource.softDeleteByLegacyId(todo.id) }
        runCatching {
            eventLogRepository.log(
                eventType = EventType.TODO_DELETED,
                entityType = EntityType.TODO,
                entityId = todo.id.toString()
            )
        }
    }

    suspend fun addAccumulatedSeconds(id: Long, seconds: Long) {
        // seconds != 0L: 음수(undo)도 Room에 반영
        if (seconds != 0L) {
            runCatching { roomDataSource.addToAccumulatedWorkMillisByLegacyId(id, seconds * 1000L) }
        }
        runCatching {
            eventLogRepository.log(
                eventType = EventType.TODO_WORK_ADDED,
                entityType = EntityType.TODO,
                entityId = id.toString()
            )
        }
    }

    suspend fun updateBurdenCaches(analyses: List<TodoBurdenAnalysis>) {
        analyses.forEach { analysis ->
            if (analysis.todo.id != 0L) {
                runCatching {
                    roomDataSource.updateBurdenCacheByLegacyId(
                        legacyId = analysis.todo.id,
                        burdenLevel = analysis.burdenLevel,
                        burdenGroupKey = analysis.burdenGroupKey,
                        burdenScore = analysis.burdenScore,
                        burdenReasonJson = analysis.burdenReasonJson
                    )
                }
            }
        }
    }

    suspend fun restoreTodosFromBackup(jsonText: String): Int {
        val envelope = runCatching {
            restoreJson.decodeFromString<TodoRestoreEnvelope>(jsonText)
        }.getOrElse { error ->
            throw IllegalArgumentException("Todo 복원 파일을 읽을 수 없습니다.", error)
        }

        val restoredTodos = envelope.todos
            .asReversed()
            .distinctBy { it.id }
            .asReversed()
            .mapNotNull { it.toEntity(userId) }

        roomDataSource.insertTodos(restoredTodos)
        return restoredTodos.size
    }

    private fun TodoRestoreEntry.toEntity(currentUserId: String): TodoEntity? {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return null

        val legacyIdValue = id.takeIf { it > 0L }
        val todoIdValue = legacyIdValue?.let { "legacy_todo_$it" } ?: UUID.randomUUID().toString()
        val resolvedCategory = category
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotEmpty() }
            ?: "NORMAL"
        val resolvedSelectedDate = selectedDate ?: dueDate
        val resolvedAccumulatedMillis = accumulatedMillis ?: (accumulatedSeconds * 1000L)

        return TodoEntity(
            todoId = todoIdValue,
            userId = currentUserId,
            title = cleanTitle,
            description = description,
            category = resolvedCategory,
            selectedDate = resolvedSelectedDate,
            isCompleted = isCompleted ?: isDone ?: false,
            completedAt = completedAt,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            scaleEstimate = scaleEstimate,
            scaleAlgorithmVersion = scaleAlgorithmVersion,
            accumulatedWorkMillis = resolvedAccumulatedMillis,
            burdenLevel = burdenLevel,
            burdenGroupKey = burdenGroupKey,
            burdenScore = burdenScore,
            burdenReasonJson = burdenReasonJson,
            reviewStage = reviewStage,
            reviewStage1CompletedAt = reviewStage1CompletedAt,
            legacyId = legacyIdValue,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncStatus = syncStatus ?: "PENDING"
        )
    }

    @Serializable
    private data class TodoRestoreEnvelope(
        val todos: List<TodoRestoreEntry> = emptyList()
    )

    @Serializable
    private data class TodoRestoreEntry(
        val userId: String? = null,
        val id: Long = 0L,
        val title: String = "",
        val description: String? = null,
        val category: String? = null,
        val selectedDate: Long? = null,
        val dueDate: Long? = null,
        val isCompleted: Boolean? = null,
        val isDone: Boolean? = null,
        val completedAt: Long? = null,
        val deletedAt: Long? = null,
        val isDeleted: Boolean = false,
        val accumulatedSeconds: Long = 0L,
        val accumulatedMillis: Long? = null,
        val burdenLevel: String? = null,
        val burdenGroupKey: String? = null,
        val burdenScore: Int = 0,
        val burdenReasonJson: String? = null,
        val reviewStage: Int = 0,
        val reviewStage1CompletedAt: Long? = null,
        val scaleEstimate: String? = null,
        val scaleAlgorithmVersion: String? = null,
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis(),
        val syncStatus: String? = null
    )

    suspend fun completeReviewTodo(todo: TodoItem) {
        val now = System.currentTimeMillis()
        val updated = when (todo.reviewStage) {
            0 -> todo.copy(isCompleted = true, reviewStage = 1, reviewStage1CompletedAt = now, updatedAt = now)
            1 -> todo.copy(isCompleted = true, reviewStage = 2, completedAt = now, updatedAt = now)
            else -> return
        }
        runCatching { roomDataSource.update(updated, userId) }
        runCatching {
            eventLogRepository.log(
                eventType = EventType.TODO_REVIEW_ADVANCED,
                entityType = EntityType.TODO,
                entityId = todo.id.toString()
            )
        }
    }
}
