// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Read/write access to policy reports against campsite owners, in the reports collection.
// FIRESTORE IMPLEMENTATION: Zainab (zsaghir)
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.data.repository

import com.example.trails.data.model.Report
import com.example.trails.data.model.ReportStatus
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to reports that campsite owners broke an app rule.
 */
interface ReportRepository {
    /**
     * Watches every report, newest first.
     *
     * @return a flow that emits all reports now and again whenever they change.
     */
    fun observeAll(): Flow<List<Report>>

    /**
     * Watches the reports against one campsite owner, newest first.
     *
     * @param ownerId device id of the reported owner.
     * @return a flow that emits the owner's reports now and again whenever they change.
     */
    fun observeForOwner(ownerId: String): Flow<List<Report>>

    /**
     * Saves a report, replacing any report with the same id. A blank id adds a new report.
     *
     * @param report the report to store.
     * @throws IllegalArgumentException if the report fails [Report.validate].
     */
    suspend fun save(report: Report)

    /**
     * Moves every report against one owner from one status to another, e.g. OPEN to
     * OWNER_REMOVED when the admin removes that owner. Reports in other statuses are left alone.
     *
     * @param ownerId device id of the reported owner.
     * @param from only reports in this status are changed.
     * @param to the status those reports are given.
     */
    suspend fun setStatusForOwner(ownerId: String, from: ReportStatus, to: ReportStatus)
}
