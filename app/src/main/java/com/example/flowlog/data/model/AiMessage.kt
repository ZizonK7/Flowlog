package com.example.flowlog.data.model

import java.util.UUID

enum class RecommendationStatus { PENDING, ACCEPTED, DISMISSED }

sealed class AiMessage {
    abstract val id: String

    data class AssistantText(
        override val id: String = UUID.randomUUID().toString(),
        val text: String
    ) : AiMessage()

    data class MainButtonRecommendation(
        override val id: String = UUID.randomUUID().toString(),
        val category: String,
        val status: RecommendationStatus = RecommendationStatus.PENDING
    ) : AiMessage()

    data class UserText(
        override val id: String = UUID.randomUUID().toString(),
        val text: String
    ) : AiMessage()
}
