// State and display logic for the admin's Profile page (US 03.05.01) and its
//   Remove profile dialog (US 03.02.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Outstanding issues:
//   - No reservation or report rows yet (Part 4).
//   - The remove message doesn't mention reservations, which don't exist yet (Part 4).
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.data.model.WaitlistEntry
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
     * @property isSelf true if this is the signed-in admin's own profile, which can't be removed.
     * @property lotteryEntryCount the user's waitlist entries across all lotteries.
     * @property removal where the Remove profile flow is.
     */
    data class Loaded(
        val user: User,
        val campsiteCount: Int,
        val isSelf: Boolean = false,
        val lotteryEntryCount: Int = 0,
        val removal: ProfileRemovalState = ProfileRemovalState.Idle
    ) : AdminProfileUiState
}

/** Where the Remove profile flow is. */
sealed interface ProfileRemovalState {
    /** No dialog open and nothing running. */
    data object Idle : ProfileRemovalState

    /** The "Remove profile?" dialog is open, waiting for Cancel or Remove. */
    data object Confirming : ProfileRemovalState

    /** The admin tapped Remove; the dialog stays open with its buttons disabled. */
    data object Removing : ProfileRemovalState

    /** The last removal failed; the dialog is closed and an error is shown. */
    data object Error : ProfileRemovalState
}

/**
 * Picks the page state from the latest user, campsites, waitlist entries and removal progress.
 * Kept free of coroutines so it can be unit-tested directly.
 *
 * @param user the profile, or null if no user has the requested id.
 * @param campsites every campsite, used to count the ones this user owns by device id.
 * @param entries waitlist entries to count; only ones whose userId is this user's are counted.
 * @param currentUserId device id of the signed-in admin, to tell if this is their own profile.
 * @param removal where the Remove profile flow is.
 * @return [AdminProfileUiState.NotFound] or [AdminProfileUiState.Loaded].
 */
fun buildAdminProfileState(
    user: User?,
    campsites: List<Campsite>,
    entries: List<WaitlistEntry> = emptyList(),
    currentUserId: String = "",
    removal: ProfileRemovalState = ProfileRemovalState.Idle
): AdminProfileUiState =
    if (user == null) AdminProfileUiState.NotFound
    else AdminProfileUiState.Loaded(
        user = user,
        campsiteCount = campsites.count { it.ownerId == user.deviceId },
        isSelf = user.deviceId == currentUserId,
        lotteryEntryCount = entries.count { it.userId == user.deviceId },
        removal = removal
    )

/**
 * The body of the "Remove profile?" dialog.
 *
 * @param name the user's display name.
 * @param campsiteCount campsites they own, removed with them; left out of the message when 0.
 * @param entryCount their lottery entries, removed with them.
 * @return e.g. "Maya Singh loses access to Quadle. Their 2 campsites and 1 lottery entry are
 *   removed. This can't be undone."
 */
fun removeProfileMessage(name: String, campsiteCount: Int, entryCount: Int): String {
    val entries = if (entryCount == 1) "1 lottery entry" else "$entryCount lottery entries"
    val what = when (campsiteCount) {
        0 -> entries
        1 -> "1 campsite and $entries"
        else -> "$campsiteCount campsites and $entries"
    }
    val verb = if (campsiteCount == 0 && entryCount == 1) "is" else "are"
    return "$name loses access to Quadle. Their $what $verb removed. This can't be undone."
}

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
