// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Sample policy reports, matching the admin policy review in Figma.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.data.fake

import com.example.trails.data.model.Report
import com.example.trails.data.model.ReportStatus

/** Sample reports used to seed [FakeReportRepository]. */
object TestReports {
    /** The app rule about listings matching the real campsite. */
    const val RULE_ACCURATE_LISTINGS = "Accurate listings · photos and details must match the campsite"

    /** The app rule about owners keeping confirmed bookings. */
    const val RULE_HONOUR_BOOKINGS = "Honour bookings · owners can't cancel a confirmed reservation"

    /**
     * Every sample report: three open reports against the sample owner (one per campsite)
     * and one dismissed report against the admin, who also holds the Owner role.
     */
    val all = listOf(
        Report(
            id = "report-banff-photos",
            ownerId = TestUsers.OWNER_ID,
            campsiteId = TestCampsites.BANFF_ID,
            reason = "Misleading photos",
            ruleBroken = RULE_ACCURATE_LISTINGS,
            reporterCount = 2,
            status = ReportStatus.OPEN,
            createdAt = daysFromNow(-3)
        ),
        Report(
            id = "report-jasper-described",
            ownerId = TestUsers.OWNER_ID,
            campsiteId = TestCampsites.JASPER_ID,
            reason = "Not as described",
            ruleBroken = RULE_ACCURATE_LISTINGS,
            reporterCount = 1,
            status = ReportStatus.OPEN,
            createdAt = daysFromNow(-12)
        ),
        Report(
            id = "report-hinton-cancelled",
            ownerId = TestUsers.OWNER_ID,
            campsiteId = TestCampsites.HINTON_ID,
            reason = "Confirmed booking cancelled",
            ruleBroken = RULE_HONOUR_BOOKINGS,
            reporterCount = 1,
            status = ReportStatus.OPEN,
            createdAt = daysFromNow(-25)
        ),
        Report(
            id = "report-admin-noise",
            ownerId = TestUsers.ADMIN_ID,
            campsiteId = "campsite-canmore-quarry", // not in TestCampsites; the admin has no sample site
            reason = "Not as described",
            ruleBroken = RULE_ACCURATE_LISTINGS,
            reporterCount = 1,
            status = ReportStatus.DISMISSED,
            createdAt = daysFromNow(-20)
        )
    )
}
