package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.local.FocusModeStore
import com.example.flowlog.data.repository.EventLogRepository
import com.example.flowlog.widget.FlowStatusWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusModeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dndEnabled = FocusModeStore.getEnableSystemDndForFocus(context)
        // DND 먼저 복원 → 그래야 종료 알림이 정상 DND 필터를 거쳐 울릴 수 있음
        FocusDndController.restoreDnd(context)
        FocusModeStore.clearFocusMode(context)
        FlowStatusWidgetProvider.updateAll(context)
        ActivityTimerNotifier(context).showFocusModeEnded()
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                EventLogRepository(context).log(
                    eventType = EventType.FOCUS_MODE_STOPPED,
                    entityType = EntityType.FOCUS_MODE,
                    metadataJson = """{"dnd_enabled":$dndEnabled,"reason":"expired"}"""
                )
            }
            pendingResult.finish()
        }
    }
}
