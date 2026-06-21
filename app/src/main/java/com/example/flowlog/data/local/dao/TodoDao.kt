package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

// OnConflictStrategy.REPLACE: 마이그레이션 재실행 또는 동기화 시 upsert 처리.
@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<TodoEntity>)

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Query("SELECT * FROM todos WHERE todoId = :todoId LIMIT 1")
    suspend fun getTodoById(todoId: String): TodoEntity?

    // 전체 Todo 조회 (completed 포함, 삭제된 것 제외) — getAllTodos() 읽기 경로용
    @Query("""
        SELECT * FROM todos
        WHERE userId = :userId
          AND isDeleted = 0
        ORDER BY isCompleted ASC, createdAt DESC
    """)
    fun observeAllTodos(userId: String): Flow<List<TodoEntity>>

    // 미완료 Todo만 조회 — getIncompleteTodos() 읽기 경로용
    // REVIEW stage=1 예외 없이 isCompleted = 0만 필터링 (기존 동작과 동일)
    @Query("""
        SELECT * FROM todos
        WHERE userId = :userId
          AND isDeleted = 0
          AND isCompleted = 0
        ORDER BY createdAt DESC
    """)
    fun observeIncompleteTodos(userId: String): Flow<List<TodoEntity>>

    // 기존 앱 동작 유지:
    // - 미완료 Todo는 항상 포함
    // - REVIEW 카테고리 stage=1(D+1 완료, D+7 대기) 항목은 isCompleted=true여도 active로 취급
    @Query("""
        SELECT * FROM todos
        WHERE userId = :userId
          AND isDeleted = 0
          AND (
            isCompleted = 0
            OR (category = 'REVIEW' AND reviewStage = 1)
          )
        ORDER BY isCompleted ASC, createdAt DESC
    """)
    fun observeActiveTodos(userId: String): Flow<List<TodoEntity>>

    // selectedDate 기준 오늘 할 일 조회 (selectedDate 우선, null이면 제외)
    @Query("""
        SELECT * FROM todos
        WHERE userId = :userId
          AND isDeleted = 0
          AND selectedDate >= :startOfDay
          AND selectedDate < :endOfDay
        ORDER BY createdAt DESC
    """)
    fun observeTodayTodos(
        userId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<TodoEntity>>

    @Query("""
        SELECT * FROM todos
        WHERE userId = :userId
          AND isDeleted = 0
          AND selectedDate >= :startTime
          AND selectedDate < :endTime
        ORDER BY createdAt DESC
    """)
    fun getTodosByDateRange(
        userId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<TodoEntity>>

    // isDeleted = 1 항목도 포함: 오프라인 삭제가 Firestore에 반영되지 않는 문제 방지.
    // FirebaseSyncDataSource에서 isDeleted 여부를 보고 set/delete 분기 처리.
    @Query("""
        SELECT * FROM todos
        WHERE userId = :userId
          AND syncStatus = '${SyncStatus.PENDING}'
    """)
    suspend fun getUnsyncedTodos(userId: String): List<TodoEntity>

        @Query("""
                SELECT * FROM todos
                WHERE userId = :userId
                    AND syncStatus = '${SyncStatus.PENDING}'
                    AND createdAt < :cutoffMillis
        """)
        suspend fun getEligibleUnsyncedTodos(userId: String, cutoffMillis: Long): List<TodoEntity>

    @Query("""
        UPDATE todos
        SET syncStatus = '${SyncStatus.SYNCED}'
        WHERE todoId = :todoId
    """)
    suspend fun markTodoSynced(todoId: String)

    @Query("""
        UPDATE todos
        SET isCompleted = 1,
            completedAt = :completedAt,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE todoId = :todoId
    """)
    suspend fun completeTodo(todoId: String, completedAt: Long, updatedAt: Long)

    @Query("""
        UPDATE todos
        SET isCompleted = 0,
            completedAt = NULL,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE todoId = :todoId
    """)
    suspend fun uncompleteTodo(todoId: String, updatedAt: Long)

    // Soft delete: 추천 후보, 통계, 목록에서 완전히 제외됨
    @Query("""
        UPDATE todos
        SET isDeleted = 1,
            deletedAt = :deletedAt,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE todoId = :todoId
    """)
    suspend fun softDeleteTodo(todoId: String, deletedAt: Long, updatedAt: Long)

    @Query("""
        UPDATE todos
        SET isDeleted = 1,
            deletedAt = :deletedAt,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.SYNCED}'
        WHERE todoId = :todoId
    """)
    suspend fun markTodoDeletedFromRemote(todoId: String, deletedAt: Long, updatedAt: Long)

    @Query("""
        UPDATE todos
        SET accumulatedWorkMillis = :accumulatedWorkMillis,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE todoId = :todoId
    """)
    suspend fun updateAccumulatedWorkMillis(
        todoId: String,
        accumulatedWorkMillis: Long,
        updatedAt: Long
    )

    // delta 누적: addAccumulatedSeconds shadow write 시 현재값을 조회 없이 원자적으로 증감
    @Query("""
        UPDATE todos
        SET accumulatedWorkMillis = accumulatedWorkMillis + :deltaMillis,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE todoId = :todoId
          AND isDeleted = 0
    """)
    suspend fun addToAccumulatedWorkMillis(todoId: String, deltaMillis: Long, updatedAt: Long)

    @Query("""
        UPDATE todos
        SET burdenLevel = :burdenLevel,
            burdenGroupKey = :burdenGroupKey,
            burdenScore = :burdenScore,
            burdenReasonJson = :burdenReasonJson,
            updatedAt = :updatedAt,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE todoId = :todoId
          AND isDeleted = 0
    """)
    suspend fun updateBurdenCache(
        todoId: String,
        burdenLevel: String,
        burdenGroupKey: String,
        burdenScore: Int,
        burdenReasonJson: String,
        updatedAt: Long
    )


    @Query("""
        SELECT COUNT(*) FROM todos
        WHERE userId = :userId
          AND isDeleted = 0
    """)
    suspend fun getActiveTodosCount(userId: String): Int

    @Query("UPDATE todos SET userId = :newUserId, syncStatus = 'PENDING' WHERE userId = 'anonymous'")
    suspend fun reassignAnonymousUser(newUserId: String): Int

    // ── 알림 전용 조회 (userId 필터 없음 — 알림은 auth 상태와 무관하게 동작) ─────

    // TodoReminderReceiver: legacyId로 단건 조회. 완료·삭제 여부는 호출부에서 확인.
    @Query("SELECT * FROM todos WHERE legacyId = :legacyId AND isDeleted = 0 LIMIT 1")
    suspend fun getTodoByLegacyId(legacyId: Long): TodoEntity?

    // TodoReminderBootReceiver: 재예약 대상 전체 조회.
    @Query("SELECT * FROM todos WHERE isDeleted = 0 AND isCompleted = 0 ORDER BY createdAt DESC")
    suspend fun getActiveTodosForReminder(): List<TodoEntity>
}
