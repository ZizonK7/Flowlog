package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.local.RoomTodoLocalDataSource
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.TodoBurdenAnalysis
import com.example.flowlog.data.sync.DeleteSyncTrigger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import java.util.concurrent.atomic.AtomicLong

class TodoRepository(context: Context) {
    private val appContext = context.applicationContext
    private val eventLogRepository = EventLogRepository(appContext)
    private val roomDataSource = RoomTodoLocalDataSource(appContext)

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
            runCatching { eventLogRepository.log(EventType.TODO_COMPLETED, EntityType.TODO, id.toString()) }
        } else {
            runCatching { roomDataSource.uncompleteTodoByLegacyId(id) }
            runCatching { eventLogRepository.log(EventType.TODO_UNCOMPLETED, EntityType.TODO, id.toString()) }
        }
    }

    suspend fun updateCompleted(todo: TodoItem, isCompleted: Boolean, completedAt: Long?) {
        val cid = todo.calendarSourceId
        if (cid != null) {
            if (isCompleted) {
                runCatching { roomDataSource.completeByCalendarSourceId(userId, cid) }
            } else {
                runCatching { roomDataSource.uncompleteByCalendarSourceId(userId, cid) }
            }
        } else {
            updateCompleted(todo.id, isCompleted, completedAt)
        }
    }

    suspend fun updateCompletedByCalendarSourceId(calendarSourceId: String, isCompleted: Boolean) {
        if (isCompleted) {
            runCatching { roomDataSource.completeByCalendarSourceId(userId, calendarSourceId) }
            runCatching { eventLogRepository.log(EventType.TODO_COMPLETED, EntityType.TODO, calendarSourceId) }
        } else {
            runCatching { roomDataSource.uncompleteByCalendarSourceId(userId, calendarSourceId) }
            runCatching { eventLogRepository.log(EventType.TODO_UNCOMPLETED, EntityType.TODO, calendarSourceId) }
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
        if (todo.calendarSourceId != null) {
            runCatching { roomDataSource.softDeleteByCalendarSourceId(userId, todo.calendarSourceId) }
        } else {
            runCatching { roomDataSource.softDeleteByLegacyId(todo.id) }
        }
        runCatching {
            eventLogRepository.log(
                eventType = EventType.TODO_DELETED,
                entityType = EntityType.TODO,
                entityId = todo.id.toString()
            )
        }
        DeleteSyncTrigger.trigger(appContext)
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

    suspend fun addAccumulatedSeconds(todo: TodoItem, seconds: Long) {
        if (todo.calendarSourceId != null) {
            addAccumulatedSecondsByCalendarSourceId(todo.calendarSourceId, seconds)
        } else {
            addAccumulatedSeconds(todo.id, seconds)
        }
    }

    suspend fun addAccumulatedSecondsByCalendarSourceId(calendarSourceId: String, seconds: Long) {
        if (seconds != 0L) {
            runCatching {
                roomDataSource.addToAccumulatedWorkMillisByCalendarSourceId(
                    userId = userId,
                    calendarSourceId = calendarSourceId,
                    deltaMillis = seconds * 1000L
                )
            }
        }
        runCatching {
            eventLogRepository.log(
                eventType = EventType.TODO_WORK_ADDED,
                entityType = EntityType.TODO,
                entityId = calendarSourceId
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
