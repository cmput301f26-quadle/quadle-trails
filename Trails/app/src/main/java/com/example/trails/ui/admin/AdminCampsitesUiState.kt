// State and list-building logic for the admin's Campsites screen (US 03.04.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Outstanding issues: no Published/Drafts filter or newest-first sort; the
//   Campsite model has no status or created date. Removing a campsite is done
//   from the admin Campsite page (US 03.01.01).
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.User

/**
 * One card on the admin's Campsites list.
 *
 * @property campsite the campsite shown; tapping the card opens it by [Campsite.id].
 * @property ownerName display name of the campsite's owner, or "Unknown owner".
 */
data class AdminCampsiteItem(val campsite: Campsite, val ownerName: String)

/**
 * Everything the admin's Campsites screen needs to draw.
 *
 * @property items campsites matching [query], sorted by name.
 * @property query the current search text.
 * @property totalCount all campsites, ignoring the search.
 */
data class AdminCampsitesUiState(
    val items: List<AdminCampsiteItem>,
    val query: String,
    val totalCount: Int
)

/**
 * Pairs each campsite with its owner's name, applies the search, and counts.
 * Kept free of coroutines so it can be unit-tested directly.
 *
 * @param campsites every campsite in the app.
 * @param users every user profile, used to look up owner names by device id.
 * @param query search text matched against campsite name, location, and owner name, ignoring case.
 * @return the state for the given inputs.
 */
fun buildAdminCampsitesState(
    campsites: List<Campsite>,
    users: List<User>,
    query: String
): AdminCampsitesUiState {
    val all = campsites.map { campsite ->
        AdminCampsiteItem(campsite, ownerName(campsite.ownerId, users))
    }
    val q = query.trim()
    val shown = all
        .filter {
            q.isEmpty() ||
                it.campsite.name.contains(q, ignoreCase = true) ||
                it.campsite.locationName.contains(q, ignoreCase = true) ||
                it.ownerName.contains(q, ignoreCase = true)
        }
        .sortedBy { it.campsite.name.lowercase() }
    return AdminCampsitesUiState(shown, query, all.size)
}

/**
 * The line under a campsite's name, e.g. "Jasper, AB · Owner Maya Singh".
 *
 * @param item the card being drawn.
 * @return location and owner, or just "Owner <name>" if the location is blank.
 */
fun adminCampsiteSubtitle(item: AdminCampsiteItem): String =
    if (item.campsite.locationName.isBlank()) "Owner ${item.ownerName}"
    else "${item.campsite.locationName} · Owner ${item.ownerName}"

/**
 * The count line above the list, e.g. "12 campsites · all owners".
 *
 * @param totalCount number of campsites in the app.
 * @return "1 campsite · all owners" for one, otherwise "N campsites · all owners".
 */
fun adminCampsiteCountLine(totalCount: Int): String =
    if (totalCount == 1) "1 campsite · all owners"
    else "$totalCount campsites · all owners"
