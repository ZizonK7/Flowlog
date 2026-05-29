package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.flowlog.data.local.db.FlowlogDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // TODO(알림 기능 활성화 전 필수 개선):
        //   현재 getActiveTodosForReminder()는 userId 필터가 없어 DB에 있는
        //   모든 계정의 active Todo를 재예약한다.
        //   한 기기에서 여러 계정 전환 시 다른 계정의 Todo 알림이 발사될 수 있음.
        //
        //   개선 방향:
        //     val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        //     todoDao().getActiveTodosForReminder(uid)  ← userId 파라미터 추가 필요
        //   주의: 부팅 직후 Firebase SDK가 캐시된 auth 상태를 복원하므로
        //   currentUser는 대부분 동기적으로 사용 가능하지만, 초기화 타이밍에 따라
        //   null일 수 있다. "anonymous" fallback 시 해당 userId 데이터만 재예약됨.

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val todos = runCatching {
                    FlowlogDatabase.getInstance(context).todoDao().getActiveTodosForReminder()
                }.getOrDefault(emptyList())

                val scheduler = TodoReminderScheduler(context)
                val now = System.currentTimeMillis()

                todos.forEach { entity ->
                    val legacyId = entity.legacyId ?: return@forEach
                    if (entity.createdAt + TodoReminderScheduler.INITIAL_DELAY_MILLIS > now) {
                        scheduler.scheduleInitialReminder(legacyId, entity.createdAt)
                    } else {
                        scheduler.scheduleNextRandomReminder(legacyId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in TodoReminderBootReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "TodoReminderBootReceiver"
    }
}
