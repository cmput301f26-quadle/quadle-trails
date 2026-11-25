// Result model holding calculated match scores and reason highlights for a campsite (Model in MVVM).
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.model

/**
 * Breakdown of subscores used to arrive at the final match percentage.
 *
 * @property tagScore tag similarity score (0.0 to 1.0, 40% weight).
 * @property oddsScore lottery accessibility / win odds score (0.0 to 1.0, 25% weight).
 * @property budgetScore nightly price fit score (0.0 to 1.0, 20% weight).
 * @property ratingScore customer satisfaction / star rating score (0.0 to 1.0, 15% weight).
 */
data class MatchScoreBreakdown(
    val tagScore: Double,
    val oddsScore: Double,
    val budgetScore: Double,
    val ratingScore: Double
)

/**
 * Result of matching a campsite against camper preferences.
 *
 * NOTE FOR TEAMMATES:
 * - Ibrahim (Campsite Discovery & Cards): You can use [matchPercentage] to display a green badge
 *   (e.g. "92% Match") and [matchHighlights] for subtitle bullet points (e.g. "High lottery odds (80%)").
 * - Zainab (Admin & Ratings): The [ratingScore] rewards campsites with consistent 4.5+ star ratings.
 *
 * @property campsiteId unique document identifier of the matched campsite.
 * @property matchPercentage overall compatibility percentage (0 to 100).
 * @property matchHighlights human-readable bullet points explaining why this site matched.
 * @property breakdown detailed subscore components for testing and inspection.
 */
data class MatchResult(
    val campsiteId: String,
    val matchPercentage: Int,
    val matchHighlights: List<String>,
    val breakdown: MatchScoreBreakdown
)
