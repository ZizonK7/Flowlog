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

    /** 확정 분류 기반 SCHOOL/MOVE 기존 버튼 제안. userId 는 생성 시점 계정 (계정 전환 가드). */
    data class ExistingButtonRecommendation(
        override val id: String,
        val userId: String,
        val proposalId: String,
        val category: String,
        val evidenceDays: Int,
        val status: RecommendationStatus = RecommendationStatus.PENDING
    ) : AiMessage()

    data class UserText(
        override val id: String = UUID.randomUUID().toString(),
        val text: String
    ) : AiMessage()
}
