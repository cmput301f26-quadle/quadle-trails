// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: In-memory ReportRepository seeded from TestReports.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.data.fake

import com.example.trails.data.model.Report
import com.example.trails.data.model.ReportStatus
import com.example.trails.data.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * In-memory [ReportRepository] for previews and tests. Changes are lost when the app closes.
 *
 * @param initial reports to start with; defaults to [TestReports.all].
 */
class FakeReportRepository(initial: List<Report> = TestReports.all) : ReportRepository {
    private val reports = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<Report>> =
        reports.map { list -> newestFirst(list) }

    override fun observeForOwner(ownerId: String): Flow<List<Report>> =
        reports.map { list -> newestFirst(list.filter { it.ownerId == ownerId }) }

    override suspend fun save(report: Report) {
        val errors = report.validate()
        require(errors.isEmpty()) { "Invalid report: $errors" }
        val stored = if (report.id.isBlank()) report.copy(id = UUID.randomUUID().toString()) else report
        reports.update { list -> list.filterNot { it.id == stored.id } + stored }
    }

    override suspend fun setStatusForOwner(ownerId: String, from: ReportStatus, to: ReportStatus) {
        reports.update { list ->
            list.map { if (it.ownerId == ownerId && it.status == from) it.copy(status = to) else it }
        }
    }

    private fun newestFirst(list: List<Report>) =
        list.sortedByDescending { it.createdAt?.toDate()?.time ?: Long.MIN_VALUE }
}
