package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.flowlog.data.local.TodoLocalDataSource

class TodoReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val scheduler = TodoReminderScheduler(context)
        TodoLocalDataSource.loadSnapshot(context)
            .filter { !it.isDone }
            .forEach { todo ->
                if (todo.createdAt + TodoReminderScheduler.INITIAL_DELAY_MILLIS > System.currentTimeMillis()) {
                    scheduler.scheduleInitialReminder(todo.id, todo.createdAt)
                } else {
                    scheduler.scheduleNextRandomReminder(todo.id)
                }
            }
    }
}
