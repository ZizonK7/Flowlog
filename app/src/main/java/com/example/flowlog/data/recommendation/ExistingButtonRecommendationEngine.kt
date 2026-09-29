package com.example.flowlog.data.recommendation

import com.example.flowlog.data.local.entity.StudyDecisionEntity
import com.example.flowlog.data.model.MainButtonConfig
import com.example.flowlog.data.model.MainButtonItem
import com.example.flowlog.data.model.MainButtonSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs

/** 추천 시점 (로컬 달력 기준). 날짜는 YYYY-MM-DD 문자열이므로 사전순 비교 = 날짜순. */
data class ExistingButtonNow(
    val userId: String,
    val localDate: String,
    val windowStartDate: String,   // 포함 (localDate - WINDOW_DAYS)
    val weekday: Int,              // Sun = 0
    val minuteOfDay: Int
)

enum class ExistingButtonAction { USE_EXISTING, ADD_OR_USE_ONCE, REPLACE_OR_USE_ONCE }

data class ExistingButtonProposal(
    val proposalId: String,
    val category: String,
    val action: ExistingButtonAction,
    val evidenceDays: Int,
    val canStartNow: Boolean
)

enum class MainButtonEditRejection { NOT_ELIGIBLE, ALREADY_PRESENT, AT_CAPACITY, OLD_MISSING, OLD_PINNED, SAME_CATEGORY }

/**
 * 사용자가 확정한 CLASSIFICATION_CONFIRM 결정만으로 SCHOOL/MOVE 기존 버튼을 제안한다.
 * 결과는 메신저 카드 제안일 뿐이며 타이머를 자동으로 전환하지 않는다.
 */
object ExistingButtonRecommendationEngine {
    val ELIGIBLE_CATEGORIES = listOf("SCHOOL", "MOVE")
    const val WINDOW_DAYS = 28
    const val MIN_DISTINCT_DAYS = 3
    const val TOLERANCE_MINUTES = 60
    const val TIME_BUCKET_MINUTES = 60

    const val KIND_CLASSIFICATION_CONFIRM = "CLASSIFICATION_CONFIRM"
    const val KIND_BUTTON_USE = "BUTTON_USE"
    const val KIND_BUTTON_ADD = "BUTTON_ADD"
    const val KIND_BUTTON_REPLACE = "BUTTON_REPLACE"
    const val KIND_RECOMMENDATION_SHOWN = "RECOMMENDATION_SHOWN"
    const val KIND_RECOMMENDATION_DISMISS = "RECOMMENDATION_DISMISS"
    const val KIND_RECOMMENDATION_SNOOZE = "RECOMMENDATION_SNOOZE"
    const val KIND_RECOMMENDATION_DISABLE = "RECOMMENDATION_DISABLE"
    const val KIND_UNDO = "UNDO"

    const val OUTCOME_APPLIED = "APPLIED"
    const val OUTCOME_DECLINED = "DECLINED"
    const val OUTCOME_SNOOZED = "SNOOZED"
    const val OUTCOME_SHOWN = "SHOWN"

    fun proposalId(category: String, localDate: String, minuteOfDay: Int): String =
        "existing-button~$category~$localDate~${minuteOfDay / TIME_BUCKET_MINUTES}"

    fun propose(
        decisions: List<StudyDecisionEntity>,
        now: ExistingButtonNow,
        mainButtons: List<MainButtonItem>,
        isRunning: Boolean
    ): List<ExistingButtonProposal> {
        val own = decisions.filter { it.userId == now.userId }
        val latestClassification = own.filter { it.kind == KIND_CLASSIFICATION_CONFIRM && it.activityId != null && it.outcome == OUTCOME_APPLIED }
            .groupBy { it.activityId }.mapValues { (_, events) -> events.maxBy { it.createdAt }.decisionId }
        val mainCategories = mainButtons.map { it.category }.toSet()
        return ELIGIBLE_CATEGORIES.mapNotNull { category ->
            val proposalId = proposalId(category, now.localDate, now.minuteOfDay)
            if (isInhibited(own, category, proposalId, now)) return@mapNotNull null
            val evidenceDays = own.asSequence()
                .filter { it.kind == KIND_CLASSIFICATION_CONFIRM && it.outcome == OUTCOME_APPLIED && it.category == category }
                .filter { isExplicitEtc(it.payloadJson) }
                .filter { it.activityId == null || latestClassification[it.activityId] == it.decisionId }
                .filter { inWindow(it, now) && it.localDate < now.localDate && matchesSlot(it, now) }
                .map { it.localDate }
                .toSet()
                .size
            if (evidenceDays < MIN_DISTINCT_DAYS) return@mapNotNull null
            val action = when {
                category in mainCategories -> ExistingButtonAction.USE_EXISTING
                mainButtons.size < MainButtonConfig.MAX_BUTTONS -> ExistingButtonAction.ADD_OR_USE_ONCE
                else -> ExistingButtonAction.REPLACE_OR_USE_ONCE
            }
            ExistingButtonProposal(proposalId, category, action, evidenceDays, canStartNow = !isRunning)
        }
    }

    fun isWithinCap(buttons: List<MainButtonItem>): Boolean = buttons.size <= MainButtonConfig.MAX_BUTTONS &&
        buttons.map { it.category }.distinct().size == buttons.size

    fun checkAdd(buttons: List<MainButtonItem>, category: String): MainButtonEditRejection? = when {
        category !in ELIGIBLE_CATEGORIES -> MainButtonEditRejection.NOT_ELIGIBLE
        buttons.any { it.category == category } -> MainButtonEditRejection.ALREADY_PRESENT
        buttons.size >= MainButtonConfig.MAX_BUTTONS -> MainButtonEditRejection.AT_CAPACITY
        else -> null
    }

    fun checkReplace(buttons: List<MainButtonItem>, oldCategory: String, newCategory: String): MainButtonEditRejection? {
        if (newCategory !in ELIGIBLE_CATEGORIES) return MainButtonEditRejection.NOT_ELIGIBLE
        if (oldCategory == newCategory) return MainButtonEditRejection.SAME_CATEGORY
        if (buttons.any { it.category == newCategory }) return MainButtonEditRejection.ALREADY_PRESENT
        val old = buttons.firstOrNull { it.category == oldCategory } ?: return MainButtonEditRejection.OLD_MISSING
        if (old.isPinned) return MainButtonEditRejection.OLD_PINNED
        return null
    }

    fun applyAdd(buttons: List<MainButtonItem>, category: String): List<MainButtonItem> {
        require(checkAdd(buttons, category) == null)
        val nextOrder = (buttons.maxOfOrNull { it.order } ?: -1) + 1
        return buttons + MainButtonItem(category = category, order = nextOrder, source = MainButtonSource.USER_ADDED)
    }

    fun applyReplace(buttons: List<MainButtonItem>, oldCategory: String, newCategory: String): List<MainButtonItem> =
        buttons.map { btn ->
            if (btn.category == oldCategory) btn.copy(category = newCategory, source = MainButtonSource.USER_ADDED)
            else btn
        }

    private fun isInhibited(
        own: List<StudyDecisionEntity>,
        category: String,
        proposalId: String,
        now: ExistingButtonNow
    ): Boolean = own.any { d ->
        when (d.kind) {
            KIND_RECOMMENDATION_DISABLE -> d.category == category
            KIND_RECOMMENDATION_DISMISS -> d.category == category && inWindow(d, now) && matchesSlot(d, now)
            KIND_UNDO -> inWindow(d, now) && (d.category == category || category in payloadCategories(d.payloadJson))
            KIND_RECOMMENDATION_SNOOZE, KIND_BUTTON_USE, KIND_BUTTON_ADD, KIND_BUTTON_REPLACE -> d.proposalId == proposalId
            else -> false
        }
    }

    private fun inWindow(d: StudyDecisionEntity, now: ExistingButtonNow): Boolean =
        d.localDate >= now.windowStartDate && d.localDate <= now.localDate

    private fun matchesSlot(d: StudyDecisionEntity, now: ExistingButtonNow): Boolean =
        d.weekday == now.weekday && abs(d.minuteOfDay - now.minuteOfDay) <= TOLERANCE_MINUTES

    private fun payloadCategories(raw: String): Set<String> = runCatching {
        val obj = Json.parseToJsonElement(raw).jsonObject
        listOf("fromCategory", "toCategory").mapNotNull { obj[it]?.jsonPrimitive?.contentOrNull }.toSet()
    }.getOrDefault(emptySet())

    private fun isExplicitEtc(raw: String): Boolean = runCatching {
        val obj = Json.parseToJsonElement(raw).jsonObject
        obj["fromCategory"]?.jsonPrimitive?.contentOrNull == "ETC" &&
            (obj["sourceType"]?.jsonPrimitive?.contentOrNull ?: "MANUAL") == "MANUAL"
    }.getOrDefault(false)
}
