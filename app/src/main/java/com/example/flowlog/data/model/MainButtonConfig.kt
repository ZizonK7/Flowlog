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
            "MEAL", "ETC", "DEVELOPMENT", "READING", "SCHOOL", "COMPANY", "MOVE", "HOBBY"
        )

        val EMPTY = MainButtonConfig(
            buttons = emptyList(),
            configured = false,
            version = 0
        )
    }
}
