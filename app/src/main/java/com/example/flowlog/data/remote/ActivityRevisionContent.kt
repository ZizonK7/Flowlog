package com.example.flowlog.data.remote

import com.example.flowlog.data.model.ActivitySession

/** Compare every field written by the uploader, excluding bookkeeping and derived aliases. */
object ActivityRevisionContent {
    fun mutationId(docId: String, activity: ActivitySession, deletedAt: Long?): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest((docId + activity.copy(remoteRevision = 0L, localActivityId = null).toString() + deletedAt).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private val metadata = setOf("modifiedTime", "revision", "lastMutationId")
    private val defaults: Map<String, Any?> = mapOf(
        "note" to null, "tags" to emptyList<String>(), "exerciseSets" to emptyList<Any>(),
        "isFavorite" to false, "linkedTodoId" to null, "linkedPetiteId" to null,
        "sourceType" to "MANUAL", "sourceId" to null, "originalCategory" to null,
        "isDeleted" to false, "deletedAt" to null
    )

    fun matches(remote: Map<String, Any?>?, local: Map<String, Any?>): Boolean {
        if (remote == null) return false
        // Optional web aliases may be absent on older documents, but cannot contradict the record.
        if (remote.containsKey("start_time") && !equal(remote["start_time"], local["startTime"])) return false
        if (remote.containsKey("end_time") && !equal(remote["end_time"], local["endTime"])) return false
        if (remote.containsKey("durationMinutes") &&
            !equal(remote["durationMinutes"], (local["durationMillis"] as? Number)?.toDouble()?.div(60000.0))) return false
        return local.all { (key, value) ->
            // The uploader preserves the server's original classification when present.
            key in metadata || (key == "originalCategory" && remote[key] is String) ||
                equal(value, if (remote.containsKey(key)) remote[key] else defaults[key])
        }
    }

    private fun equal(a: Any?, b: Any?): Boolean = when {
        a is Number && b is Number -> a.toString().toBigDecimal().compareTo(b.toString().toBigDecimal()) == 0
        a is List<*> && b is List<*> -> a.size == b.size && a.indices.all { equal(a[it], b[it]) }
        a is Map<*, *> && b is Map<*, *> -> (a.keys + b.keys).all { equal(a[it], b[it]) }
        else -> a == b
    }
}
