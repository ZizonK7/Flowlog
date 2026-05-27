package com.example.flowlog.data.remote

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.TodoItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreSyncRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val uid: String?
        get() = auth.currentUser?.uid

    suspend fun syncActivities(activities: List<ActivitySession>) {
        val userId = uid ?: return
        activities.forEach { activity ->
            activityCollection(userId).document(activity.id.toString())
                .set(activity.toRemoteMap(), SetOptions.merge())
                .awaitResult()
        }
        markSynced(userId)
    }

    suspend fun syncActivity(activity: ActivitySession) {
        val userId = uid ?: return
        activityCollection(userId).document(activity.id.toString())
            .set(activity.toRemoteMap(), SetOptions.merge())
            .awaitResult()
        markSynced(userId)
    }

    suspend fun deleteActivity(id: Long) {
        val userId = uid ?: return
        activityCollection(userId).document(id.toString()).delete().awaitResult()
        markSynced(userId)
    }

    suspend fun syncTodos(todos: List<TodoItem>) {
        val userId = uid ?: return
        todos.forEach { todo ->
            todoCollection(userId).document(todo.id.toString())
                .set(todo.toRemoteMap(), SetOptions.merge())
                .awaitResult()
        }
        markSynced(userId)
    }

    suspend fun syncTodo(todo: TodoItem) {
        val userId = uid ?: return
        todoCollection(userId).document(todo.id.toString())
            .set(todo.toRemoteMap(), SetOptions.merge())
            .awaitResult()
        markSynced(userId)
    }

    suspend fun deleteTodo(id: Long) {
        val userId = uid ?: return
        todoCollection(userId).document(id.toString()).delete().awaitResult()
        markSynced(userId)
    }

    private fun activityCollection(userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("activitySessions")

    private fun todoCollection(userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("todos")

    private suspend fun markSynced(userId: String) {
        firestore.collection("users").document(userId)
            .collection("flowlog").document("metadata")
            .set(mapOf("updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .awaitResult()
    }

    private fun ActivitySession.toRemoteMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "category" to category,
        "title" to title,
        "startTime" to startTime,
        "endTime" to endTime,
        "durationMillis" to durationMillis,
        "note" to note,
        "tags" to tags,
        "isFavorite" to isFavorite,
        "linkedTodoId" to linkedTodoId,
        "modifiedTime" to modifiedTime
    )

    private fun TodoItem.toRemoteMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "category" to category.name,
        "isCompleted" to isCompleted,
        "createdAt" to createdAt,
        "completedAt" to completedAt,
        "selectedDate" to selectedDate,
        "accumulatedSeconds" to accumulatedSeconds,
        "updatedAt" to updatedAt
    )
}
