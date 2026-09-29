package com.example.flowlog.data.sync

import android.content.Context
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.StudyDecisionEntity
import com.example.flowlog.data.local.entity.StudyLinkEntity
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.remote.awaitResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

/** Private collections are outside the legacy admin-readable flowlog subtree. */
class StudySyncDataSource(context: Context) {
    private val dao = FlowlogDatabase.getInstance(context).studyDao()
    private val auth = FirebaseAuth.getInstance()
    private val remote = FirebaseFirestore.getInstance()
    private fun checkOwner(uid: String) = check(auth.currentUser?.uid == uid) { "Account changed" }
    private fun collection(uid: String, name: String) = remote.collection("users").document(uid).collection(name)

    suspend fun sync(uid: String, restoreRemote: Boolean = true): SyncOutcome {
        checkOwner(uid)
        var ok = 0
        var failed = 0
        for (d in dao.getPendingDecisions(uid)) {
            runCatching {
                checkOwner(uid)
                val ref = collection(uid, "interactionDecisions").document(d.decisionId)
                val value = mapOf(
                    "schemaVersion" to 1, "decisionId" to d.decisionId, "proposalId" to d.proposalId,
                    "kind" to d.kind, "activityId" to d.activityId, "category" to d.category,
                    "localDate" to d.localDate, "minuteOfDay" to d.minuteOfDay, "weekday" to d.weekday,
                    "outcome" to d.outcome, "createdAt" to d.createdAt, "revision" to 1,
                    "payload" to jsonObject(JSONObject(d.payloadJson))
                )
                remote.runTransaction { tx ->
                    checkOwner(uid)
                    val existing = tx.get(ref)
                    if (!existing.exists()) tx.set(ref, value)
                    null
                }.awaitResult()
                checkOwner(uid)
                dao.markDecisionSynced(uid, d.decisionId)
                ok++
            }.onFailure { failed++ }
        }
        // Rebuild recommendations and historical lesson details after reinstall or web edits.
        if (restoreRemote) runCatching { restore(uid) }.onFailure { failed++ }
        return SyncOutcome(attemptedCount = ok + failed, successCount = ok, failureCount = failed)
    }

    suspend fun restore(uid: String) {
        checkOwner(uid)
        var malformed = 0
        val links = collection(uid, "activityStudyLinks").get().awaitResult()
        checkOwner(uid)
        for (doc in links.documents) {
            val d = doc.data ?: continue
            runCatching {
            dao.restoreLink(StudyLinkEntity(
                linkId = doc.id, userId = uid, activityId = d["activityId"] as String,
                courseId = d["courseId"] as String, lessonId = d["lessonId"] as String,
                lessonDate = d["lessonDate"] as String, phase = d["phase"] as String,
                segmentsJson = JSONArray(d["segments"] as List<*>).toString(),
                snapshotJson = JSONObject(d["snapshot"] as Map<*, *>).toString(),
                sourceVersion = d["sourceVersion"] as String,
                confirmedAt = (d["confirmedAt"] as Number).toLong(), updatedAt = (d["updatedAt"] as Number).toLong(),
                remoteRevision = (d["revision"] as Number).toLong(), deletedAt = (d["deletedAt"] as? Number)?.toLong(),
                syncStatus = SyncStatus.SYNCED
            ))
            }.onFailure { malformed++ }
        }
        val decisions = collection(uid, "interactionDecisions").get().awaitResult()
        checkOwner(uid)
        for (doc in decisions.documents) {
            val d = doc.data ?: continue
            runCatching {
            dao.insertDecisionIfAbsent(StudyDecisionEntity(
                decisionId = doc.id, userId = uid, proposalId = d["proposalId"] as String,
                kind = d["kind"] as String, activityId = d["activityId"] as? String, category = d["category"] as? String,
                localDate = d["localDate"] as String, minuteOfDay = (d["minuteOfDay"] as Number).toInt(),
                weekday = (d["weekday"] as Number).toInt(), outcome = d["outcome"] as String,
                createdAt = (d["createdAt"] as Number).toLong(),
                payloadJson = JSONObject(d["payload"] as Map<*, *>).toString(), syncStatus = SyncStatus.SYNCED
            ))
            }.onFailure { malformed++ }
        }
        check(malformed == 0) { "$malformed study documents could not be restored; valid documents were retained" }
    }

    private fun jsonObject(o: JSONObject): Map<String, Any?> = o.keys().asSequence().associateWith { key -> jsonValue(o.get(key)) }
    private fun jsonValue(v: Any?): Any? = when (v) {
        JSONObject.NULL -> null
        is JSONObject -> jsonObject(v)
        is JSONArray -> (0 until v.length()).map { jsonValue(v.get(it)) }
        else -> v
    }
}
