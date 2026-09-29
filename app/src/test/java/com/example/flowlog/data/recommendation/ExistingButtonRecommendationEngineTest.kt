package com.example.flowlog.data.recommendation

import com.example.flowlog.data.local.entity.StudyDecisionEntity
import com.example.flowlog.data.model.MainButtonItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExistingButtonRecommendationEngineTest {
    private val engine = ExistingButtonRecommendationEngine
    private val uid = "user-a"

    // 2026-09-28 월요일(weekday 1) 09:00, 창 시작 2026-08-31
    private val now = ExistingButtonNow(uid, "2026-09-28", "2026-08-31", 1, 540)

    private val baseButtons = listOf("SLEEP", "WORK", "MEAL", "REST")
        .mapIndexed { i, c -> MainButtonItem(category = c, order = i) }
    private val fullButtons = listOf(
        "SLEEP", "REST", "WORK", "STUDY", "EXERCISE", "WASH", "MEAL", "ETC", "DEVELOPMENT", "READING"
    ).mapIndexed { i, c -> MainButtonItem(category = c, order = i) }

    private var seq = 0

    private fun decision(
        localDate: String,
        kind: String = ExistingButtonRecommendationEngine.KIND_CLASSIFICATION_CONFIRM,
        category: String? = "SCHOOL",
        minuteOfDay: Int = 540,
        weekday: Int = 1,
        outcome: String = ExistingButtonRecommendationEngine.OUTCOME_APPLIED,
        userId: String = uid,
        proposalId: String? = null,
        payloadJson: String = "{\"fromCategory\":\"ETC\",\"sourceType\":\"MANUAL\"}"
    ): StudyDecisionEntity {
        seq++
        return StudyDecisionEntity(
            decisionId = "m$seq~$kind",
            userId = userId,
            proposalId = proposalId ?: "p$seq",
            kind = kind,
            category = category,
            localDate = localDate,
            minuteOfDay = minuteOfDay,
            weekday = weekday,
            outcome = outcome,
            createdAt = seq.toLong(),
            payloadJson = payloadJson
        )
    }

    private fun threeMondays(category: String = "SCHOOL", minutes: List<Int> = listOf(540, 540, 540)) =
        listOf("2026-09-21", "2026-09-14", "2026-09-07").zip(minutes).map { (d, m) ->
            decision(localDate = d, category = category, minuteOfDay = m)
        }

    private fun propose(decisions: List<StudyDecisionEntity>, buttons: List<MainButtonItem> = baseButtons, running: Boolean = false) =
        engine.propose(decisions, now, buttons, running)

    @Test
    fun threeDistinctDaysWithinToleranceProducesProposal() {
        val result = propose(threeMondays(minutes = listOf(480, 600, 540)))
        assertEquals(1, result.size)
        assertEquals("SCHOOL", result[0].category)
        assertEquals(3, result[0].evidenceDays)
        assertEquals(ExistingButtonAction.ADD_OR_USE_ONCE, result[0].action)
        assertTrue(result[0].canStartNow)
    }

    @Test
    fun sameDayDecisionsCountOnce() {
        val decisions = listOf(
            decision("2026-09-21"), decision("2026-09-21", minuteOfDay = 560), decision("2026-09-14")
        )
        assertTrue(propose(decisions).isEmpty())
    }

    @Test
    fun todayDecisionsAreNotEvidence() {
        val decisions = listOf(decision("2026-09-28"), decision("2026-09-21"), decision("2026-09-14"))
        assertTrue(propose(decisions).isEmpty())
    }

    @Test
    fun outsideToleranceOrOtherWeekdayIsIgnored() {
        assertTrue(propose(threeMondays(minutes = listOf(540, 540, 601))).isEmpty())
        val otherWeekday = listOf("2026-09-22", "2026-09-15", "2026-09-08").map { decision(it, weekday = 2) }
        assertTrue(propose(otherWeekday).isEmpty())
    }

    @Test
    fun decisionsOutsideWindowAreIgnored() {
        val decisions = listOf(decision("2026-09-21"), decision("2026-09-14"), decision("2026-08-24"))
        assertTrue(propose(decisions).isEmpty())
    }

    @Test
    fun otherUserDecisionsAreIgnored() {
        val decisions = listOf("2026-09-21", "2026-09-14", "2026-09-07").map { decision(it, userId = "user-b") }
        assertTrue(propose(decisions).isEmpty())
    }

    @Test
    fun onlyConfirmedEligibleClassificationsCount() {
        assertTrue(propose(threeMondays(category = "STUDY")).isEmpty())
        val declined = listOf("2026-09-21", "2026-09-14", "2026-09-07").map {
            decision(it, outcome = ExistingButtonRecommendationEngine.OUTCOME_DECLINED)
        }
        assertTrue(propose(declined).isEmpty())
    }

    @Test
    fun moveIsRecommendedIndependently() {
        val result = propose(threeMondays(category = "MOVE"))
        assertEquals(listOf("MOVE"), result.map { it.category })
    }

    @Test
    fun dismissInSameSlotInhibits() {
        val dismiss = decision("2026-09-21", kind = ExistingButtonRecommendationEngine.KIND_RECOMMENDATION_DISMISS,
            outcome = ExistingButtonRecommendationEngine.OUTCOME_DECLINED)
        assertTrue(propose(threeMondays() + dismiss).isEmpty())
    }

    @Test
    fun disableInhibitsEvenOutsideWindow() {
        val disable = decision("2026-01-05", kind = ExistingButtonRecommendationEngine.KIND_RECOMMENDATION_DISABLE,
            outcome = ExistingButtonRecommendationEngine.OUTCOME_DECLINED, weekday = 3, minuteOfDay = 100)
        assertTrue(propose(threeMondays() + disable).isEmpty())
    }

    @Test
    fun undoReferencingCategoryInPayloadInhibits() {
        val undo = decision("2026-09-22", kind = ExistingButtonRecommendationEngine.KIND_UNDO, category = null,
            payloadJson = "{\"fromCategory\":\"SCHOOL\",\"toCategory\":\"ETC\"}")
        assertTrue(propose(threeMondays() + undo).isEmpty())
    }

    @Test
    fun actedProposalIsNotRepeatedButShownDoesNotInhibit() {
        val pid = engine.proposalId("SCHOOL", now.localDate, now.minuteOfDay)
        val shown = decision("2026-09-28", kind = ExistingButtonRecommendationEngine.KIND_RECOMMENDATION_SHOWN,
            outcome = ExistingButtonRecommendationEngine.OUTCOME_SHOWN, proposalId = pid)
        assertEquals(1, propose(threeMondays() + shown).size)
        val used = decision("2026-09-28", kind = ExistingButtonRecommendationEngine.KIND_BUTTON_USE, proposalId = pid)
        assertTrue(propose(threeMondays() + used).isEmpty())
    }

    @Test
    fun proposalIdIsStableWithinTimeBucket() {
        assertEquals(engine.proposalId("SCHOOL", "2026-09-28", 540), engine.proposalId("SCHOOL", "2026-09-28", 599))
        assertFalse(engine.proposalId("SCHOOL", "2026-09-28", 540) == engine.proposalId("SCHOOL", "2026-09-28", 600))
        assertEquals(engine.proposalId("SCHOOL", now.localDate, now.minuteOfDay), propose(threeMondays())[0].proposalId)
    }

    @Test
    fun actionReflectsMainButtonsAndRunningState() {
        val withSchool = baseButtons + MainButtonItem(category = "SCHOOL", order = 4)
        assertEquals(ExistingButtonAction.USE_EXISTING, propose(threeMondays(), withSchool)[0].action)
        assertEquals(ExistingButtonAction.REPLACE_OR_USE_ONCE, propose(threeMondays(), fullButtons)[0].action)
        assertFalse(propose(threeMondays(), running = true)[0].canStartNow)
    }

    @Test
    fun addIsRejectedAtCapacity() {
        assertEquals(MainButtonEditRejection.AT_CAPACITY, engine.checkAdd(fullButtons, "SCHOOL"))
        assertNull(engine.checkAdd(baseButtons, "SCHOOL"))
        assertEquals(MainButtonEditRejection.NOT_ELIGIBLE, engine.checkAdd(baseButtons, "HOBBY"))
        val added = engine.applyAdd(baseButtons, "SCHOOL")
        assertEquals(5, added.size)
        assertEquals(4, added.last().order)
    }

    @Test
    fun replaceKeepsCountAndOrder() {
        assertNull(engine.checkReplace(fullButtons, "READING", "SCHOOL"))
        val replaced = engine.applyReplace(fullButtons, "READING", "SCHOOL")
        assertEquals(10, replaced.size)
        assertEquals("SCHOOL", replaced[9].category)
        assertEquals(9, replaced[9].order)
        assertTrue(engine.isWithinCap(replaced))
    }

    @Test
    fun invalidReplacementsAreRejected() {
        val pinned = fullButtons.map { if (it.category == "READING") it.copy(isPinned = true) else it }
        assertEquals(MainButtonEditRejection.OLD_PINNED, engine.checkReplace(pinned, "READING", "SCHOOL"))
        assertEquals(MainButtonEditRejection.OLD_MISSING, engine.checkReplace(fullButtons, "HOBBY", "SCHOOL"))
        val withSchool = fullButtons.dropLast(1) + MainButtonItem(category = "SCHOOL", order = 9)
        assertEquals(MainButtonEditRejection.ALREADY_PRESENT, engine.checkReplace(withSchool, "ETC", "SCHOOL"))
    }

    @Test
    fun capRejectsElevenButtons() {
        assertFalse(engine.isWithinCap(fullButtons + MainButtonItem(category = "SCHOOL", order = 10)))
    }
}
