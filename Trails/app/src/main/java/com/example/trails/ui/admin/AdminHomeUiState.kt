// State and greeting logic for the admin's home screen (Figma Scenario 8 · Admin home).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.04.01, 03.05.01, 03.07.01
// Outstanding issues: the Images and Logs parts of the Figma home come in Part 4.
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Report
import com.example.trails.data.model.User
import com.google.firebase.Timestamp

/**
 * Everything the admin home needs to draw.
 *
 * @property greeting e.g. "Good morning, Maya", or just "Good morning" if the admin has no name.
 * @property campsiteCount every campsite in the app, shown on the Campsites tile.
 * @property profileCount every user profile in the app, shown on the Profiles tile.
 * @property openReportCount open policy reports waiting for review, the same ones the Policy
 *   reports list shows; the Needs review card is hidden when this is 0.
 * @property reportCountLine the card's main line, e.g. "3 policy reports".
 * @property oldestReportLine the card's second line, e.g. "Oldest report: 2 days ago", or null if
 *   no open report has a date.
 */
data class AdminHomeUiState(
    val greeting: String,
    val campsiteCount: Int,
    val profileCount: Int,
    val openReportCount: Int = 0,
    val reportCountLine: String = "",
    val oldestReportLine: String? = null
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
 * Builds the admin home state from the signed-in admin, all campsites, all users and all reports.
 * Kept free of coroutines so it can be unit-tested directly.
 *
 * @param user the signed-in admin, or null if their profile hasn't loaded or doesn't exist.
 * @param campsites every campsite in the app.
 * @param users every user profile in the app.
 * @param hour current hour of the day, 0–23, used to pick the greeting.
 * @param reports every policy report; only the open ones [buildReportedOwners] keeps are counted.
 * @param now the current time, which the report window and the oldest report's age are measured from.
 * @return the state for the given inputs.
 */
fun buildAdminHomeState(
    user: User?,
    campsites: List<Campsite>,
    users: List<User>,
    hour: Int,
    reports: List<Report> = emptyList(),
    now: Timestamp = Timestamp.now()
): AdminHomeUiState {
    val firstName = user?.name?.trim()?.substringBefore(' ').orEmpty()
    val text = if (firstName.isEmpty()) greeting(hour) else "${greeting(hour)}, $firstName"
    val owners = buildReportedOwners(reports, users, campsites, now)
    val openCount = owners.sumOf { it.openReports.size }
    val oldestAt = owners.mapNotNull { it.oldestReportAt }.minOrNull()
    return AdminHomeUiState(
        greeting = text,
        campsiteCount = campsites.size,
        profileCount = users.size,
        openReportCount = openCount,
        reportCountLine = needsReviewLine(openCount, null, now),
        oldestReportLine = oldestReportLine(oldestAt, now)
    )
}
