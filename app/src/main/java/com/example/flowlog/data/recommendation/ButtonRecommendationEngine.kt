package com.example.flowlog.data.recommendation

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
        // 직접 카테고리 버튼을 눌렀을 때 후보로 인정되는 카테고리
        private val CANDIDATE_CATEGORIES = setOf("DEVELOPMENT", "SCHOOL", "COMPANY")

        // 활동 제목(정규화)으로 후보 카테고리를 추론하는 매핑
        // 사용자가 다른 버튼(예: 공부)을 눌러도 제목이 "개발"이면 DEVELOPMENT로 집계
        private val TITLE_TO_CATEGORY = mapOf(
            "개발" to "DEVELOPMENT",
            "코딩" to "DEVELOPMENT",
            "프로그래밍" to "DEVELOPMENT",
            "학교" to "SCHOOL",
            "회사" to "COMPANY",
            "출근" to "COMPANY"
        )

        // SCHOOL과 COMPANY는 같은 슬롯을 공유 — 점수 높은 쪽 하나만 승격
        private val MUTUAL_EXCLUSION_GROUPS = listOf(setOf("SCHOOL", "COMPANY"))

        private val DEFAULT_CATEGORIES = setOf(
            "SLEEP", "REST", "WORK", "STUDY", "EXERCISE", "WASH", "MEAL", "ETC"
        )

        private const val BURST_WINDOW_MILLIS = 5 * 60 * 1000L
        private const val LOOKBACK_DAYS = 90
        private const val MIN_SECOND_SCORE = 30.0
        private const val MIN_SECOND_ACTIVE_DAYS = 3
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
            .mapNotNull { session ->
                val candidateCategory = when {
                    session.category in CANDIDATE_CATEGORIES -> session.category
                    else -> TITLE_TO_CATEGORY[session.title.trim().lowercase()]
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

        val deduplicated = applyMutualExclusion(scores)
        val top2 = deduplicated.sortedByDescending { it.score }.take(2)

        if (top2.size < 2) return emptyList()

        val second = top2[1]
        val secondIsStable = second.score >= MIN_SECOND_SCORE ||
            second.recentActiveDays >= MIN_SECOND_ACTIVE_DAYS

        if (!secondIsStable) return emptyList()

        return top2.map { it.category }
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

    private fun dayKey(millis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }
}
