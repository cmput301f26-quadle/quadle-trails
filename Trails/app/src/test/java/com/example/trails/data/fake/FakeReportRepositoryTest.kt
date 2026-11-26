// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Unit tests for the in-memory FakeReportRepository.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.data.fake

import com.example.trails.data.model.Report
import com.example.trails.data.model.ReportStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeReportRepositoryTest {

    private lateinit var repo: FakeReportRepository

    @Before
    fun setUp() {
        repo = FakeReportRepository()
    }

    // This checks that observeAll returns every sample report, newest first.
    @Test
    fun observeAll_everyReportNewestFirst(): Unit = runBlocking {
        val ids = repo.observeAll().first().map { it.id }
        assertEquals(
            listOf("report-banff-photos", "report-jasper-described", "report-admin-noise", "report-hinton-cancelled"),
            ids
        )
    }

    // This checks that observeForOwner only returns the reports against that owner.
    @Test
    fun observeForOwner_onlyThatOwnersReports(): Unit = runBlocking {
        val reports = repo.observeForOwner(TestUsers.OWNER_ID).first()
        assertEquals(3, reports.size)
        assertTrue(reports.all { it.ownerId == TestUsers.OWNER_ID })
    }

    // This checks that an owner nobody reported has no reports.
    @Test
    fun observeForOwner_unreportedOwner_empty(): Unit = runBlocking {
        assertTrue(repo.observeForOwner(TestUsers.ENTRANT_ID).first().isEmpty())
    }

    // This checks that removing the sample owner moves only their OPEN reports to OWNER_REMOVED.
    @Test
    fun setStatusForOwner_changesOnlyThatOwnersOpenReports(): Unit = runBlocking {
        repo.setStatusForOwner(TestUsers.OWNER_ID, ReportStatus.OPEN, ReportStatus.OWNER_REMOVED)

        val owners = repo.observeForOwner(TestUsers.OWNER_ID).first()
        assertTrue(owners.all { it.status == ReportStatus.OWNER_REMOVED })
        val others = repo.observeForOwner(TestUsers.ADMIN_ID).first()
        assertEquals(listOf(ReportStatus.DISMISSED), others.map { it.status })
    }

    // This checks that a report in a different status than "from" is left alone.
    @Test
    fun setStatusForOwner_otherStatusUntouched(): Unit = runBlocking {
        repo.save(
            Report(
                id = "report-banff-old", ownerId = TestUsers.OWNER_ID, campsiteId = TestCampsites.BANFF_ID,
                reason = "Not as described", ruleBroken = TestReports.RULE_ACCURATE_LISTINGS,
                status = ReportStatus.DISMISSED, createdAt = daysFromNow(-28)
            )
        )
        repo.setStatusForOwner(TestUsers.OWNER_ID, ReportStatus.OPEN, ReportStatus.OWNER_REMOVED)

        val old = repo.observeForOwner(TestUsers.OWNER_ID).first().single { it.id == "report-banff-old" }
        assertEquals(ReportStatus.DISMISSED, old.status)
    }

    // This checks that saving a report with an existing id replaces it instead of adding a copy.
    @Test
    fun save_sameId_replacesReport(): Unit = runBlocking {
        val original = TestReports.all.first { it.id == "report-banff-photos" }
        repo.save(original.copy(reporterCount = 3))

        val all = repo.observeAll().first()
        assertEquals(TestReports.all.size, all.size)
        assertEquals(3, all.single { it.id == "report-banff-photos" }.reporterCount)
    }

    // This checks that saving a report with a blank id adds it with a new id.
    @Test
    fun save_blankId_addsWithNewId(): Unit = runBlocking {
        repo.save(
            Report(
                ownerId = TestUsers.OWNER_ID, campsiteId = TestCampsites.JASPER_ID,
                reason = "Misleading photos", ruleBroken = TestReports.RULE_ACCURATE_LISTINGS,
                createdAt = daysFromNow(-1)
            )
        )
        val all = repo.observeAll().first()
        assertEquals(TestReports.all.size + 1, all.size)
        assertTrue(all.none { it.id.isBlank() })
    }

    // This checks that an invalid report (no reason) is refused.
    @Test(expected = IllegalArgumentException::class)
    fun save_invalidReport_throws(): Unit = runBlocking {
        repo.save(TestReports.all.first().copy(reason = ""))
    }
}
