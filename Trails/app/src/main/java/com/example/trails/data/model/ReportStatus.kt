// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Where a policy report against a campsite owner stands in the admin's review.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.data.model

/** Where a [Report] stands in the admin's review. */
enum class ReportStatus {
    /** Filed and waiting for an admin to review it. */
    OPEN,
    /** Reviewed by an admin, who found no violation. */
    DISMISSED,
    /** Reviewed by an admin, who removed the reported owner from the app. */
    OWNER_REMOVED
}
