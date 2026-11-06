// State and display logic for the admin's read-only Profile page (US 03.05.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Outstanding issues: no reservation, lottery-entry or report rows and no
//   Remove profile button yet (Part 4, US 03.02.01).
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/** Every situation the admin's Profile page can be in. */
sealed interface AdminProfileUiState {
    /** Waiting for the user to load. */
    data object Loading : AdminProfileUiState

    /** No user has that id (e.g. the profile was removed). */
    data object NotFound : AdminProfileUiState

    /**
     * The user is loaded.
     *
     * @property user the profile to show.
     * @property campsiteCount number of campsites this user owns.
     */
    data class Loaded(val user: User, val campsiteCount: Int) : AdminProfileUiState
}

/**
 * Picks the page state from the latest user and campsites.
 * Kept free of coroutines so it can be unit-tested directly.
 *
 * @param user the profile, or null if no user has the requested id.
 * @param campsites every campsite, used to count the ones this user owns by device id.
 * @return [AdminProfileUiState.NotFound] or [AdminProfileUiState.Loaded].
 */
fun buildAdminProfileState(user: User?, campsites: List<Campsite>): AdminProfileUiState =
    if (user == null) AdminProfileUiState.NotFound
    else AdminProfileUiState.Loaded(user, campsites.count { it.ownerId == user.deviceId })

/**
 * The role chips shown under a user's name.
 *
 * @param user the profile being drawn.
 * @return "Admin", "Owner" and "Entrant", in that order, keeping only roles the user holds.
 */
fun roleLabels(user: User): List<String> = buildList {
    if (Role.ADMIN in user.roles) add("Admin")
    if (Role.OWNER in user.roles) add("Owner")
    if (Role.ENTRANT in user.roles) add("Entrant")
}

/**
 * When the user joined, e.g. "Joined Jan 2027".
 *
 * @param createdAt when the profile was created, or null if unknown.
 * @param locale locale used for the month name; defaults to US English.
 * @return the label, or null if [createdAt] is null.
 */
fun joinedLabel(createdAt: Timestamp?, locale: Locale = Locale.US): String? =
    createdAt?.let { "Joined " + SimpleDateFormat("MMM yyyy", locale).format(it.toDate()) }

/**
 * The grey line under the user's name, e.g. "priya@example.com · Joined Jan 2027".
 *
 * @param user the profile being drawn.
 * @return the email and joined date separated by " · ", only the part that exists if one is
 *   missing, or "" if both are missing.
 */
fun emailAndJoinedLine(user: User): String =
    listOfNotNull(user.email.trim().takeIf { it.isNotEmpty() }, joinedLabel(user.createdAt))
        .joinToString(" · ")

/**
 * The value shown on the Phone row.
 *
 * @param phone the user's phone number, possibly blank.
 * @return the phone number, or "Not provided" if it is blank.
 */
fun phoneLabel(phone: String): String = phone.trim().ifEmpty { "Not provided" }
