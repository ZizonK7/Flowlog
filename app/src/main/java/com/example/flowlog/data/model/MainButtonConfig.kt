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
    val buttons: List<MainButtonItem>
) {
    companion object {
        const val MIN_BUTTONS = 4
        const val MAX_BUTTONS = 10

        val ALL_SELECTABLE_CATEGORIES = listOf(
            "SLEEP", "REST", "WORK", "STUDY", "EXERCISE", "WASH",
            "MEAL", "ETC", "DEVELOPMENT", "READING", "SCHOOL", "COMPANY", "MOVE"
        )

        val DEFAULT = MainButtonConfig(
            buttons = listOf(
                MainButtonItem("STUDY", 0),
                MainButtonItem("REST", 1),
                MainButtonItem("EXERCISE", 2),
                MainButtonItem("MEAL", 3),
                MainButtonItem("SLEEP", 4),
                MainButtonItem("ETC", 5),
            )
        )
    }
}
