// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01, 03.04.01, 03.09.01
// PURPOSE: State and display logic for the admin's Campsite page, where an
//   administrator reviews one campsite and its ratings, and can remove either.
// Outstanding issues:
//   - The Lotteries and Lottery entries counts are what removal would delete
//     (lotteries where this is the only campsite), not shared lotteries.
//   - Reservations don't exist yet, so they aren't counted (Part 4).
//   - Guests with reservations aren't notified and reservations aren't cancelled
//     (needs reservations and notifications, Part 4).
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.User
import java.util.Locale

/** Every situation the admin's Campsite page can be in. */
sealed interface AdminCampsiteUiState {
    /** Waiting for the campsite to load. */
    data object Loading : AdminCampsiteUiState

    /** No campsite has that id (e.g. it was just removed). */
    data object NotFound : AdminCampsiteUiState

    /**
     * The campsite is loaded.
     *
     * @property campsite the campsite to show.
     * @property ownerName display name of its owner, or "Unknown owner".
     * @property lotteryCount lotteries that are removed with it.
     * @property entryCount waitlist entries in those lotteries.
     * @property ratingCount the campsite's ratings, removed with it.
     * @property ratings the campsite's ratings, newest first (US 03.09.01).
     */
    data class Loaded(
        val campsite: Campsite,
        val ownerName: String,
        val lotteryCount: Int,
        val entryCount: Int,
        val ratingCount: Int = 0,
        val ratings: List<AdminRatingItem> = emptyList()
    ) : AdminCampsiteUiState
}

/**
 * Progress of a removal the admin confirmed.
 *
 * @property removing true while the removal is running; the dialog buttons are disabled.
 * @property failed true if the last attempt failed, so an error message is shown.
 * @property removed true once the campsite is gone, so the screen goes back to the list.
 */
data class AdminCampsiteRemoveState(
    val removing: Boolean = false,
    val failed: Boolean = false,
    val removed: Boolean = false
)

/**
 * Picks the page state from the latest campsite, users, removal impact and ratings.
 * Kept free of coroutines so it can be unit-tested directly.
 *
 * @param campsite the campsite, or null if none has the requested id.
 * @param users every user profile, used to look up the owner's name by device id.
 * @param impact what removing the campsite would delete, or null if unknown (counts as 0).
 * @param ratings the campsite's ratings as cards, from [buildAdminRatingItems].
 * @return [AdminCampsiteUiState.NotFound] or [AdminCampsiteUiState.Loaded].
 */
fun buildAdminCampsiteState(
    campsite: Campsite?,
    users: List<User>,
    impact: CampsiteImpact?,
    ratings: List<AdminRatingItem> = emptyList()
): AdminCampsiteUiState =
    if (campsite == null) AdminCampsiteUiState.NotFound
    else AdminCampsiteUiState.Loaded(
        campsite = campsite,
        ownerName = ownerName(campsite.ownerId, users),
        lotteryCount = impact?.lotteryCount ?: 0,
        entryCount = impact?.entryCount ?: 0,
        ratingCount = impact?.ratingCount ?: 0,
        ratings = ratings
    )

/**
 * The name shown for a campsite's owner.
 *
 * @param ownerId device id of the owner.
 * @param users every user profile.
 * @return the owner's name, or "Unknown owner" if no user matches or the name is blank.
 */
fun ownerName(ownerId: String, users: List<User>): String =
    users.find { it.deviceId == ownerId }?.name?.takeIf { it.isNotBlank() } ?: "Unknown owner"

/**
 * The value on the Rating row, e.g. "★ 4.5 (12)".
 *
 * @param campsite the campsite being shown.
 * @return the average to one decimal with the count, or "No ratings" if it has none.
 */
fun ratingLabel(campsite: Campsite): String =
    if (campsite.ratingCount <= 0) "No ratings"
    else String.format(Locale.US, "★ %.1f (%d)", campsite.ratingAverage, campsite.ratingCount)

/**
 * The body of the "Remove campsite?" dialog.
 *
 * @param name the campsite's name.
 * @param lotteryCount lotteries removed with it.
 * @param entryCount lottery entries removed with it.
 * @param ratingCount ratings removed with it.
 * @return e.g. "Jasper Pines is removed for everyone. Its 2 lotteries, 1 lottery entry and
 *   3 ratings are removed too. This can't be undone." Counts of 0 are left out; if all are 0
 *   the middle sentence is left out.
 */
fun removeCampsiteMessage(name: String, lotteryCount: Int, entryCount: Int, ratingCount: Int = 0): String {
    val parts = listOfNotNull(
        countPart(lotteryCount, "lottery", "lotteries"),
        countPart(entryCount, "lottery entry", "lottery entries"),
        countPart(ratingCount, "rating", "ratings")
    )
    val removed = when (parts.size) {
        0 -> ""
        1 -> "Its ${parts[0]} ${if (parts[0].startsWith("1 ")) "is" else "are"} removed too. "
        else -> "Its ${parts.dropLast(1).joinToString(", ")} and ${parts.last()} are removed too. "
    }
    return "$name is removed for everyone. ${removed}This can't be undone."
}

/** "1 [one]" or "[count] [many]", or null when [count] is 0 so the part is left out. */
private fun countPart(count: Int, one: String, many: String): String? = when {
    count <= 0 -> null
    count == 1 -> "1 $one"
    else -> "$count $many"
}
