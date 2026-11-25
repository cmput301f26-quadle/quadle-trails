// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Unit tests for buildReportedOwners(), buildPolicyReviewState() and the policy
//   review text helpers.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Report
import com.example.trails.data.model.ReportStatus
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class PolicyReviewStateTest {

    /** Noon on Apr 10 2027, so day boundaries don't depend on the time zone. */
    private val now = Timestamp(Calendar.getInstance().apply {
        set(2027, Calendar.APRIL, 10, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.time)

    private fun daysAgo(days: Long, extraHours: Long = 0) = Timestamp(
        Date(now.toDate().time - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(extraHours))
    )

    private val accurate = "Accurate listings"
    private val honour = "Honour bookings"

    private fun report(
        id: String,
        ownerId: String,
        daysAgo: Long,
        rule: String = accurate,
        status: ReportStatus = ReportStatus.OPEN,
        campers: Int = 1
    ) = Report(
        id = id,
        ownerId = ownerId,
        campsiteId = "site-$ownerId",
        reason = "Misleading photos",
        ruleBroken = rule,
        reporterCount = campers,
        status = status,
        createdAt = daysAgo(daysAgo)
    )

    private val maya = User(deviceId = "device-maya", name = "Maya Singh", roles = listOf(Role.OWNER))
    private val omar = User(deviceId = "device-omar", name = "Omar Haddad", roles = listOf(Role.OWNER))
    private val jordan = User(deviceId = "device-jordan", name = "Jordan Lee", roles = listOf(Role.ADMIN, Role.OWNER))
    private val users = listOf(maya, omar, jordan)

    private val campsites = listOf(
        Campsite(id = "a", ownerId = "device-maya"),
        Campsite(id = "b", ownerId = "device-maya"),
        Campsite(id = "c", ownerId = "device-omar")
    )

    private val mayaNew = report("maya-new", "device-maya", 2, honour)
    private val mayaMid = report("maya-mid", "device-maya", 5)
    private val mayaOld = report("maya-old", "device-maya", 9)
    private val mayaTooOld = report("maya-too-old", "device-maya", 31)
    private val mayaDismissed = report("maya-dismissed", "device-maya", 1, status = ReportStatus.DISMISSED)
    private val omarOne = report("omar-one", "device-omar", 20)
    private val goneOwner = report("gone", "device-gone", 25)
    private val omarResolved = report("omar-resolved", "device-omar", 3, status = ReportStatus.OWNER_REMOVED)

    private val allReports = listOf(mayaMid, omarOne, mayaNew, goneOwner, mayaDismissed, mayaOld, mayaTooOld, omarResolved)

    // This checks that open reports are grouped one entry per owner, with each owner's campsite count.
    @Test
    fun reportedOwners_groupsByOwner() {
        val owners = buildReportedOwners(allReports, users, campsites, now)
        assertEquals(setOf(maya, omar), owners.map { it.user }.toSet())
        val mayaRow = owners.single { it.user == maya }
        assertEquals(listOf(mayaNew, mayaMid, mayaOld), mayaRow.openReports)
        assertEquals(2, mayaRow.campsiteCount)
        assertEquals(1, owners.single { it.user == omar }.campsiteCount)
    }

    // This checks that the owner whose oldest open report has waited longest comes first.
    @Test
    fun reportedOwners_oldestReportFirst() {
        val owners = buildReportedOwners(allReports, users, campsites, now)
        assertEquals(listOf(omar, maya), owners.map { it.user })
        assertEquals(daysAgo(20), owners[0].oldestReportAt)
        assertEquals(daysAgo(9), owners[1].oldestReportAt)
    }

    // This checks that reports against an owner who no longer exists are skipped.
    @Test
    fun reportedOwners_missingOwnerSkipped() {
        val owners = buildReportedOwners(listOf(goneOwner), users, campsites, now)
        assertTrue(owners.isEmpty())
    }

    // This checks that owners with only closed or older-than-30-day reports aren't listed.
    @Test
    fun reportedOwners_onlyOpenRecentReports() {
        val owners = buildReportedOwners(listOf(mayaDismissed, mayaTooOld, omarResolved), users, campsites, now)
        assertTrue(owners.isEmpty())
    }

    // This checks that a missing user gives NotFound.
    @Test
    fun reviewState_missingUser_isNotFound() {
        val state = buildPolicyReviewState(null, allReports, campsites, null, "device-jordan", now)
        assertEquals(PolicyReviewUiState.NotFound, state)
    }

    // This checks that the page keeps only the owner's open reports from the last 30 days, newest first.
    @Test
    fun reviewState_openReportsInWindowNewestFirst() {
        val state = buildPolicyReviewState(maya, allReports, campsites, null, "device-jordan", now)
            as PolicyReviewUiState.Loaded
        assertEquals(listOf(mayaNew, mayaMid, mayaOld), state.reports)
        assertEquals(2, state.campsiteCount)
        assertFalse(state.isSelf)
        assertEquals(PolicyActionState.Idle, state.action)
    }

    // This checks that a report exactly 30 days old is still inside the window.
    @Test
    fun reviewState_thirtyDaysIsInsideWindow() {
        val edge = report("edge", "device-maya", 30)
        val state = buildPolicyReviewState(maya, listOf(edge), campsites, null, "", now)
            as PolicyReviewUiState.Loaded
        assertEquals(listOf(edge), state.reports)
    }

    // This checks that the rule broken most often is picked, and null when there are no reports.
    @Test
    fun reviewState_mostCommonRule() {
        val state = buildPolicyReviewState(maya, allReports, campsites, null, "", now)
            as PolicyReviewUiState.Loaded
        assertEquals(accurate, state.ruleBroken)
        val none = buildPolicyReviewState(maya, emptyList(), campsites, null, "", now)
            as PolicyReviewUiState.Loaded
        assertNull(none.ruleBroken)
    }

    // This checks that a tie between rules goes to the rule on the newest report.
    @Test
    fun mostCommonRule_tieGoesToNewest() {
        assertEquals(honour, mostCommonRule(listOf(mayaNew, mayaMid)))
    }

    // This checks that the impact passed in is used, and a fallback is built when it isn't known yet.
    @Test
    fun reviewState_impact() {
        val given = ProfileImpact("Maya Singh", 2, 4)
        val loaded = buildPolicyReviewState(maya, allReports, campsites, given, "", now) as PolicyReviewUiState.Loaded
        assertEquals(given, loaded.impact)
        val fallback = buildPolicyReviewState(maya, allReports, campsites, null, "", now) as PolicyReviewUiState.Loaded
        assertEquals(ProfileImpact("Maya Singh", 2, 0), fallback.impact)
    }

    // This checks that isSelf is set when the admin is reviewing their own reports, and the action is passed through.
    @Test
    fun reviewState_isSelfAndAction() {
        val state = buildPolicyReviewState(
            jordan, emptyList(), campsites, null, "device-jordan", now, PolicyActionState.Confirming
        ) as PolicyReviewUiState.Loaded
        assertTrue(state.isSelf)
        assertEquals(PolicyActionState.Confirming, state.action)
    }

    // This checks the report subtitle with singular and plural campers, and without a date.
    @Test
    fun reportSubtitle_formats() {
        val apr3 = Report(reporterCount = 2, createdAt = daysAgo(7))
        assertEquals("Pine Ridge · Site 04 · 2 campers · Apr 3", reportSubtitle(apr3, "Pine Ridge · Site 04"))
        assertEquals("Pine Ridge · 1 camper · Apr 3", reportSubtitle(apr3.copy(reporterCount = 1), "Pine Ridge"))
        assertEquals("Pine Ridge · 1 camper", reportSubtitle(Report(reporterCount = 1), "Pine Ridge"))
    }

    // This checks the report count label for one and many.
    @Test
    fun reportCountLabel_plurals() {
        assertEquals("1 report", reportCountLabel(1))
        assertEquals("3 reports", reportCountLabel(3))
        assertEquals("0 reports", reportCountLabel(0))
    }

    // This checks the remove owner impact line with plurals, and that a part with a 0 count is left out.
    @Test
    fun removeOwnerImpactLine_plurals() {
        assertEquals(
            "Removing also takes down their 3 campsites and cancels 2 lottery entries.",
            removeOwnerImpactLine(3, 2)
        )
        assertEquals(
            "Removing also takes down their 1 campsite and cancels 1 lottery entry.",
            removeOwnerImpactLine(1, 1)
        )
        assertEquals("Removing also takes down their 2 campsites.", removeOwnerImpactLine(2, 0))
        assertEquals("Removing also cancels 4 lottery entries.", removeOwnerImpactLine(0, 4))
        assertEquals("", removeOwnerImpactLine(0, 0))
    }

    // This checks the needs review line for today, 1 day and several days, and with no date.
    @Test
    fun needsReviewLine_ages() {
        assertEquals("3 policy reports · Oldest report: today", needsReviewLine(3, daysAgo(0, 2), now))
        assertEquals("1 policy report · Oldest report: 1 day ago", needsReviewLine(1, daysAgo(1, 2), now))
        assertEquals("3 policy reports · Oldest report: 2 days ago", needsReviewLine(3, daysAgo(2), now))
        assertEquals("3 policy reports", needsReviewLine(3, null, now))
    }
}
