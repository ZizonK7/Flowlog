package com.example.flowlog.data.remote

/** Pure transaction decision, shared by the actual uploader and JVM regression tests. */
object ActivityRevisionPolicy {
    fun next(docId: String, base: Long, remote: Long, mutationId: String, remoteMutationId: String?, remoteMatchesLocal: Boolean = false): Long {
        // A legacy row can have revision 0 even though the same content is already uploaded.
        // Acknowledge that snapshot without writing or incrementing the server revision.
        if (remoteMatchesLocal && remote >= base) return remote
        if (remoteMutationId == mutationId) return remote
        if (base != remote) throw ActivityRevisionConflictException(docId, base, remote)
        return remote + 1
    }
}
