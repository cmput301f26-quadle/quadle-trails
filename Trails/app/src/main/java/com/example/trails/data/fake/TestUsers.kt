// Sample users, one per role.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.fake

import com.example.trails.data.model.Role
import com.example.trails.data.model.User

object TestUsers {
    const val ENTRANT_ID = "device-entrant-001"
    const val OWNER_ID = "device-owner-001"
    const val ADMIN_ID = "device-admin-001"

    val all = listOf(
        User(
            deviceId = ENTRANT_ID,
            name = "Priya Sharma",
            email = "priya.sharma@example.com",
            phone = "780-555-0142",
            createdAt = daysFromNow(-30)
        ),
        User(
            deviceId = OWNER_ID,
            name = "Daniel Whitehorse",
            email = "daniel.whitehorse@example.com",
            phone = "403-555-0187",
            roles = listOf(Role.ENTRANT, Role.OWNER),
            activeMode = Role.OWNER,
            createdAt = daysFromNow(-90)
        ),
        User(
            deviceId = ADMIN_ID,
            name = "Morgan Lee",
            email = "morgan.lee@example.com",
            roles = listOf(Role.ENTRANT, Role.ADMIN),
            activeMode = Role.ADMIN,
            createdAt = daysFromNow(-120)
        )
    )
}
