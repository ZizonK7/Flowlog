package com.example.flowlog.data.local

import android.content.Context
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.dao.TodoDao
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.TodoEntity
import com.example.flowlog.data.local.mapper.toTodoEntity
import com.example.flowlog.data.local.mapper.toTodoItem
import com.example.flowlog.data.model.TodoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room 기반 Todo 로컬 데이터 소스. TodoRepository의 primary local store.
 */
class RoomTodoLocalDataSource(context: Context) {

    private val dao: TodoDao = FlowlogDatabase.getInstance(context).todoDao()

    /**
     * Todo 저장. 반환값은 Room에서 부여된 todoId (String).
     */
    suspend fun insert(todo: TodoItem, userId: String): String {
        val entity = todo.toTodoEntity(userId)
        dao.insertTodo(entity)
        return entity.todoId
    }

    suspend fun insertTodos(todos: List<TodoEntity>) {
        if (todos.isEmpty()) return
        dao.insertTodos(todos)
    }

    /**
     * Todo 전체 필드 업데이트.
     * legacyId가 있으면 "legacy_todo_${id}", 캘린더 출처면 calendarSourceId로 조회.
     */
    suspend fun update(todo: TodoItem, userId: String) {
        val todoId = when {
            todo.id != 0L -> "legacy_todo_${todo.id}"
            todo.calendarSourceId != null -> dao.getTodoByCalendarSourceId(userId, todo.calendarSourceId)?.todoId ?: return
            else -> return
        }
        val existing = dao.getTodoById(todoId)
        if (existing == null) {
            // 엔티티가 Room에 없으면 (migration 미완료 등) upsert로 삽입
            dao.insertTodo(todo.toTodoEntity(userId))
            return
        }
        dao.updateTodo(
            existing.copy(
                title = todo.title,
                category = todo.category.name,
                selectedDate = todo.selectedDate,
                isCompleted = todo.isCompleted,
                completedAt = todo.completedAt,
                accumulatedWorkMillis = todo.accumulatedSeconds * 1000L,
                burdenLevel = todo.burdenLevel,
                burdenGroupKey = todo.burdenGroupKey,
                burdenScore = todo.burdenScore,
                burdenReasonJson = todo.burdenReasonJson,
                reviewStage = todo.reviewStage,
                reviewStage1CompletedAt = todo.reviewStage1CompletedAt,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
        )
    }

    suspend fun completeTodo(todoId: String) {
        val now = System.currentTimeMillis()
        dao.completeTodo(todoId, now, now)
    }

    suspend fun completeTodoByLegacyId(legacyId: Long) {
        completeTodo("legacy_todo_$legacyId")
    }

    suspend fun softDelete(todoId: String) {
        val now = System.currentTimeMillis()
        dao.softDeleteTodo(todoId, now, now)
    }

    suspend fun softDeleteByLegacyId(legacyId: Long) {
        softDelete("legacy_todo_$legacyId")
    }

    suspend fun softDeleteByCalendarSourceId(userId: String, calendarSourceId: String) {
        val now = System.currentTimeMillis()
        dao.softDeleteByCalendarSourceId(userId, calendarSourceId, now)
    }

    suspend fun completeByCalendarSourceId(userId: String, calendarSourceId: String) {
        dao.completeTodoByCalendarSourceId(userId, calendarSourceId, System.currentTimeMillis())
    }

    suspend fun uncompleteByCalendarSourceId(userId: String, calendarSourceId: String) {
        dao.uncompleteTodoByCalendarSourceId(userId, calendarSourceId, System.currentTimeMillis())
    }

    suspend fun uncompleteTodo(todoId: String) {
        dao.uncompleteTodo(todoId, System.currentTimeMillis())
    }

    suspend fun uncompleteTodoByLegacyId(legacyId: Long) {
        uncompleteTodo("legacy_todo_$legacyId")
    }

    suspend fun addToAccumulatedWorkMillisByLegacyId(legacyId: Long, deltaMillis: Long) {
        dao.addToAccumulatedWorkMillis(
            todoId = "legacy_todo_$legacyId",
            deltaMillis = deltaMillis,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun addToAccumulatedWorkMillisByCalendarSourceId(
        userId: String,
        calendarSourceId: String,
        deltaMillis: Long
    ) {
        dao.addToAccumulatedWorkMillisByCalendarSourceId(
            userId = userId,
            calendarSourceId = calendarSourceId,
            deltaMillis = deltaMillis,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun updateBurdenCacheByLegacyId(
        legacyId: Long,
        burdenLevel: String,
        burdenGroupKey: String,
        burdenScore: Int,
        burdenReasonJson: String
    ) {
        dao.updateBurdenCache(
            todoId = "legacy_todo_$legacyId",
            burdenLevel = burdenLevel,
            burdenGroupKey = burdenGroupKey,
            burdenScore = burdenScore,
            burdenReasonJson = burdenReasonJson,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 전체 Todo 실시간 관찰 (completed 포함, 삭제 제외). getAllTodos() 읽기 경로용.
     */
    fun observeAllTodos(userId: String): Flow<List<TodoItem>> {
        return dao.observeAllTodos(userId)
            .map { entities -> entities.map { it.toTodoItem() } }
    }

    /**
     * 미완료 Todo만 조회 (isCompleted = 0). getIncompleteTodos() 읽기 경로용.
     * REVIEW stage=1 예외 없이 단순 isCompleted = 0 필터링 (기존 동작 동일).
     */
    fun observeIncompleteTodos(userId: String): Flow<List<TodoItem>> {
        return dao.observeIncompleteTodos(userId)
            .map { entities -> entities.map { it.toTodoItem() } }
    }

    /**
     * 미완료 + REVIEW stage=1 Todo 실시간 관찰. 기존 앱 동작 유지.
     * isDeleted = 0 조건 포함.
     */
    fun observeActiveTodos(userId: String): Flow<List<TodoItem>> {
        return dao.observeActiveTodos(userId)
            .map { entities -> entities.map { it.toTodoItem() } }
    }

    /**
     * selectedDate 기준 오늘 Todo 실시간 관찰.
     */
    fun observeTodayTodos(
        userId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<TodoItem>> {
        return dao.observeTodayTodos(userId, startOfDay, endOfDay)
            .map { entities -> entities.map { it.toTodoItem() } }
    }

    /**
     * Firebase batch sync 대상 조회. Entity 그대로 반환 (sync 레이어에서 String todoId 필요).
     */
    suspend fun getUnsyncedTodos(userId: String): List<TodoEntity> {
        return dao.getUnsyncedTodos(userId)
    }

    suspend fun markSynced(todoId: String) {
        dao.markTodoSynced(todoId)
    }

    suspend fun updateAccumulatedWorkMillis(todoId: String, millis: Long) {
        dao.updateAccumulatedWorkMillis(todoId, millis, System.currentTimeMillis())
    }

    suspend fun updateAccumulatedWorkMillisByLegacyId(legacyId: Long, millis: Long) {
        updateAccumulatedWorkMillis("legacy_todo_$legacyId", millis)
    }
}
