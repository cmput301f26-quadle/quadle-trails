// State and list-building logic for the admin's Profiles screen (US 03.05.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Outstanding issues: no reservation, lottery-entry or policy-report
//   counts yet (Part 4 data).
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.data.model.User

/** The filter chips above the admin's Profiles list. */
enum class ProfileFilter {
    /** Every user. */
    ALL,
    /** Users who hold the OWNER role. */
    OWNERS,
    /**
     * Plain entrants: users who hold neither OWNER nor ADMIN. Every user holds ENTRANT
     * by default, so owners and admins are left out here even if they also hold it.
     */
    ENTRANTS,
    /** Users who hold the ADMIN role. */
    ADMINS
}

/**
 * One row on the admin's Profiles list.
 *
 * @property user the profile shown; tapping the row opens it by [User.deviceId].
 * @property campsiteCount number of campsites this user owns.
 */
data class AdminProfileItem(val user: User, val campsiteCount: Int)

/**
 * Everything the admin's Profiles screen needs to draw.
 *
 * @property items profiles matching [query] and [filter], sorted by name.
 * @property query the current search text.
 * @property filter the selected filter chip.
 * @property totalCount all profiles, ignoring the search and filter.
 */
data class AdminProfilesUiState(
    val items: List<AdminProfileItem>,
    val query: String,
    val filter: ProfileFilter,
    val totalCount: Int
)

/**
 * Pairs each user with their campsite count, applies the filter and search, and counts.
 * Kept free of coroutines so it can be unit-tested directly.
 *
 * @param users every user profile in the app.
 * @param campsites every campsite, used to count how many each user owns by device id.
 * @param query search text matched against name and email, ignoring case.
 * @param filter which roles to keep; a user who is both an owner and an admin appears under
 *   both of those chips, while [ProfileFilter.ENTRANTS] keeps only plain entrants.
 * @return the state for the given inputs.
 */
fun buildAdminProfilesState(
    users: List<User>,
    campsites: List<Campsite>,
    query: String,
    filter: ProfileFilter
): AdminProfilesUiState {
    val all = users.map { user ->
        AdminProfileItem(user, campsites.count { it.ownerId == user.deviceId })
    }
    val q = query.trim()
    val shown = all
        .filter { matchesFilter(it.user, filter) }
        .filter {
            q.isEmpty() ||
                it.user.name.contains(q, ignoreCase = true) ||
                it.user.email.contains(q, ignoreCase = true)
        }
        .sortedWith(compareBy({ it.user.name.isBlank() }, { it.user.name.trim().lowercase() }))
    return AdminProfilesUiState(shown, query, filter, all.size)
}

/**
 * Whether [user] belongs under [filter].
 *
 * @param user the profile being checked.
 * @param filter the selected chip.
 * @return true if the user holds the chip's role (for [ProfileFilter.ENTRANTS], holds neither
 *   OWNER nor ADMIN), or always for [ProfileFilter.ALL].
 */
private fun matchesFilter(user: User, filter: ProfileFilter): Boolean = when (filter) {
    ProfileFilter.ALL -> true
    ProfileFilter.OWNERS -> Role.OWNER in user.roles
    ProfileFilter.ENTRANTS -> Role.OWNER !in user.roles && Role.ADMIN !in user.roles
    ProfileFilter.ADMINS -> Role.ADMIN in user.roles
}

/**
 * The single role used to label a user on the list.
 *
 * @param user the profile being labelled.
 * @return ADMIN if held, else OWNER if held, else ENTRANT.
 */
fun primaryRole(user: User): Role = when {
    Role.ADMIN in user.roles -> Role.ADMIN
    Role.OWNER in user.roles -> Role.OWNER
    else -> Role.ENTRANT
}

/**
 * The grey line under a user's name, e.g. "Owner · 2 campsites".
 *
 * @param item the row being drawn.
 * @return "Owner · N campsites" (singular for 1) for owners, otherwise "Admin" or "Entrant".
 */
fun adminProfileSubtitle(item: AdminProfileItem): String = when (primaryRole(item.user)) {
    Role.ADMIN -> "Admin"
    Role.OWNER ->
        if (item.campsiteCount == 1) "Owner · 1 campsite"
        else "Owner · ${item.campsiteCount} campsites"
    Role.ENTRANT -> "Entrant"
}

/**
 * The name shown on a user's row.
 *
 * @param user the profile being drawn.
 * @return the user's name, or "Unnamed user" if it is blank.
 */
fun adminProfileDisplayName(user: User): String =
    user.name.takeIf { it.isNotBlank() } ?: "Unnamed user"

/**
 * The count line above the list, e.g. "12 profiles".
 *
 * @param totalCount number of profiles in the app.
 * @return "1 profile" for one, otherwise "N profiles".
 */
fun adminProfileCountLine(totalCount: Int): String =
    if (totalCount == 1) "1 profile" else "$totalCount profiles"
