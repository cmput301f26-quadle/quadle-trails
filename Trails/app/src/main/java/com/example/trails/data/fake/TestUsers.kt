// Sample users for each role; the admin also holds Owner and Camper (US 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
package com.example.trails.data.fake

import com.example.trails.data.model.Role
import com.example.trails.data.model.User

/**
 * Sample users for local testing, one per role.
 *
 * Avatars are DiceBear "Notionists" illustrations (CC0 1.0), not photos of real people.
 */
object TestUsers {
    /** Device id of the sample camper. */
    const val ENTRANT_ID = "device-entrant-001"

    /** Device id of the sample owner. */
    const val OWNER_ID = "device-owner-001"

    /** Device id of the sample admin, who also holds Owner and Camper. */
    const val ADMIN_ID = "device-admin-001"

    private fun avatar(seed: String) = "https://api.dicebear.com/9.x/notionists/png?seed=$seed"

    /** All sample users. */
    val all = listOf(
        User(
            deviceId = ENTRANT_ID,
            name = "Priya Sharma",
            email = "priya.sharma@example.com",
            phone = "780-555-0142",
            profileImageUrl = avatar("Priya"),
            createdAt = daysFromNow(-30)
        ),
        User(
            deviceId = OWNER_ID,
            name = "Daniel Whitehorse",
            email = "daniel.whitehorse@example.com",
            phone = "403-555-0187",
            profileImageUrl = avatar("Daniel"),
            roles = listOf(Role.ENTRANT, Role.OWNER),
            activeMode = Role.OWNER,
            createdAt = daysFromNow(-90)
        ),
        User(
            deviceId = ADMIN_ID,
            name = "Morgan Lee",
            email = "morgan.lee@example.com",
            profileImageUrl = avatar("Morgan"),
            roles = listOf(Role.ENTRANT, Role.OWNER, Role.ADMIN),
            activeMode = Role.ADMIN,
            createdAt = daysFromNow(-120)
        )
    )
}
