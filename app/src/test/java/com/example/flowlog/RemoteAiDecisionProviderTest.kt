package com.example.flowlog

import com.example.flowlog.data.agent.OrganizedPetite
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.agent.RemoteAiDecisionClient
import com.example.flowlog.data.agent.RemoteAiDecisionProvider
import com.example.flowlog.data.agent.TodayOrganizerContext
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress

class RemoteAiDecisionProviderTest {

    @Test
    fun blankEndpointUsesLocalFallback() = runBlocking {
        val provider = RemoteAiDecisionProvider(endpointUrl = "", enabled = true)

        val result = provider.rankAmbiguousItems(reversedCandidates(), context)

        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun malformedUrlUsesLocalFallback() = runBlocking {
        val provider = remoteProvider("not-a-url", token = "test-token")

        val result = provider.rankAmbiguousItems(reversedCandidates(), context)

        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun serverFallbackResponseKeepsLocalRankOrder() = runBlocking {
        withServer("""{"fallback":true,"error":{"code":"OPENAI_REQUEST_FAILED"}}""") { endpoint ->
            val provider = remoteProvider(endpoint, token = "test-token")

            val result = provider.rankAmbiguousItems(reversedCandidates(), context)

            assertEquals(listOf("a", "b"), result.map { it.id })
        }
    }

    @Test
    fun weirdOrderedIdsIgnoreUnknownAndDuplicates() = runBlocking {
        val provider = RemoteAiDecisionProvider(
            endpointUrl = "http://127.0.0.1/aiDecision",
            enabled = true,
            timeoutMillis = 1_000L,
            client = object : RemoteAiDecisionClient("http://127.0.0.1/aiDecision") {
                override suspend fun rankAmbiguousItems(
                    candidates: List<OrganizedPetite>,
                    context: TodayOrganizerContext
                ): List<String> = listOf("missing", "b", "b", "a")
            }
        )

        val result = provider.rankAmbiguousItems(reversedCandidates(), context)

        assertEquals(listOf("b", "a"), result.map { it.id })
    }

    @Test
    fun serverFallbackResponseKeepsLocalReason() = runBlocking {
        withServer("""{"fallback":true,"error":{"code":"OPENAI_API_KEY_MISSING"}}""") { endpoint ->
            val provider = remoteProvider(endpoint, token = "test-token")

            val result = provider.generateRecommendationReason(examCandidate(), context)

            assertNotNull(result?.aiComment)
            assertTrue(result?.steps.orEmpty().isNotEmpty())
            assertEquals(50, result?.estimatedMinutes)
        }
    }

    @Test
    fun abnormalReasonFieldsFallBackToLocalTemplate() = runBlocking {
        withServer("""{"comment":"","steps":["","   "],"estimatedMinutes":999}""") { endpoint ->
            val provider = remoteProvider(endpoint, token = "test-token")

            val result = provider.generateRecommendationReason(examCandidate(), context)

            assertNotNull(result?.aiComment)
            assertTrue(result?.steps.orEmpty().isNotEmpty())
            assertEquals(50, result?.estimatedMinutes)
        }
    }

    @Test
    fun unavailableAuthTokenUsesLocalFallback() = runBlocking {
        withServer("""{"orderedIds":["b"]}""") { endpoint ->
            val provider = remoteProvider(endpoint, token = null)

            val result = provider.rankAmbiguousItems(reversedCandidates(), context)

            assertEquals(listOf("a", "b"), result.map { it.id })
        }
    }

    private fun remoteProvider(endpoint: String, token: String?): RemoteAiDecisionProvider {
        return RemoteAiDecisionProvider(
            endpointUrl = endpoint,
            enabled = true,
            timeoutMillis = 1_000L,
            client = RemoteAiDecisionClient(
                endpointUrl = endpoint,
                timeoutMillis = 1_000L,
                authTokenProvider = { token }
            )
        )
    }

    private suspend fun withServer(responseBody: String, block: suspend (String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/aiDecision") { exchange ->
            exchange.requestBody.use { it.readBytes() }
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}/aiDecision")
        } finally {
            server.stop(0)
        }
    }

    private fun reversedCandidates(): List<OrganizedPetite> = listOf(
        OrganizedPetite(
            id = "b",
            title = "B",
            sourceType = PetiteSourceType.TODO,
            sourceId = "2",
            priorityScore = 20
        ),
        OrganizedPetite(
            id = "a",
            title = "A",
            sourceType = PetiteSourceType.EXAM,
            sourceId = "1",
            priorityScore = 10
        )
    )

    private fun examCandidate(): OrganizedPetite = OrganizedPetite(
        id = "exam_1",
        title = "Exam study",
        sourceType = PetiteSourceType.EXAM,
        sourceId = "1",
        priorityScore = 40,
        isSeverelyBehind = false,
        examDValue = 3
    )

    private val context = TodayOrganizerContext(
        todayMillis = 1_780_704_000_000L,
        recoveryMode = false
    )
}
