package com.example.flowlog.data.recommendation

import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.model.ActivitySession
import java.util.Calendar

class ButtonRecommendationEngine {

    data class CandidateScore(
        val category: String,
        val score: Double,
        val activeDays: Int,
        val recentActiveDays: Int
    )

    companion object {
        // 추천 후보 카테고리 — TOOTHBRUSH/SNACK(빠른 타이머)을 제외한 13개 전부
        private val CANDIDATE_CATEGORIES = setOf(
            "SLEEP", "REST", "WORK", "STUDY", "EXERCISE", "WASH", "MEAL", "ETC",
            "DEVELOPMENT", "READING", "SCHOOL", "COMPANY", "MOVE", "GAME"
        )

        // 활동 제목(정규화)으로 후보 카테고리를 추론하는 매핑
        // 카테고리 버튼을 직접 눌렀을 때는 session.category로 바로 집계되고,
        // ETC 등 다른 버튼을 눌렀지만 제목이 매핑되면 해당 카테고리로 집계
        private val TITLE_TO_CATEGORY = mapOf(
            "수면" to "SLEEP", "잠" to "SLEEP", "취침" to "SLEEP",
            "휴식" to "REST", "쉬기" to "REST", "낮잠" to "REST",
            "업무" to "WORK", "작업" to "WORK",
            "공부" to "STUDY", "스터디" to "STUDY",
            "운동" to "EXERCISE", "헬스" to "EXERCISE", "달리기" to "EXERCISE", "산책" to "EXERCISE",
            "씻기" to "WASH", "샤워" to "WASH", "목욕" to "WASH",
            "식사" to "MEAL", "밥" to "MEAL", "점심" to "MEAL", "저녁" to "MEAL", "아침" to "MEAL",
            "개발" to "DEVELOPMENT", "코딩" to "DEVELOPMENT", "프로그래밍" to "DEVELOPMENT",
            "독서" to "READING", "책" to "READING", "읽기" to "READING", "reading" to "READING", "read" to "READING",
            "학교" to "SCHOOL",
            "회사" to "COMPANY", "출근" to "COMPANY",
            "이동" to "MOVE", "이동시간" to "MOVE", "통학" to "MOVE", "퇴근" to "MOVE", "버스" to "MOVE", "지하철" to "MOVE",
            "게임" to "GAME", "gaming" to "GAME",
            "롤" to "GAME", "lol" to "GAME", "tft" to "GAME",
            "롤체" to "GAME", "롤토체스" to "GAME",
            "배그" to "GAME", "옵치" to "GAME", "오버워치" to "GAME", "fm24" to "GAME"
        )
        private val TEXT_KEYWORDS_TO_CATEGORY = listOf(
            listOf("수면", "취침", "잠") to "SLEEP",
            listOf("휴식", "쉬기", "낮잠") to "REST",
            listOf("업무", "작업") to "WORK",
            listOf("공부", "스터디") to "STUDY",
            listOf("운동", "헬스", "달리기", "산책") to "EXERCISE",
            listOf("씻기", "샤워", "목욕") to "WASH",
            listOf("식사", "밥", "점심", "저녁", "아침") to "MEAL",
            listOf("개발", "코딩", "프로그래밍") to "DEVELOPMENT",
            listOf("독서", "책", "읽기", "reading", "read") to "READING",
            listOf("학교") to "SCHOOL",
            listOf("회사", "출근") to "COMPANY",
            listOf("이동", "이동시간", "통학", "퇴근", "버스", "지하철") to "MOVE",
            listOf("게임", "gaming", "롤", "lol", "tft", "롤체", "롤토체스", "배그", "옵치", "오버워치", "fm24") to "GAME"
        )

        // SCHOOL과 COMPANY는 같은 슬롯을 공유 — 점수 높은 쪽 하나만 승격
        private val MUTUAL_EXCLUSION_GROUPS = listOf(setOf("SCHOOL", "COMPANY"))

        private const val BURST_WINDOW_MILLIS = 5 * 60 * 1000L
        private const val LOOKBACK_DAYS = 90
        private const val MIN_SCORE = 30.0
        private const val MIN_ACTIVE_DAYS = 3
    }

    fun computePromotedCategories(activities: List<ActivitySession>): List<String> {
        val now = System.currentTimeMillis()
        val windowStart = now - LOOKBACK_DAYS.toLong() * 24 * 60 * 60 * 1000L
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000L
        val fourteenDaysAgo = now - 14L * 24 * 60 * 60 * 1000L

        // 각 세션을 후보 카테고리로 매핑:
        //   1순위: 카테고리 자체가 후보인 경우 (개발 버튼을 직접 눌렀을 때)
        //   2순위: 제목이 매핑 테이블에 있는 경우 (공부 버튼 + 제목 "개발")
        val candidateSessions: List<Pair<String, ActivitySession>> = activities
            .filter { it.startTime >= windowStart }
            .filter { it.sourceType != ActivitySourceType.DAILY_CUE_ROUTINE }
            .mapNotNull { session ->
                val candidateCategory = when {
                    session.category == "ETC" -> inferCategoryFromText(session) ?: session.category
                    session.category in CANDIDATE_CATEGORIES -> session.category
                    else -> inferCategoryFromText(session)
                }
                candidateCategory?.let { it to session }
            }

        val byCategory = candidateSessions.groupBy({ it.first }, { it.second })

        val scores = byCategory.entries.map { (category, sessions) ->
            val sorted = sessions.sortedBy { it.startTime }

            var burstCount = 0
            for (i in 1 until sorted.size) {
                if (sorted[i].startTime - sorted[i - 1].startTime < BURST_WINDOW_MILLIS) {
                    burstCount++
                }
            }
            val dedupedCount = sessions.size - burstCount

            val activeDaySet = sessions.map { dayKey(it.startTime) }.toSet()
            val activeDays = activeDaySet.size

            val recent7d = sessions.count { it.startTime >= sevenDaysAgo }
            val recent14d = sessions.count { it.startTime >= fourteenDaysAgo }

            val avgDurationMinutes = sessions.map { it.durationMillis / 60_000.0 }.average()
            val durBonus = when {
                avgDurationMinutes >= 10 -> 3.0
                avgDurationMinutes >= 3 -> 1.0
                else -> 0.0
            }

            val consistency = when {
                activeDays >= 5 -> 5.0
                activeDays >= 3 -> 2.0
                else -> 0.0
            }

            val deduped = minOf(dedupedCount, 30) * 0.5
            val activeDaysScore = minOf(activeDays, 14) * 1.5
            val recent7dScore = recent7d * 2.0
            val recent14dScore = recent14d * 0.5
            val burstPenalty = -minOf(burstCount * 0.3, 5.0)

            val rawScore = deduped + activeDaysScore + recent7dScore + recent14dScore +
                durBonus + consistency + burstPenalty
            val score = maxOf(0.0, Math.round(rawScore * 10) / 10.0)

            val recentActiveDays = sessions
                .filter { it.startTime >= fourteenDaysAgo }
                .map { dayKey(it.startTime) }
                .toSet()
                .size

            CandidateScore(
                category = category,
                score = score,
                activeDays = activeDays,
                recentActiveDays = recentActiveDays
            )
        }

        return applyMutualExclusion(scores)
            .sortedByDescending { it.score }
            .filter { it.isStablePromotion() }
            .map { it.category }
    }

    private fun CandidateScore.isStablePromotion(): Boolean {
        return score >= MIN_SCORE || recentActiveDays >= MIN_ACTIVE_DAYS
    }

    private fun applyMutualExclusion(scores: List<CandidateScore>): List<CandidateScore> {
        val usedGroups = mutableSetOf<Set<String>>()
        return scores
            .sortedByDescending { it.score }
            .filter { candidate ->
                val group = MUTUAL_EXCLUSION_GROUPS.find { candidate.category in it }
                if (group == null) {
                    true
                } else if (group !in usedGroups) {
                    usedGroups += group
                    true
                } else {
                    false
                }
            }
    }

    private fun inferCategoryFromText(session: ActivitySession): String? {
        val title = session.title.trim().lowercase()
        val note = session.note.orEmpty().trim().lowercase()
        val exactTitleMatch = TITLE_TO_CATEGORY[title]
        if (exactTitleMatch != null) return exactTitleMatch

        return TEXT_KEYWORDS_TO_CATEGORY.firstNotNullOfOrNull { (keywords, category) ->
            category.takeIf { keywords.any { keyword -> title.contains(keyword) || note.contains(keyword) } }
        }
    }

    private fun dayKey(millis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }
}
