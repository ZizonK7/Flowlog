package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.constants.EventSource
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.EventLogEntity
import com.example.flowlog.data.sync.RemoteTimerSync
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

class EventLogRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = FlowlogDatabase.getInstance(context).eventLogDao()
    private val auth = FirebaseAuth.getInstance()

    @Suppress("DEPRECATION")
    private val appVersion: String? = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) { null }

    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    /**
     * Which install of the app wrote the row. Reuses the random per-install id that
     * [RemoteTimerSync.deviceId] already keeps for the shared timer (not a hardware id;
     * a reinstall or a device wipe makes a new one).
     */
    private val installationId: String? by lazy {
        runCatching { RemoteTimerSync.deviceId(appContext) }.getOrNull()
    }

    suspend fun log(
        eventType: String,
        entityType: String? = null,
        entityId: String? = null,
        source: String = EventSource.APP,
        metadataJson: String? = null,
        algorithmVersion: String? = null
    ) {
        val now = System.currentTimeMillis()
        dao.insertEvent(
            EventLogEntity(
                eventId = UUID.randomUUID().toString(),
                userId = userId,
                installationId = installationId,
                eventType = eventType,
                entityType = entityType,
                entityId = entityId,
                source = source,
                metadataJson = metadataJson,
                appVersion = appVersion,
                algorithmVersion = algorithmVersion,
                timestamp = now,
                createdAt = now
            )
        )
    }
}
