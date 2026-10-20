package com.example.flowlog.data.sync

import android.content.Context
import android.util.Log
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.dao.TodoDao
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.TodoEntity
import com.example.flowlog.data.remote.awaitResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulls Todos changed on the server (PC web app, another device) into Room after each
 * periodic sync, the same way activities are pulled: incremental on `updatedAt` with
 * [RemotePullCursor] (24 h overlap, a full read on first use and weekly). Decisions are in
 * [TodoMergePolicy].
 *
 * The web deletes a Todo by deleting its document, so deletions are found by absence:
 * - full read: a SYNCED phone row with no document is deleted on the phone;
 * - incremental read: the open (incomplete) documents are also read, and an open SYNCED
 *   phone row missing from them and from the changed set is checked with a direct read
 *   before it is deleted.
 * Rows still PENDING (not uploaded yet) are never deleted here.
 */
class TodoRemotePull(context: Context) {
    private val appContext = context.applicationContext
    private val todoDao = FlowlogDatabase.getInstance(appContext).todoDao()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun pull(userId: String): RestoreSection = withContext(Dispatchers.IO) {
        check(FirebaseAuth.getInstance().currentUser?.uid == userId) { "Account changed" }
        val collection = TodoSyncSupport.collection(firestore, userId)
        val pullStartedAt = System.currentTimeMillis()
        val since = RemotePullCursor.since(appContext, userId, TodoSyncSupport.CURSOR_KEY, pullStartedAt)

        val changed = (if (since != null) collection.whereGreaterThanOrEqualTo("updatedAt", since) else collection)
            .get().awaitResult().documents
        val open = if (since != null) collection.whereEqualTo("isCompleted", false).get().awaitResult().documents else emptyList()
        check(FirebaseAuth.getInstance().currentUser?.uid == userId) { "Account changed" }

        var inserted = 0
        var skipped = 0
        var failed = 0
        val byId = LinkedHashMap<String, DocumentSnapshot>()
        (changed + open).forEach { byId[it.id] = it }
        byId.values.forEach { doc ->
            runCatching {
                when (TodoSyncSupport.applyRemote(appContext, todoDao, firestore, userId, doc)) {
                    true -> inserted++
                    false -> skipped++
                }
            }.onFailure { e ->
                failed++
                Log.w(TAG, "Todo pull item failed: ${doc.id} — ${e.javaClass.simpleName}")
            }
        }

        // Deletions on the server.
        val locals = todoDao.getSyncedLiveTodos(userId)
        val candidates = TodoMergePolicy.deletionCandidates(
            locals = locals.map(TodoSyncSupport::toLocal),
            presentIds = byId.keys,
            full = since == null,
            pullStartedAt = pullStartedAt
        )
        var deleted = 0
        candidates.forEach { candidate ->
            runCatching {
                if (since != null) {
                    val snapshot = collection.document(candidate.docId).get().awaitResult()
                    if (snapshot.exists()) {
                        TodoSyncSupport.applyRemote(appContext, todoDao, firestore, userId, snapshot)
                        return@runCatching
                    }
                }
                // Keep updatedAt: if the document comes back unchanged, rule 1 restores the row.
                todoDao.markTodoDeletedFromRemote(candidate.todoId, pullStartedAt, candidate.updatedAt)
                deleted++
            }.onFailure { e ->
                failed++
                Log.w(TAG, "Todo deletion check failed: ${candidate.todoId} — ${e.javaClass.simpleName}")
            }
        }

        Log.i(TAG, "Todos pulled: fetched=${byId.size} applied=$inserted unchanged=$skipped deleted=$deleted failed=$failed incremental=${since != null}")
        if (failed == 0) {
            RemotePullCursor.recordSuccess(appContext, userId, TodoSyncSupport.CURSOR_KEY, pullStartedAt, wasFullPull = since == null)
        }
        RestoreSection(fetched = byId.size, inserted = inserted + deleted, skipped = skipped, failed = failed)
    }

    private companion object {
        const val TAG = "TodoRemotePull"
    }
}

/** Shared by the pull and the Todo upload (FirebaseSyncDataSource). */
internal object TodoSyncSupport {
    const val CURSOR_KEY = "todos"
    private const val PREFS = "flowlog_todo_sync"
    private const val KEY_DONE_PREFIX = "done:"

    fun collection(firestore: FirebaseFirestore, userId: String) =
        firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("todos")

    fun toLocal(entity: TodoEntity) = LocalTodo(
        todoId = entity.todoId,
        docId = TodoMergePolicy.docIdFor(entity.todoId, entity.legacyId),
        updatedAt = entity.updatedAt,
        isCompleted = entity.isCompleted,
        completedAt = entity.completedAt,
        isDeleted = entity.isDeleted,
        pending = entity.syncStatus == SyncStatus.PENDING,
        calendarOnly = entity.calendarSourceId != null || entity.todoId.startsWith("calendar_")
    )

    fun toRemote(doc: DocumentSnapshot) = RemoteTodo(
        docId = doc.id,
        updatedAt = doc.getLong("updatedAt"),
        isCompleted = doc.getBoolean("isCompleted") ?: false,
        completedAt = doc.getLong("completedAt")
    )

    /**
     * Before uploading a PENDING row: merge with the server copy so a newer web change
     * (a completion above all) is not overwritten. Returns the row to upload, or null when
     * the server copy won and was applied to Room instead.
     */
    suspend fun reconcileBeforeUpload(
        context: Context,
        todoDao: TodoDao,
        userId: String,
        entity: TodoEntity,
        doc: DocumentSnapshot
    ): TodoEntity? {
        val remote = toRemote(doc)
        val action = TodoMergePolicy.merge(toLocal(entity), remote, completionOnServer(context, entity.todoId))
        rememberServerCompletion(context, entity.todoId, remote.isCompleted, remote.completedAt)
        return when (action) {
            is TodoMergeAction.ApplyRemote -> {
                val merged = fromDocument(doc, userId, entity.todoId, base = entity, numericId = entity.legacyId)
                if (!action.keepLocalCompletion) {
                    todoDao.updateTodo(merged)
                    null
                } else {
                    merged.copy(
                        isCompleted = true,
                        completedAt = entity.completedAt,
                        updatedAt = maxOf(System.currentTimeMillis(), merged.updatedAt + 1L),
                        syncStatus = SyncStatus.PENDING
                    ).also { todoDao.updateTodo(it) }
                }
            }
            is TodoMergeAction.KeepLocal -> if (action.adoptRemoteCompletion) {
                entity.copy(isCompleted = true, completedAt = remote.completedAt).also { todoDao.updateTodo(it) }
            } else {
                entity
            }
            else -> entity
        }
    }

    /** completedAt the server is known to hold for this row (see TodoMergePolicy rule 2). */
    fun completionOnServer(context: Context, todoId: String): Long? =
        prefs(context).getLong(KEY_DONE_PREFIX + todoId, -1L).takeIf { it >= 0L }

    /** Records what the server now holds: a completion time, or none. */
    fun rememberServerCompletion(context: Context, todoId: String, isCompleted: Boolean, completedAt: Long?) {
        val editor = prefs(context).edit()
        if (isCompleted && completedAt != null) editor.putLong(KEY_DONE_PREFIX + todoId, completedAt)
        else editor.remove(KEY_DONE_PREFIX + todoId)
        editor.apply()
    }

    /**
     * Merges one server document into Room. Returns true when the phone row changed.
     * Documents without a numeric id (made on the web) are adopted first.
     */
    suspend fun applyRemote(
        context: Context,
        todoDao: TodoDao,
        firestore: FirebaseFirestore,
        userId: String,
        doc: DocumentSnapshot
    ): Boolean {
        // Calendar-only rows the phone once uploaded ("calendar_..." ids) belong to the calendar pull.
        if (TodoMergePolicy.isCalendarDocument(doc.id)) return false
        val numericId = (doc.get("id") as? Number)?.toLong()
        if (TodoMergePolicy.needsAdoption(doc.id, numericId)) {
            val adopted = adopt(context, todoDao, firestore, userId, doc) ?: return false
            return applyRemote(context, todoDao, firestore, userId, adopted)
        }
        val todoId = TodoMergePolicy.todoIdFor(doc.id, numericId)
        val existing = todoDao.getTodoById(todoId)
        val remote = toRemote(doc)
        val action = TodoMergePolicy.merge(existing?.let(::toLocal), remote, completionOnServer(context, todoId))
        rememberServerCompletion(context, todoId, remote.isCompleted, remote.completedAt)
        return when (action) {
            TodoMergeAction.InsertRemote -> {
                todoDao.insertTodo(fromDocument(doc, userId, todoId, base = null, numericId = numericId))
                true
            }
            is TodoMergeAction.ApplyRemote -> {
                val base = existing ?: return false
                var merged = fromDocument(doc, userId, todoId, base = base, numericId = numericId)
                if (action.keepLocalCompletion) {
                    // Newer than the server copy now, so the next upload sends the merge.
                    merged = merged.copy(
                        isCompleted = true,
                        completedAt = base.completedAt,
                        updatedAt = maxOf(System.currentTimeMillis(), merged.updatedAt + 1L),
                        syncStatus = SyncStatus.PENDING
                    )
                }
                todoDao.updateTodo(merged)
                true
            }
            is TodoMergeAction.KeepLocal -> {
                val base = existing ?: return false
                if (!action.adoptRemoteCompletion) return false
                todoDao.updateTodo(base.copy(isCompleted = true, completedAt = remote.completedAt))
                true
            }
            TodoMergeAction.ReuploadLocal -> {
                val base = existing ?: return false
                todoDao.updateTodo(base.copy(syncStatus = SyncStatus.PENDING))
                true
            }
            TodoMergeAction.Ignore -> false
        }
    }

    /**
     * Gives a web-made document (random id, `id: 0`) a numeric id the phone can key on:
     * one transaction writes the same Todo under the new id and deletes the old document.
     * Returns the new document, or null when it was already adopted or the old phone row
     * still has unsent changes (the upload goes first; adoption waits for the next pull).
     */
    private suspend fun adopt(
        context: Context,
        todoDao: TodoDao,
        firestore: FirebaseFirestore,
        userId: String,
        doc: DocumentSnapshot
    ): DocumentSnapshot? {
        val oldRow = todoDao.getTodoById(doc.id)
        if (oldRow?.syncStatus == SyncStatus.PENDING) return null
        val collection = collection(firestore, userId)
        val newId = allocateLegacyId(todoDao)
        val newRef = collection.document(newId.toString())
        val oldRef = collection.document(doc.id)
        val now = System.currentTimeMillis()
        val moved = firestore.runTransaction { tx ->
            val current = tx.get(oldRef)
            if (!current.exists()) return@runTransaction false
            check(!tx.get(newRef).exists()) { "Todo id already used" }
            val data = HashMap(current.data.orEmpty())
            data["id"] = newId
            data["updatedAt"] = now
            tx.set(newRef, data)
            tx.delete(oldRef)
            true
        }.awaitResult()
        if (!moved) return null
        if (oldRow != null) todoDao.deleteTodoRow(oldRow.todoId)
        Log.i("TodoRemotePull", "Adopted a web Todo under a numeric id")
        return newRef.get().awaitResult()
    }

    private suspend fun allocateLegacyId(todoDao: TodoDao): Long {
        var candidate = maxOf(System.currentTimeMillis(), (todoDao.getMaxLegacyId() ?: 0L) + 1L)
        while (todoDao.getTodoById(TodoMergePolicy.LEGACY_PREFIX + candidate) != null) candidate++
        return candidate
    }

    /** The server copy as a Room row; fields the server does not carry come from [base]. */
    fun fromDocument(doc: DocumentSnapshot, userId: String, todoId: String, base: TodoEntity?, numericId: Long?): TodoEntity {
        val now = System.currentTimeMillis()
        val template = base ?: TodoEntity(todoId = todoId, userId = userId, title = "")
        return template.copy(
            userId = userId,
            title = doc.getString("title") ?: template.title,
            category = doc.getString("category") ?: template.category,
            selectedDate = doc.getLong("selectedDate"),
            isCompleted = doc.getBoolean("isCompleted") ?: false,
            completedAt = doc.getLong("completedAt"),
            isDeleted = false,
            deletedAt = null,
            // The server stores accumulatedSeconds (seconds); Room keeps milliseconds.
            accumulatedWorkMillis = (doc.getLong("accumulatedSeconds") ?: 0L) * 1000L,
            burdenLevel = doc.getString("burdenLevel"),
            burdenGroupKey = doc.getString("burdenGroupKey"),
            burdenScore = (doc.getLong("burdenScore") ?: 0L).toInt(),
            burdenReasonJson = doc.getString("burdenReasonJson"),
            legacyId = numericId?.takeIf { it != 0L } ?: template.legacyId,
            createdAt = doc.getLong("createdAt") ?: base?.createdAt ?: now,
            updatedAt = doc.getLong("updatedAt") ?: base?.updatedAt ?: now,
            syncStatus = SyncStatus.SYNCED
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
