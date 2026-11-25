// Pure Kotlin recommendation engine computing weighted compatibility scores between campers and campsites.
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.engine

import com.example.trails.data.model.Campsite
import com.example.trails.matcher.model.CamperPreferences
import com.example.trails.matcher.model.LotteryContext
import com.example.trails.matcher.model.MatchResult
import com.example.trails.matcher.model.MatchScoreBreakdown
import kotlin.math.roundToInt

/**
 * Pure Kotlin, stateless recommendation engine implementing the CampMatch scoring algorithm.
 *
 * NOTE FOR TEAMMATES:
 * - Ibrahim (Campsite Discovery): Call [rankCampsites] with the full list of campsites to get
 *   a sorted list ordered by match percentage for a "Best Match" or "Recommended for You" section.
 *   Alternatively, call [calculateMatch] on a single campsite to display a badge on the campsite card.
 * - Skanda (Lottery): Pass in a [LotteryContext] map to let entrant odds dynamically boost lotteries
 *   with high chances of winning.
 *
 * Scoring Formula:
 * - Tag / Amenity Similarity (40% weight): Ratio of user's preferred tags present on the campsite.
 * - Lottery Win Odds (25% weight): Entrant odds based on active lottery winner count vs waitlist size.
 * - Budget Compatibility (20% weight): Nightly price relative to camper's maximum budget.
 * - Quality / Rating (15% weight): Average star rating across entrant reviews.
 */
object CampsiteMatcher {

    private const val TAG_WEIGHT = 0.40
    private const val ODDS_WEIGHT = 0.25
    private const val BUDGET_WEIGHT = 0.20
    private const val RATING_WEIGHT = 0.15

    /**
     * Calculates the compatibility score between a [campsite] and [preferences].
     *
     * @param campsite the campsite listing to score.
     * @param preferences the camper's active preference and filter criteria.
     * @param lotteryContext optional lottery and waitlist data for this campsite's active lottery.
     * @return a [MatchResult] containing the match percentage (0-100), human-readable highlights, and breakdown.
     */
    fun calculateMatch(
        campsite: Campsite,
        preferences: CamperPreferences,
        lotteryContext: LotteryContext? = null
    ): MatchResult {
        val highlights = mutableListOf<String>()

        // 1. Tag / Amenity Score (40% weight)
        val tagScore = computeTagScore(campsite.tags, preferences.preferredTags, highlights)

        // 2. Lottery Odds Score (25% weight)
        val oddsScore = computeOddsScore(lotteryContext, preferences.prioritizeHighWinOdds, highlights)

        // 3. Budget Fit Score (20% weight)
        val budgetScore = computeBudgetScore(campsite.pricePerNight, preferences.maxPricePerNight, highlights)

        // 4. Rating Quality Score (15% weight)
        val ratingScore = computeRatingScore(campsite.ratingAverage, campsite.ratingCount, preferences.minRating, highlights)

        // Weighted sum (0.0 to 1.0)
        var totalRawScore = (tagScore * TAG_WEIGHT) +
            (oddsScore * ODDS_WEIGHT) +
            (budgetScore * BUDGET_WEIGHT) +
            (ratingScore * RATING_WEIGHT)

        // Party size constraint: penalize if site cannot fit requested occupancy
        if (preferences.minOccupancy != null && preferences.minOccupancy > 0) {
            if (campsite.maxOccupancy < preferences.minOccupancy) {
                totalRawScore *= (campsite.maxOccupancy.toDouble() / preferences.minOccupancy).coerceIn(0.1, 1.0)
            } else {
                highlights.add("Accommodates your party of ${preferences.minOccupancy}")
            }
        }

        val percentage = (totalRawScore * 100.0).roundToInt().coerceIn(0, 100)

        val breakdown = MatchScoreBreakdown(
            tagScore = tagScore,
            oddsScore = oddsScore,
            budgetScore = budgetScore,
            ratingScore = ratingScore
        )

        return MatchResult(
            campsiteId = campsite.id,
            matchPercentage = percentage,
            matchHighlights = highlights,
            breakdown = breakdown
        )
    }

    /**
     * Ranks a collection of campsites against [preferences], returning matches sorted descending by percentage.
     *
     * @param campsites list of campsite entities to score.
     * @param preferences active camper criteria.
     * @param lotteryContexts map of campsite active lottery ids to their [LotteryContext] stats.
     * @return sorted list of [MatchResult] records with highest match first.
     */
    fun rankCampsites(
        campsites: List<Campsite>,
        preferences: CamperPreferences,
        lotteryContexts: Map<String, LotteryContext> = emptyMap()
    ): List<MatchResult> {
        return campsites.map { site ->
            val context = site.activeLotteryId?.let { lotteryContexts[it] }
            calculateMatch(site, preferences, context)
        }.sortedByDescending { it.matchPercentage }
    }

    private fun computeTagScore(
        campsiteTags: List<String>,
        preferredTags: Set<String>,
        highlights: MutableList<String>
    ): Double {
        if (preferredTags.isEmpty()) {
            return 1.0 // neutral full score if no tag preference specified
        }

        val matchingTags = preferredTags.filter { pref ->
            campsiteTags.any { matchesTag(it, pref) }
        }

        if (matchingTags.size == preferredTags.size) {
            highlights.add("Matches all ${matchingTags.size} desired amenities")
        } else if (matchingTags.isNotEmpty()) {
            highlights.add("Matches ${matchingTags.size} of ${preferredTags.size} desired amenities")
        }

        return (matchingTags.size.toDouble() / preferredTags.size).coerceIn(0.0, 1.0)
    }

    private fun computeOddsScore(
        lotteryContext: LotteryContext?,
        prioritizeOdds: Boolean,
        highlights: MutableList<String>
    ): Double {
        if (lotteryContext == null) {
            return 0.75 // neutral baseline for regular listings without an active lottery
        }

        val probability = lotteryContext.estimatedWinProbability
        val percentage = (probability * 100).roundToInt()

        if (percentage >= 70) {
            highlights.add("High lottery win odds ($percentage%)")
        } else if (percentage >= 40) {
            highlights.add("Moderate lottery win odds ($percentage%)")
        }

        return if (prioritizeOdds) {
            probability
        } else {
            0.50 + (probability * 0.50)
        }
    }

    private fun computeBudgetScore(
        pricePerNight: Double,
        maxPrice: Double?,
        highlights: MutableList<String>
    ): Double {
        if (maxPrice == null) {
            return 1.0
        }

        return if (pricePerNight <= maxPrice) {
            highlights.add("Within your budget ($${pricePerNight.toInt()}/night)")
            1.0
        } else {
            // smooth decay when above budget
            (maxPrice / pricePerNight).coerceIn(0.0, 1.0)
        }
    }

    private fun computeRatingScore(
        ratingAverage: Double,
        ratingCount: Int,
        minRating: Double?,
        highlights: MutableList<String>
    ): Double {
        if (ratingCount == 0) {
            return 0.70 // neutral baseline for newly listed, unrated campsites
        }

        val normalized = (ratingAverage / 5.0).coerceIn(0.0, 1.0)

        if (ratingAverage >= 4.5) {
            highlights.add("Top rated campsite (${String.format(java.util.Locale.US, "%.1f", ratingAverage)} stars)")
        }

        return if (minRating != null && ratingAverage < minRating) {
            normalized * 0.50 // penalty if below user's minimum acceptable rating
        } else {
            normalized
        }
    }

    /**
     * Case- and punctuation-tolerant tag matching so campsite owner inputs like "fire-pit" or "firepit"
     * reliably match entrant preference tags like "Fire pit".
     */
    fun matchesTag(tagA: String, tagB: String): Boolean {
        if (tagA.equals(tagB, ignoreCase = true)) return true
        val normA = tagA.replace(" ", "").replace("-", "").lowercase()
        val normB = tagB.replace(" ", "").replace("-", "").lowercase()
        if (normA.isEmpty() || normB.isEmpty()) return false
        return normA == normB || (normA.length >= 4 && normB.length >= 4 && (normA.contains(normB) || normB.contains(normA)))
    }
}
