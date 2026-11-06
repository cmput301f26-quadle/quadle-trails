// Rating rules and average calculation for rating a campsite (US 01.13.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Outstanding issues: anyone except the owner can rate; limiting rating
//   to completed stays needs reservations (Part 4).
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Rating

/** Longest review text kept when a rating is saved; anything longer is cut off. */
const val MAX_REVIEW_LENGTH = 500

/**
 * The summary numbers stored on a campsite and shown on its details screen.
 *
 * @property average mean score across all ratings, or 0.0 if there are none.
 * @property count number of ratings.
 */
data class RatingStats(val average: Double, val count: Int)

/**
 * Works out the average score and the number of ratings.
 *
 * @param ratings every rating for one campsite.
 * @return 0.0 and 0 for an empty list, otherwise the mean of the scores and the list size.
 */
fun ratingStats(ratings: List<Rating>): RatingStats =
    if (ratings.isEmpty()) RatingStats(0.0, 0)
    else RatingStats(ratings.map { it.score }.average(), ratings.size)

/**
 * Whether a user is allowed to rate a campsite. Owners can't rate their own site.
 *
 * @param userId device id of the user trying to rate.
 * @param campsite the campsite being rated.
 * @return false if [userId] is blank or matches [Campsite.ownerId], otherwise true.
 */
fun canRate(userId: String, campsite: Campsite): Boolean =
    userId.isNotBlank() && userId != campsite.ownerId

/**
 * Whether a score is a valid star rating.
 *
 * @param score the score to check.
 * @return true for 1 to 5 inclusive.
 */
fun isValidScore(score: Int): Boolean = score in 1..5

/**
 * The word shown next to a star score.
 *
 * @param score a score from 1 to 5.
 * @return "Poor", "Fair", "Good", "Great" or "Excellent", or "" for anything else.
 */
fun scoreLabel(score: Int): String = when (score) {
    1 -> "Poor"
    2 -> "Fair"
    3 -> "Good"
    4 -> "Great"
    5 -> "Excellent"
    else -> ""
}

/**
 * A one-line description of a score, e.g. "4 out of 5 · Great".
 *
 * @param score a score from 1 to 5.
 * @return the score out of 5 followed by its [scoreLabel].
 */
fun scoreSummary(score: Int): String = "$score out of 5 · ${scoreLabel(score)}"
