package com.example.flowlog.data.remote

/** Pure transaction decision, shared by the actual uploader and JVM regression tests. */
object ActivityRevisionPolicy {
    fun next(docId: String, base: Long, remote: Long, mutationId: String, remoteMutationId: String?): Long {
        if (remoteMutationId == mutationId) return remote
        if (base != remote) throw ActivityRevisionConflictException(docId, base, remote)
        return remote + 1
    }
}
