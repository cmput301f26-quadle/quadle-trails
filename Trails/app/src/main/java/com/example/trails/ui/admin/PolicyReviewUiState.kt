// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: State and display logic for the admin's policy review: the list of
//   reported campsite owners and the review page for one owner, where the admin
//   removes the owner or dismisses their reports.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Report
import com.example.trails.data.model.ReportStatus
import com.example.trails.data.model.User
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/** How far back the admin looks: only open reports from the last this-many days count. */
const val REPORT_WINDOW_DAYS = 30L

/**
 * One campsite owner waiting for policy review, shown as a row in the reported owners list.
 *
 * @property user the reported owner.
 * @property openReports their open reports from the last [REPORT_WINDOW_DAYS] days, newest first.
 * @property campsiteCount campsites the owner has listed.
 * @property oldestReportAt when the oldest of [openReports] was filed, or null if none has a date.
 */
data class ReportedOwner(
    val user: User,
    val openReports: List<Report>,
    val campsiteCount: Int,
    val oldestReportAt: Timestamp?
)

/** Every situation the admin's policy review page for one owner can be in. */
sealed interface PolicyReviewUiState {
    /** Waiting for the owner to load. */
    data object Loading : PolicyReviewUiState

    /** No user has that id (e.g. the owner was already removed). */
    data object NotFound : PolicyReviewUiState

    /**
     * The owner and their reports are loaded.
     *
     * @property user the reported owner.
     * @property campsiteCount campsites the owner has listed.
     * @property reports their open reports from the last [REPORT_WINDOW_DAYS] days, newest first.
     * @property ruleBroken the rule broken most often among [reports], or null if there are none.
     * @property impact what removing the owner takes with them.
     * @property action where the Remove owner / Dismiss flow is.
     * @property isSelf true if this is the signed-in admin, who can't remove themselves.
     * @property campsiteNames display name of each campsite by id, for the report subtitles.
     */
    data class Loaded(
        val user: User,
        val campsiteCount: Int,
        val reports: List<Report>,
        val ruleBroken: String?,
        val impact: ProfileImpact,
        val action: PolicyActionState = PolicyActionState.Idle,
        val isSelf: Boolean = false,
        val campsiteNames: Map<String, String> = emptyMap()
    ) : PolicyReviewUiState
}

/** Where the Remove owner / Dismiss reports flow is. */
sealed interface PolicyActionState {
    /** No dialog open and nothing running. */
    data object Idle : PolicyActionState

    /** The confirmation dialog is open, waiting for Cancel or the action. */
    data object Confirming : PolicyActionState

    /** The admin confirmed; buttons stay disabled until the write finishes. */
    data object Working : PolicyActionState

    /** The last action failed; an error is shown. */
    data object Error : PolicyActionState
}

/**
 * Builds the reported owners list: one entry per owner with open reports from the last
 * [REPORT_WINDOW_DAYS] days, the owner waiting longest first. Kept free of coroutines so it can
 * be unit-tested directly.
 *
 * @param reports every report; only OPEN ones inside the window are used.
 * @param users every user; owners with no matching user (already removed) are skipped.
 * @param campsites every campsite, used to count each owner's campsites.
 * @param now the current time, which the window is measured back from.
 * @return the reported owners, oldest report first; owners whose reports have no date go last.
 */
fun buildReportedOwners(
    reports: List<Report>,
    users: List<User>,
    campsites: List<Campsite>,
    now: Timestamp
): List<ReportedOwner> {
    val usersById = users.associateBy { it.deviceId }
    return openRecent(reports, now)
        .groupBy { it.ownerId }
        .mapNotNull { (ownerId, ownerReports) ->
            val user = usersById[ownerId] ?: return@mapNotNull null
            ReportedOwner(
                user = user,
                openReports = newestFirst(ownerReports),
                campsiteCount = campsites.count { it.ownerId == ownerId },
                oldestReportAt = ownerReports.mapNotNull { it.createdAt }.minOrNull()
            )
        }
        .sortedWith(compareBy(nullsLast()) { it.oldestReportAt })
}

/**
 * Picks the review page state for one owner. Kept free of coroutines so it can be unit-tested
 * directly.
 *
 * @param user the reported owner, or null if no user has the requested id.
 * @param reports reports to consider; only this owner's OPEN ones inside the window are kept.
 * @param campsites every campsite, used to count the ones this owner has listed.
 * @param impact what [AdminRemoval.profileImpact] says removing the owner takes with them, or
 *   null if it isn't known yet (then their campsite count and 0 entries are used).
 * @param currentUserId device id of the signed-in admin, to tell if this is their own profile.
 * @param now the current time, which the window is measured back from.
 * @param action where the Remove owner / Dismiss flow is.
 * @return [PolicyReviewUiState.NotFound] or [PolicyReviewUiState.Loaded].
 */
fun buildPolicyReviewState(
    user: User?,
    reports: List<Report>,
    campsites: List<Campsite>,
    impact: ProfileImpact?,
    currentUserId: String,
    now: Timestamp,
    action: PolicyActionState = PolicyActionState.Idle
): PolicyReviewUiState {
    if (user == null) return PolicyReviewUiState.NotFound
    val campsiteCount = campsites.count { it.ownerId == user.deviceId }
    val ownerReports = newestFirst(openRecent(reports, now).filter { it.ownerId == user.deviceId })
    return PolicyReviewUiState.Loaded(
        user = user,
        campsiteCount = campsiteCount,
        reports = ownerReports,
        ruleBroken = mostCommonRule(ownerReports),
        impact = impact ?: ProfileImpact(user.name, campsiteCount, 0),
        action = action,
        isSelf = user.deviceId == currentUserId,
        campsiteNames = campsites.associate { it.id to it.name }
    )
}

/**
 * The rule broken most often among [reports]; a tie goes to the rule on the newest report.
 *
 * @param reports reports newest first.
 * @return the rule, or null if [reports] is empty.
 */
fun mostCommonRule(reports: List<Report>): String? =
    reports.groupingBy { it.ruleBroken }.eachCount().maxByOrNull { it.value }?.key

/**
 * The grey line under a report's reason, e.g. "Pine Ridge · Site 04 · 2 campers · Apr 3".
 *
 * @param report the report being drawn.
 * @param campsiteName the reported campsite's display name.
 * @param locale locale used for the month name; defaults to US English.
 * @return the campsite, camper count and filing date separated by " · "; the date is left out
 *   if the report has none.
 */
fun reportSubtitle(report: Report, campsiteName: String, locale: Locale = Locale.US): String {
    val campers = if (report.reporterCount == 1) "1 camper" else "${report.reporterCount} campers"
    val date = report.createdAt?.let { SimpleDateFormat("MMM d", locale).format(it.toDate()) }
    return listOfNotNull(campsiteName, campers, date).joinToString(" · ")
}

/**
 * @param n how many reports.
 * @return "1 report" or e.g. "3 reports".
 */
fun reportCountLabel(n: Int): String = if (n == 1) "1 report" else "$n reports"

/**
 * The extra line in the Remove owner dialog saying what goes with the owner.
 *
 * @param campsiteCount campsites the owner has listed; left out when 0.
 * @param entryCount the owner's own lottery entries; left out when 0.
 * @return e.g. "Removing also takes down their 3 campsites and cancels 2 lottery entries.",
 *   or "" if both counts are 0.
 */
fun removeOwnerImpactLine(campsiteCount: Int, entryCount: Int): String {
    val parts = listOfNotNull(
        when (campsiteCount) {
            0 -> null
            1 -> "takes down their 1 campsite"
            else -> "takes down their $campsiteCount campsites"
        },
        when (entryCount) {
            0 -> null
            1 -> "cancels 1 lottery entry"
            else -> "cancels $entryCount lottery entries"
        }
    )
    return if (parts.isEmpty()) "" else "Removing also ${parts.joinToString(" and ")}."
}

/**
 * The "needs review" line, e.g. "3 policy reports · Oldest report: 2 days ago".
 *
 * @param openCount open reports waiting for review.
 * @param oldestAt when the oldest of them was filed, or null if unknown (then left out).
 * @param now the current time.
 * @return the count, then how long the oldest has waited: "today", "1 day ago" or "n days ago".
 */
fun needsReviewLine(openCount: Int, oldestAt: Timestamp?, now: Timestamp): String {
    val count = if (openCount == 1) "1 policy report" else "$openCount policy reports"
    return listOfNotNull(count, oldestReportLine(oldestAt, now)).joinToString(" · ")
}

/**
 * How long the oldest open report has waited, e.g. "Oldest report: 2 days ago".
 *
 * @param oldestAt when the oldest open report was filed, or null if unknown.
 * @param now the current time.
 * @return "Oldest report: " then "today", "1 day ago" or "n days ago", or null if [oldestAt] is null.
 */
fun oldestReportLine(oldestAt: Timestamp?, now: Timestamp): String? {
    if (oldestAt == null) return null
    val days = TimeUnit.MILLISECONDS.toDays(now.toDate().time - oldestAt.toDate().time).coerceAtLeast(0)
    val ago = when (days) {
        0L -> "today"
        1L -> "1 day ago"
        else -> "$days days ago"
    }
    return "Oldest report: $ago"
}

/**
 * How many open reports from the last [REPORT_WINDOW_DAYS] days are against one owner, the same
 * reports their Policy review page lists.
 *
 * @param reports reports to count; only [ownerId]'s OPEN ones inside the window are counted.
 * @param ownerId device id of the owner.
 * @param now the current time, which the window is measured back from.
 * @return the number of open reports.
 */
fun openReportCount(reports: List<Report>, ownerId: String, now: Timestamp): Int =
    openRecent(reports, now).count { it.ownerId == ownerId }

/** Every situation the reported owners list can be in. */
sealed interface ReportsUiState {
    /** Waiting for the reports to load. */
    data object Loading : ReportsUiState

    /**
     * The reports are loaded.
     *
     * @property owners the reported owners from [buildReportedOwners]; empty when none are open.
     */
    data class Loaded(val owners: List<ReportedOwner>) : ReportsUiState
}

/**
 * The grey line under an owner in the reported owners list, e.g. "3 reports · oldest Nov 1".
 *
 * @param owner the reported owner being drawn.
 * @param locale locale used for the month name; defaults to US English.
 * @return the report count and the oldest report's date, or just the count if it has no date.
 */
fun reportedOwnerSubtitle(owner: ReportedOwner, locale: Locale = Locale.US): String {
    val oldest = owner.oldestReportAt?.let { "oldest " + SimpleDateFormat("MMM d", locale).format(it.toDate()) }
    return listOfNotNull(reportCountLabel(owner.openReports.size), oldest).joinToString(" · ")
}

/**
 * The grey line under the owner's name on the review page, e.g.
 * "Owner · 3 campsites · Joined Nov 2026".
 *
 * @param user the reported owner.
 * @param campsiteCount campsites the owner has listed.
 * @param locale locale used for the month name; defaults to US English.
 * @return "Owner", the campsite count and, if known, the joined month, separated by " · ".
 */
fun policyOwnerLine(user: User, campsiteCount: Int, locale: Locale = Locale.US): String {
    val campsites = if (campsiteCount == 1) "1 campsite" else "$campsiteCount campsites"
    return listOfNotNull("Owner", campsites, joinedLabel(user.createdAt, locale)).joinToString(" · ")
}

/** OPEN reports filed within [REPORT_WINDOW_DAYS] days before [now]; undated ones are kept. */
private fun openRecent(reports: List<Report>, now: Timestamp): List<Report> {
    val cutoff = now.toDate().time - TimeUnit.DAYS.toMillis(REPORT_WINDOW_DAYS)
    return reports.filter { report ->
        report.status == ReportStatus.OPEN &&
            (report.createdAt?.toDate()?.time?.let { it >= cutoff } ?: true)
    }
}

/** [reports] sorted newest first; undated ones go last. */
private fun newestFirst(reports: List<Report>): List<Report> =
    reports.sortedWith(compareByDescending(nullsFirst()) { it.createdAt })
