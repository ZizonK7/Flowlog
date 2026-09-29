package com.example.flowlog.data.remote

class ActivityRevisionConflictException(
    val docId: String,
    val expectedRevision: Long,
    val remoteRevision: Long
) : IllegalStateException("Activity revision conflict: $docId expected=$expectedRevision remote=$remoteRevision")
