package com.example.flowlog.data.agent

import com.example.flowlog.data.remote.awaitResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RemoteAiDecisionProvider(
    private val endpointUrl: String,
    private val enabled: Boolean,
    private val fallback: AiDecisionProvider = MockAiDecisionProvider(),
    private val timeoutMillis: Long = 2_500L,
    private val client: RemoteAiDecisionClient = RemoteAiDecisionClient(endpointUrl, timeoutMillis)
) : AiDecisionProvider {
    override suspend fun rankAmbiguousItems(
        candidates: List<OrganizedPetite>,
        context: TodayOrganizerContext
    ): List<OrganizedPetite> {
        if (!enabled || endpointUrl.isBlank()) return fallback.rankAmbiguousItems(candidates, context)
        return runCatching {
            val orderedIds = client.rankAmbiguousItems(candidates, context)
            mergeRemoteOrder(candidates, orderedIds)
        }.getOrElse {
            fallback.rankAmbiguousItems(candidates, context)
        }
    }

    override suspend fun generateRecommendationReason(
        candidate: OrganizedPetite,
        context: TodayOrganizerContext
    ): AiRecommendationReason? {
        val local = fallback.generateRecommendationReason(candidate, context)
        if (!enabled || endpointUrl.isBlank()) return local
        return runCatching {
            val remote = client.generateRecommendationReason(candidate, context)
            AiRecommendationReason(
                aiComment = remote.aiComment?.takeIf { it.isNotBlank() } ?: local?.aiComment,
                estimatedMinutes = remote.estimatedMinutes
                    ?.takeIf { it in MIN_ESTIMATED_MINUTES..MAX_ESTIMATED_MINUTES }
                    ?: local?.estimatedMinutes,
                steps = remote.steps.takeIf { it.isNotEmpty() } ?: local?.steps.orEmpty()
            )
        }.getOrElse {
            local
        }
    }

    private fun mergeRemoteOrder(
        localOrder: List<OrganizedPetite>,
        orderedIds: List<String>
    ): List<OrganizedPetite> {
        val byId = localOrder.associateBy { it.id }
        val remoteOrdered = orderedIds
            .distinct()
            .mapNotNull { byId[it] }
        val remoteIdSet = remoteOrdered.map { it.id }.toSet()
        return remoteOrdered + localOrder.filterNot { it.id in remoteIdSet }
    }

    private companion object {
        const val MIN_ESTIMATED_MINUTES = 1
        const val MAX_ESTIMATED_MINUTES = 240
    }
}

open class RemoteAiDecisionClient(
    private val endpointUrl: String,
    private val timeoutMillis: Long = 2_500L,
    private val authTokenProvider: suspend () -> String? = { currentFirebaseIdToken() }
) {
    open suspend fun rankAmbiguousItems(
        candidates: List<OrganizedPetite>,
        context: TodayOrganizerContext
    ): List<String> {
        val payload = basePayload("rankAmbiguousItems", context).apply {
            put("candidates", JSONArray(candidates.map { it.toJson() }))
        }
        val response = postJson(payload)
        val ordered = response.optJSONArray("orderedIds") ?: JSONArray()
        return List(ordered.length()) { index -> ordered.optString(index) }
            .filter { it.isNotBlank() }
    }

    open suspend fun generateRecommendationReason(
        candidate: OrganizedPetite,
        context: TodayOrganizerContext
    ): AiRecommendationReason {
        val payload = basePayload("generateRecommendationReason", context).apply {
            put("candidate", candidate.toJson())
        }
        val response = postJson(payload)
        val stepsArray = response.optJSONArray("steps") ?: JSONArray()
        return AiRecommendationReason(
            aiComment = response.optString("comment").takeIf { it.isNotBlank() },
            estimatedMinutes = response.optInt("estimatedMinutes", -1).takeIf { it > 0 },
            steps = List(stepsArray.length()) { index -> stepsArray.optString(index) }
                .filter { it.isNotBlank() }
        )
    }

    private suspend fun postJson(payload: JSONObject): JSONObject {
        val idToken = authTokenProvider()?.takeIf { it.isNotBlank() }
            ?: error("AI decision auth token is unavailable")
        return withContext(Dispatchers.IO) {
            withTimeout(timeoutMillis) {
                val connection = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = timeoutMillis.toInt()
                    readTimeout = timeoutMillis.toInt()
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $idToken")
                }
                try {
                    OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(payload.toString())
                    }
                    val code = connection.responseCode
                    if (code !in 200..299) error("AI decision endpoint failed: HTTP $code")
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
                    JSONObject(body)
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    private fun basePayload(type: String, context: TodayOrganizerContext): JSONObject {
        return JSONObject()
            .put("type", type)
            .put("context", JSONObject()
                .put("todayMillis", context.todayMillis)
                .put("recoveryMode", context.recoveryMode)
            )
            .put("promptRules", JSONArray(PROMPT_RULES))
    }

    private fun OrganizedPetite.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("sourceType", sourceType.name)
            .put("title", title)
            .put("dueDate", dateMillis)
            .put("dDay", examDValue)
            .put("category", category?.name ?: activityCategory ?: routineTimerCategory)
            .put("priorityScore", priorityScore)
            .put("burdenScore", burdenScore)
            .put("isSeverelyBehind", isSeverelyBehind)
            .put("studySummary", JSONObject()
                .put("totalStudyMinutesSinceD7", totalStudyMinutesSinceD7)
                .put("studiedDaysSinceD7", studiedDaysSinceD7)
                .put("missedDaysSinceD7", missedDaysSinceD7)
            )
    }

    private companion object {
        suspend fun currentFirebaseIdToken(): String? {
            val user = FirebaseAuth.getInstance().currentUser ?: return null
            return user.getIdToken(false).awaitResult().token
        }

        val PROMPT_RULES = listOf(
            "Flowlog reduces record and choice fatigue.",
            "Do not tell the user to catch up on everything that is late.",
            "Use D-7 exam guidance, compressed to the remaining days and study deficit.",
            "Explain only one recommendation card per exam.",
            "On D-1, prefer wrong answers, formulas, and representative problem flow over new study.",
            "On D-0, recommend warm-up only.",
            "When same-day assignments conflict with exams, consider deadline and recovery possibility together.",
            "Return JSON only."
        )
    }
}

object AiDecisionProviderFactory {
    fun create(): AiDecisionProvider {
        val mock = MockAiDecisionProvider()
        return if (AiDecisionSettings.REMOTE_AI_DECISION_ENABLED) {
            RemoteAiDecisionProvider(
                endpointUrl = AiDecisionSettings.AI_DECISION_ENDPOINT_URL,
                enabled = true,
                fallback = mock
            )
        } else {
            mock
        }
    }
}
