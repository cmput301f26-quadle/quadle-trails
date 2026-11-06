// Unit tests for buildAdminProfileState() and the admin Profile page text helpers (US 03.05.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class AdminProfileStateTest {

    private lateinit var maya: User
    private lateinit var campsites: List<Campsite>
    private lateinit var jan2027: Timestamp

    @Before
    fun setUp() {
        // Mid-month at noon, so the month is the same in any time zone.
        jan2027 = Timestamp(Calendar.getInstance().apply { set(2027, Calendar.JANUARY, 15, 12, 0, 0) }.time)
        maya = User(
            deviceId = "device-maya", name = "Maya Singh", email = "maya.singh@outlook.com",
            phone = "780-555-0101", roles = listOf(Role.ENTRANT, Role.OWNER), createdAt = jan2027
        )
        campsites = listOf(
            Campsite(id = "jasper-pines", ownerId = "device-maya"),
            Campsite(id = "bow-river", ownerId = "device-maya"),
            Campsite(id = "grande-cache", ownerId = "device-zainab")
        )
    }

    // This checks that when no user has the id, the page shows the not-found state.
    @Test
    fun missingUser_givesNotFound() {
        assertEquals(AdminProfileUiState.NotFound, buildAdminProfileState(null, campsites))
    }

    // This checks that a loaded owner's campsite count only includes campsites they own.
    @Test
    fun owner_countsTheirCampsites() {
        assertEquals(AdminProfileUiState.Loaded(maya, 2), buildAdminProfileState(maya, campsites))
    }

    // This checks that role chips are listed Admin, Owner, Entrant, whatever order the roles are stored in.
    @Test
    fun roleLabels_inAdminOwnerEntrantOrder() {
        val everything = User(roles = listOf(Role.ENTRANT, Role.OWNER, Role.ADMIN))
        assertEquals(listOf("Admin", "Owner", "Entrant"), roleLabels(everything))
        assertEquals(listOf("Owner", "Entrant"), roleLabels(maya))
        assertEquals(listOf("Admin"), roleLabels(User(roles = listOf(Role.ADMIN))))
    }

    // This checks that the joined date is written as a short month and the year.
    @Test
    fun joinedLabel_formatsMonthYear() {
        assertEquals("Joined Jan 2027", joinedLabel(jan2027, Locale.US))
    }

    // This checks that an unknown join date gives no label.
    @Test
    fun joinedLabel_null_givesNull() {
        assertNull(joinedLabel(null))
    }

    // This checks that the grey line shows the email and the joined date with a dot between.
    @Test
    fun emailAndJoined_bothParts() {
        assertEquals("maya.singh@outlook.com · Joined Jan 2027", emailAndJoinedLine(maya))
    }

    // This checks that with no join date only the email is shown, with no stray dot.
    @Test
    fun emailAndJoined_missingDate_emailOnly() {
        assertEquals("maya.singh@outlook.com", emailAndJoinedLine(maya.copy(createdAt = null)))
    }

    // This checks that with no email only the joined date is shown, and nothing when both are missing.
    @Test
    fun emailAndJoined_missingEmail() {
        assertEquals("Joined Jan 2027", emailAndJoinedLine(maya.copy(email = "")))
        assertEquals("", emailAndJoinedLine(maya.copy(email = "", createdAt = null)))
    }

    // This checks that a blank phone shows "Not provided" and a real one is shown as-is.
    @Test
    fun phoneLabel_blank_notProvided() {
        assertEquals("Not provided", phoneLabel(""))
        assertEquals("Not provided", phoneLabel("   "))
        assertEquals("780-555-0101", phoneLabel("780-555-0101"))
    }

    // This checks that a two-word name gives the first letters of both words, in capitals.
    @Test
    fun initials_twoWords() {
        assertEquals("MS", profileInitials("Maya Singh"))
        assertEquals("ZS", profileInitials("  zainab  ali saghir "))
    }

    // This checks that a one-word name gives a single capital letter.
    @Test
    fun initials_oneWord() {
        assertEquals("C", profileInitials("cher"))
    }

    // This checks that a blank name gives "?" instead of crashing.
    @Test
    fun initials_blank_questionMark() {
        assertEquals("?", profileInitials(""))
        assertEquals("?", profileInitials("   "))
    }
}
