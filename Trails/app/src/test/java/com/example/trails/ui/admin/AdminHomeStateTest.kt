// Unit tests for buildAdminHomeState() and greeting() on the admin home (US 03.04.01, 03.05.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AdminHomeStateTest {

    private lateinit var admin: User
    private lateinit var users: List<User>
    private lateinit var campsites: List<Campsite>

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
}
