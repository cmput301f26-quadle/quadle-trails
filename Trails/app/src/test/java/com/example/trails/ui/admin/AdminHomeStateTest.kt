// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.04.01, 03.05.01, 03.07.01
// PURPOSE: Unit tests for buildAdminHomeState() and greeting() on the admin home.
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.concurrent.TimeUnit

class AdminHomeStateTest {

    private lateinit var admin: User
    private lateinit var users: List<User>
    private lateinit var campsites: List<Campsite>
    private val now = Timestamp(Date(1_800_000_000_000L))

    /** A report filed [days] days before [now], against Daniel unless another owner is given. */
    private fun report(days: Long, status: ReportStatus = ReportStatus.OPEN, ownerId: String = "device-daniel") =
        Report(
            ownerId = ownerId, campsiteId = "campsite-banff-tunnel", reason = "Misleading photos",
            ruleBroken = "Accurate listings", status = status,
            createdAt = Timestamp(Date(now.toDate().time - TimeUnit.DAYS.toMillis(days)))
        )

    @Before
    fun setUp() {
        admin = User(
            deviceId = "device-admin-maya", name = "Maya Singh",
            roles = listOf(Role.ENTRANT, Role.ADMIN), activeMode = Role.ADMIN
        )
        users = listOf(
            admin,
            User(deviceId = "device-daniel", name = "Daniel Whitehorse"),
            User(deviceId = "device-ava", name = "Ava Patel")
        )
        campsites = listOf(
            Campsite(
                id = "campsite-banff-tunnel", ownerId = "device-daniel", name = "Tunnel Mountain Site 14",
                locationName = "Banff, AB", maxOccupancy = 6
            ),
            Campsite(
                id = "campsite-jasper-whistlers", ownerId = "device-daniel", name = "Whistlers Loop Site 22",
                locationName = "Jasper, AB", maxOccupancy = 4
            )
        )
    }

    // This checks that hours before noon, including the first and last, greet with "Good morning" and the first name.
    @Test
    fun morningHour_goodMorning() {
        assertEquals("Good morning", greeting(0))
        assertEquals("Good morning", greeting(11))
        assertEquals("Good morning, Maya", buildAdminHomeState(admin, campsites, users, 9).greeting)
    }

    // This checks that 12:00 up to just before 17:00 greets with "Good afternoon".
    @Test
    fun afternoonHour_goodAfternoon() {
        assertEquals("Good afternoon", greeting(12))
        assertEquals("Good afternoon", greeting(16))
        assertEquals("Good afternoon, Maya", buildAdminHomeState(admin, campsites, users, 14).greeting)
    }

    // This checks that 17:00 and later greets with "Good evening".
    @Test
    fun eveningHour_goodEvening() {
        assertEquals("Good evening", greeting(17))
        assertEquals("Good evening", greeting(23))
        assertEquals("Good evening, Maya", buildAdminHomeState(admin, campsites, users, 20).greeting)
    }

    // This checks that the tiles count every campsite and every user profile in the lists.
    @Test
    fun counts_matchLists() {
        val state = buildAdminHomeState(admin, campsites, users, 9)
        assertEquals(2, state.campsiteCount)
        assertEquals(3, state.profileCount)
    }

    // This checks that a missing profile or a blank name gives the greeting with no trailing comma.
    @Test
    fun missingUser_greetingWithoutName() {
        assertEquals("Good morning", buildAdminHomeState(null, campsites, users, 9).greeting)
        assertEquals(
            "Good evening",
            buildAdminHomeState(admin.copy(name = "  "), campsites, users, 19).greeting
        )
    }

    // This checks that the Needs review card counts only open reports from the last 30 days and
    // says how long the oldest of them has waited.
    @Test
    fun openReports_countedWithOldest() {
        val reports = listOf(
            report(2),
            report(5, ownerId = "device-ava"),
            report(1),
            report(3, status = ReportStatus.DISMISSED),
            report(10, status = ReportStatus.OWNER_REMOVED),
            report(45)
        )
        val state = buildAdminHomeState(admin, campsites, users, 9, reports, now)
        assertEquals(3, state.openReportCount)
        assertEquals("3 policy reports", state.reportCountLine)
        assertEquals("Oldest report: 5 days ago", state.oldestReportLine)
    }

    // This checks that one open report uses the singular "1 policy report".
    @Test
    fun oneOpenReport_singular() {
        val state = buildAdminHomeState(admin, campsites, users, 9, listOf(report(0)), now)
        assertEquals(1, state.openReportCount)
        assertEquals("1 policy report", state.reportCountLine)
        assertEquals("Oldest report: today", state.oldestReportLine)
    }

    // This checks that with no open reports the count is 0, so the Needs review card is hidden.
    @Test
    fun noOpenReports_countZero() {
        val closed = listOf(report(2, status = ReportStatus.DISMISSED))
        assertEquals(0, buildAdminHomeState(admin, campsites, users, 9, closed, now).openReportCount)
        val none = buildAdminHomeState(admin, campsites, users, 9, emptyList(), now)
        assertEquals(0, none.openReportCount)
        assertNull(none.oldestReportLine)
    }

    // This checks that reports against an owner who no longer has a profile aren't counted,
    // matching the Policy reports list.
    @Test
    fun reportAgainstRemovedOwner_notCounted() {
        val reports = listOf(report(2, ownerId = "device-gone"))
        assertEquals(0, buildAdminHomeState(admin, campsites, users, 9, reports, now).openReportCount)
    }
}
