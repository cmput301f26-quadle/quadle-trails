// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.09.01
// PURPOSE: State and display logic for the "Ratings & reviews" section on the
//   admin's Campsite page, where an administrator removes ratings that violate
//   app policy.
// Outstanding issues:
//   - There is no way for campers to flag a rating yet, so the admin reads
//     every rating to find the ones that break policy.
//   - AppContainer still uses FakeRatingRepository; FirestoreRatingRepository
//     isn't switched on until ratings are seeded in Firestore.
package com.example.trails.ui.admin

import com.example.trails.data.model.Rating
import com.example.trails.data.model.User
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * One rating as the admin sees it.
 *
 * @property rating the stored rating (score, review, reviewer id).
 * @property reviewerName display name of the reviewer, or "Unknown user".
 * @property dateLabel when it was submitted, e.g. "Oct 3, 2026", or "" if unknown.
 */
data class AdminRatingItem(
    val rating: Rating,
    val reviewerName: String,
    val dateLabel: String
)

/**
 * Progress of removing one rating. The dialog is open while [confirmingUserId] is set.
 *
 * @property confirmingUserId reviewer whose rating the "Remove rating?" dialog is about, or null.
 * @property removing true while the removal runs; the dialog buttons are disabled.
 * @property failed true if the last attempt failed, so an error message is shown.
 */
data class AdminRatingRemoveState(
    val confirmingUserId: String? = null,
    val removing: Boolean = false,
    val failed: Boolean = false
)

/**
 * Turns a campsite's ratings into the cards shown to the admin, newest first.
 * Ratings with no date go last.
 *
 * @param ratings every rating of one campsite.
 * @param users every user profile, used to look up reviewer names by device id.
 * @param locale locale used for the month name; defaults to US English.
 * @return one [AdminRatingItem] per rating.
 */
fun buildAdminRatingItems(
    ratings: List<Rating>,
    users: List<User>,
    locale: Locale = Locale.US
): List<AdminRatingItem> {
    val names = users.associate { it.deviceId to it.name }
    val format = SimpleDateFormat("MMM d, yyyy", locale)
    return ratings
        .sortedByDescending { it.createdAt?.toDate()?.time ?: Long.MIN_VALUE }
        .map { rating ->
            AdminRatingItem(
                rating = rating,
                reviewerName = names[rating.userId]?.takeIf { it.isNotBlank() } ?: "Unknown user",
                dateLabel = rating.createdAt?.let { format.format(it.toDate()) }.orEmpty()
            )
        }
}

/**
 * The body of the "Remove rating?" dialog.
 *
 * @param reviewerName who wrote the rating.
 * @param score the rating's star score.
 * @return e.g. "Priya Sharma's 4-star rating is removed and the campsite's average is updated.
 *   This can't be undone."
 */
fun removeRatingMessage(reviewerName: String, score: Int): String =
    "$reviewerName's $score-star rating is removed and the campsite's average is updated. " +
        "This can't be undone."
