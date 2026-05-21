package com.example.flowlog.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.flowlog.R
import com.example.flowlog.data.local.TodoLocalDataSource
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.ui.component.formatDuration

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodoWidgetRemoteViewsFactory(applicationContext)
    }
}

private class TodoWidgetRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private var todos: List<TodoItem> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        todos = TodoLocalDataSource.loadSnapshot(context)
            .filter { !it.isDone }
            .sortedByDescending { it.createdAt }
    }

    override fun onDestroy() {
        todos = emptyList()
    }

    override fun getCount(): Int = todos.size

    override fun getViewAt(position: Int): RemoteViews {
        val todo = todos.getOrNull(position)
            ?: return RemoteViews(context.packageName, R.layout.todo_widget_item)

        return RemoteViews(context.packageName, R.layout.todo_widget_item).apply {
            setTextViewText(R.id.todo_widget_item_title, todo.title)
            setTextViewText(R.id.todo_widget_item_time, formatDuration(todo.accumulatedMillis))
            setOnClickFillInIntent(
                R.id.todo_widget_item_start,
                todoActionIntent(TodoWidgetActionReceiver.ACTION_START_TODO, todo)
            )
            setOnClickFillInIntent(
                R.id.todo_widget_item_done,
                todoActionIntent(TodoWidgetActionReceiver.ACTION_TOGGLE_DONE, todo)
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = todos.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun todoActionIntent(action: String, todo: TodoItem): Intent {
        return Intent().apply {
            this.action = action
            putExtra(TodoWidgetActionReceiver.EXTRA_TODO_ID, todo.id)
            putExtra(TodoWidgetActionReceiver.EXTRA_TODO_TITLE, todo.title)
        }
    }
}
