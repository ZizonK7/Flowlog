package com.example.flowlog.data.model

/**
 * A user-selected context in which Flowlog may surface a Routine.
 *
 * This is intentionally descriptive metadata only. Recommendation eligibility
 * and ranking rules can later be implemented against this stable enum without
 * changing the Daily Cue editor or persistence format.
 */
enum class DailyCueRecommendationTiming(
    val title: String,
    val description: String
) {
    NONE(
        title = "선택 안 함",
        description = "추천 타이밍을 지정하지 않아요"
    ),
    AFTER_WAKING(
        title = "일어난 직후",
        description = "하루를 시작할 때 가볍게"
    ),
    FOCUS_READY(
        title = "집중할 수 있을 때",
        description = "가장 몰입하기 좋은 시간"
    ),
    AFTER_FOCUS(
        title = "집중한 뒤 이어서",
        description = "다른 집중 활동 이후 자연스럽게"
    ),
    SOFT_TRANSITION(
        title = "식사 후 / 잠들기 전",
        description = "부담 없이 전환하고 싶을 때"
    ),
    FLOW_RESET(
        title = "흐름이 끊겼을 때",
        description = "휴식 후 다시 흐름을 잡고 싶을 때"
    ),
    AFTER_MAIN_TASKS(
        title = "하루 할 일을 끝낸 뒤",
        description = "성과 후 보상처럼 하고 싶을 때"
    );

    companion object {
        val default: DailyCueRecommendationTiming = NONE

        fun fromStorage(value: String?): DailyCueRecommendationTiming =
            entries.firstOrNull { it.name == value } ?: default
    }
}
