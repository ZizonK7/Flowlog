package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.entity.ExamStrategyCheckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamStrategyCheckDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: ExamStrategyCheckEntity)

    @Query("""
        SELECT * FROM exam_strategy_checks
        WHERE userId = :userId
        ORDER BY checkedAtMillis DESC
    """)
    fun observeAllChecks(userId: String): Flow<List<ExamStrategyCheckEntity>>

    @Query("""
        SELECT * FROM exam_strategy_checks
        WHERE userId = :userId
          AND examTodoLegacyId = :examTodoLegacyId
    """)
    fun observeChecksForExam(userId: String, examTodoLegacyId: Long): Flow<List<ExamStrategyCheckEntity>>

    @Query("""
        SELECT * FROM exam_strategy_checks
        WHERE userId = :userId
          AND syncStatus = '${SyncStatus.PENDING}'
    """)
    suspend fun getUnsyncedChecks(userId: String): List<ExamStrategyCheckEntity>

    @Query("UPDATE exam_strategy_checks SET syncStatus = '${SyncStatus.SYNCED}' WHERE checkId = :checkId")
    suspend fun markCheckSynced(checkId: String)

    // undo: undoneAtMillis를 기록하고 PENDING으로 되돌려 Firebase 재동기화 트리거
    @Query("""
        UPDATE exam_strategy_checks
        SET undoneAtMillis = :undoneAtMillis,
            syncStatus = '${SyncStatus.PENDING}'
        WHERE checkId = :checkId
    """)
    suspend fun markUndone(checkId: String, undoneAtMillis: Long)

    @Query("SELECT * FROM exam_strategy_checks WHERE checkId = :checkId LIMIT 1")
    suspend fun getCheckById(checkId: String): ExamStrategyCheckEntity?
}
