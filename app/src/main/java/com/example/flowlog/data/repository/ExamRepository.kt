package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.local.dao.ExamStrategyCheckDao
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.ExamStrategyCheckEntity
import com.example.flowlog.data.constants.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.callbackFlow
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ExamRepository(context: Context) {

    private val dao: ExamStrategyCheckDao = FlowlogDatabase.getInstance(context).examStrategyCheckDao()

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    private fun userIdFlow() = callbackFlow {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.uid ?: "anonymous")
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid ?: "anonymous")
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    fun observeAllChecks() = userIdFlow().flatMapLatest { uid ->
        dao.observeAllChecks(uid)
    }

    /** 체크 삽입 후 생성된 checkId를 반환한다. */
    suspend fun insertCheck(
        examTodoLegacyId: Long,
        subjectTitleSnapshot: String,
        examDateMillis: Long,
        strategyDValue: Int,
        strategyLabelSnapshot: String,
        daysUntilExam: Int
    ): String {
        val now = System.currentTimeMillis()
        val checkId = UUID.randomUUID().toString()
        dao.insertCheck(
            ExamStrategyCheckEntity(
                checkId = checkId,
                userId = userId,
                examTodoLegacyId = examTodoLegacyId,
                subjectTitleSnapshot = subjectTitleSnapshot,
                examDateMillis = examDateMillis,
                strategyDValue = strategyDValue,
                strategyLabelSnapshot = strategyLabelSnapshot,
                checkedAtMillis = now,
                checkedOnDateKey = startOfDay(now),
                checkedOnDaysUntilExam = daysUntilExam,
                syncStatus = SyncStatus.PENDING
            )
        )
        return checkId
    }

    /** undoneAtMillis를 기록해서 해당 체크를 무효화한다. */
    suspend fun undoCheck(checkId: String) {
        dao.markUndone(checkId, System.currentTimeMillis())
    }

    suspend fun getUnsyncedChecks(userId: String) = dao.getUnsyncedChecks(userId)

    suspend fun markCheckSynced(checkId: String) = dao.markCheckSynced(checkId)

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
