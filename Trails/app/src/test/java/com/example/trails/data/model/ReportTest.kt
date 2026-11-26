// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Unit tests for Report.validate().
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.data.model

import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestReports
import com.example.trails.data.fake.TestUsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTest {

    private val report = Report(
        id = "report-banff-photos",
        ownerId = TestUsers.OWNER_ID,
        campsiteId = TestCampsites.BANFF_ID,
        reason = "Misleading photos",
        ruleBroken = TestReports.RULE_ACCURATE_LISTINGS,
        reporterCount = 2
    )

    // This checks that a report with every required field filled in has no errors.
    @Test
    fun validReport_noErrors() {
        assertTrue(report.validate().isEmpty())
    }

    // This checks that every sample report is itself valid.
    @Test
    fun sampleReports_noErrors() {
        TestReports.all.forEach { assertTrue(it.id, it.validate().isEmpty()) }
    }

    // This checks that a new report starts OPEN with one camper reporting it.
    @Test
    fun newReport_defaultsToOpenWithOneReporter() {
        val fresh = Report()
        assertEquals(ReportStatus.OPEN, fresh.status)
        assertEquals(1, fresh.reporterCount)
    }

    // This checks that a blank owner id is reported as an ownerId error.
    @Test
    fun blankOwnerId_ownerIdError() {
        assertEquals(setOf("ownerId"), report.copy(ownerId = "").validate().keys)
    }

    // This checks that a blank campsite id is reported as a campsiteId error.
    @Test
    fun blankCampsiteId_campsiteIdError() {
        assertEquals(setOf("campsiteId"), report.copy(campsiteId = "  ").validate().keys)
    }

    // This checks that a blank reason is reported as a reason error.
    @Test
    fun blankReason_reasonError() {
        assertEquals(setOf("reason"), report.copy(reason = "").validate().keys)
    }

    // This checks that a blank rule is reported as a ruleBroken error.
    @Test
    fun blankRuleBroken_ruleBrokenError() {
        assertEquals(setOf("ruleBroken"), report.copy(ruleBroken = " ").validate().keys)
    }

    // This checks that a report no camper made (count 0) is reported as a reporterCount error.
    @Test
    fun zeroReporterCount_reporterCountError() {
        assertEquals(setOf("reporterCount"), report.copy(reporterCount = 0).validate().keys)
    }

    // This checks that an empty report lists every missing field at once.
    @Test
    fun emptyReport_allFieldErrors() {
        assertEquals(
            setOf("ownerId", "campsiteId", "reason", "ruleBroken"),
            Report().validate().keys
        )
    }
}
