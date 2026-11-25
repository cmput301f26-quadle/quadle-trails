// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: A report that a campsite owner broke an app rule, stored in the reports collection.
//          Follows the Report CRC card on the wiki.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * A report that a campsite owner broke an app rule, reviewed by an admin (US 03.07.01).
 *
 * @property id Firestore document id.
 * @property ownerId Device id of the reported campsite owner.
 * @property campsiteId Id of the campsite the report is about.
 * @property reason What campers reported, e.g. "Misleading photos".
 * @property ruleBroken The app rule the owner broke, e.g.
 *   "Accurate listings · photos and details must match the campsite".
 * @property reporterCount How many campers made this same report; at least 1.
 * @property status Where the report stands in the admin's review.
 * @property createdAt When the report was first filed.
 */
data class Report(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val campsiteId: String = "",
    val reason: String = "",
    val ruleBroken: String = "",
    val reporterCount: Int = 1,
    val status: ReportStatus = ReportStatus.OPEN,
    val createdAt: Timestamp? = null
) {
    /**
     * Checks the fields a report needs before it's saved.
     * Kept as a function rather than init checks because Firestore builds objects from defaults.
     *
     * @return error messages keyed by field name; empty if the report can be saved.
     */
    fun validate(): Map<String, String> = buildMap {
        if (ownerId.isBlank()) put("ownerId", "Reported owner is required")
        if (campsiteId.isBlank()) put("campsiteId", "Reported campsite is required")
        if (reason.isBlank()) put("reason", "Reason is required")
        if (ruleBroken.isBlank()) put("ruleBroken", "Rule broken is required")
        if (reporterCount < 1) put("reporterCount", "At least 1 camper must have reported it")
    }
}
