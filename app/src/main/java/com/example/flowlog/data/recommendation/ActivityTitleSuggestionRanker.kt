package com.example.flowlog.data.recommendation

import com.example.flowlog.data.model.ActivitySession
import kotlin.math.pow

/** Recommendations from saved, completed sessions only. Scores are derived, never persisted. */
internal object ActivityTitleSuggestionRanker {
    // Initial tuning values: keep these together when comparing real recommendation results.
    private const val BASE_SCORE = 40.0
    private const val FREQUENCY_WEIGHT = 60.0
    private const val SATURATION_CONSTANT = 3.0
    private const val USAGE_HALF_LIFE_DAYS = 14.0
    private const val RECENCY_HALF_LIFE_DAYS = 7.0
    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val MAX_SUGGESTIONS = 5

    /** Components available for tests/debugging without adding diagnostic UI or logging titles. */
    data class CandidateScore(
        val title: String,
        val score: Double,
        val weightedUses: Double,
        val lastCompletedAt: Long
    )

    private class Aggregate(var weightedUses: Double = 0.0, var lastCompletedAt: Long = 0L)

    fun rankedScores(
        category: String,
        activities: List<ActivitySession>,
        defaultTitle: String,
        nowMillis: Long
    ): List<CandidateScore> {
        val candidates = mutableMapOf<String, Aggregate>()
        for (activity in activities) {
            if (activity.category != category || activity.title.isBlank() || activity.title == defaultTitle) continue
            val completedAt = activity.endTime
            if (completedAt <= 0L || completedAt > nowMillis || completedAt < activity.startTime) continue

            // Exact stored titles: no trimming, case folding, or rewriting historical records.
            val candidate = candidates.getOrPut(activity.title) { Aggregate() }
            val ageDays = (nowMillis - completedAt).toDouble() / MILLIS_PER_DAY
            candidate.weightedUses += 2.0.pow(-ageDays / USAGE_HALF_LIFE_DAYS)
            candidate.lastCompletedAt = maxOf(candidate.lastCompletedAt, completedAt)
        }
        return candidates.map { (title, candidate) ->
            val daysSinceLastUse = (nowMillis - candidate.lastCompletedAt).toDouble() / MILLIS_PER_DAY
            val frequencyScore = BASE_SCORE + FREQUENCY_WEIGHT *
                candidate.weightedUses / (candidate.weightedUses + SATURATION_CONSTANT)
            CandidateScore(
                title = title,
                score = frequencyScore * 2.0.pow(-daysSinceLastUse / RECENCY_HALF_LIFE_DAYS),
                weightedUses = candidate.weightedUses,
                lastCompletedAt = candidate.lastCompletedAt
            )
        }.sortedWith(
            compareByDescending<CandidateScore> { it.score }
                .thenByDescending { it.lastCompletedAt }
                .thenBy { it.title }
        )
    }

    fun suggest(
        category: String,
        activities: List<ActivitySession>,
        defaultTitle: String,
        nowMillis: Long
    ): List<String> = rankedScores(category, activities, defaultTitle, nowMillis)
        .take(MAX_SUGGESTIONS)
        .map { it.title }
}
