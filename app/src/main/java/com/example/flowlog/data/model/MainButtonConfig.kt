package com.example.flowlog.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class MainButtonSource { DEFAULT, USER_ADDED }

@Serializable
data class MainButtonItem(
    val category: String,
    val order: Int,
    val source: MainButtonSource = MainButtonSource.DEFAULT,
    val isPinned: Boolean = false
)

@Serializable
data class MainButtonConfig(
    val buttons: List<MainButtonItem>,
    val configured: Boolean = false,
    val version: Int = 0
) {
    companion object {
        const val MIN_BUTTONS = 4
        const val MAX_BUTTONS = 10
        const val CURRENT_VERSION = 1

        val ALL_SELECTABLE_CATEGORIES = listOf(
            "SLEEP", "REST", "WORK", "STUDY", "EXERCISE", "WASH",
            "MEAL", "ETC", "DEVELOPMENT", "READING", "SCHOOL", "COMPANY", "MOVE"
        )

        // 자동 보충 시 사용하는 기본 후보 순서
        val DEFAULT_FALLBACK_CATEGORIES = listOf(
            "STUDY", "REST", "EXERCISE", "MEAL", "SLEEP", "ETC"
        )

        val DEFAULT = MainButtonConfig(
            buttons = listOf(
                MainButtonItem("STUDY", 0),
                MainButtonItem("REST", 1),
                MainButtonItem("EXERCISE", 2),
                MainButtonItem("MEAL", 3),
                MainButtonItem("SLEEP", 4),
                MainButtonItem("ETC", 5),
            ),
            configured = false,
            version = 0
        )
    }
}
