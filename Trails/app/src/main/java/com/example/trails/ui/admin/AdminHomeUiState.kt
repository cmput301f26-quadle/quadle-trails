// State and greeting logic for the admin's home screen (Figma Scenario 8 · Admin home).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.04.01, 03.05.01
// Outstanding issues: the Images, Logs and Needs review parts of the Figma home come in Part 4.
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.User

/**
 * Everything the admin home needs to draw.
 *
 * @property greeting e.g. "Good morning, Maya", or just "Good morning" if the admin has no name.
 * @property campsiteCount every campsite in the app, shown on the Campsites tile.
 * @property profileCount every user profile in the app, shown on the Profiles tile.
 */
data class AdminHomeUiState(
    val greeting: String,
    val campsiteCount: Int,
    val profileCount: Int
)

/**
 * The greeting for a time of day, without a name.
 *
 * @param hour hour of the day, 0–23.
 * @return "Good morning" before 12, "Good afternoon" from 12 to before 17, otherwise "Good evening".
 */
fun greeting(hour: Int): String = when {
    hour < 12 -> "Good morning"
    hour < 17 -> "Good afternoon"
    else -> "Good evening"
}

/**
 * Builds the admin home state from the signed-in admin, all campsites and all users.
 * Kept free of coroutines so it can be unit-tested directly.
 *
 * @param user the signed-in admin, or null if their profile hasn't loaded or doesn't exist.
 * @param campsites every campsite in the app.
 * @param users every user profile in the app.
 * @param hour current hour of the day, 0–23, used to pick the greeting.
 * @return the state for the given inputs.
 */
fun buildAdminHomeState(
    user: User?,
    campsites: List<Campsite>,
    users: List<User>,
    hour: Int
): AdminHomeUiState {
    val firstName = user?.name?.trim()?.substringBefore(' ').orEmpty()
    val text = if (firstName.isEmpty()) greeting(hour) else "${greeting(hour)}, $firstName"
    return AdminHomeUiState(text, campsites.size, users.size)
}
