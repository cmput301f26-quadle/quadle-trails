// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.05.01, 03.02.01
// PURPOSE: Unit tests for buildAdminProfileState() and the admin Profile page text helpers.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.data.model.WaitlistEntry
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // This checks that the signed-in admin's own profile is marked as theirs, so it can't be removed.
    @Test
    fun self_isSelfTrue() {
        val state = buildAdminProfileState(maya, campsites, currentUserId = "device-maya")
        assertTrue((state as AdminProfileUiState.Loaded).isSelf)
    }

    // This checks that someone else's profile is not marked as the admin's own.
    @Test
    fun otherUser_isSelfFalse() {
        val state = buildAdminProfileState(maya, campsites, currentUserId = "device-zainab")
        assertFalse((state as AdminProfileUiState.Loaded).isSelf)
    }

    // This checks that the lottery entry count only includes this user's waitlist entries.
    @Test
    fun entryCount_matchesWaitlist() {
        val entries = listOf(
            WaitlistEntry(userId = "device-maya", lotteryId = "summer"),
            WaitlistEntry(userId = "device-maya", lotteryId = "fall"),
            WaitlistEntry(userId = "device-priya", lotteryId = "summer")
        )
        val state = buildAdminProfileState(maya, campsites, entries)
        assertEquals(2, (state as AdminProfileUiState.Loaded).lotteryEntryCount)
    }

    // This checks the dialog message for an owner with campsites and several lottery entries.
    @Test
    fun removeMessage_ownerWithCampsites() {
        assertEquals(
            "Maya Singh loses access to Quadle. Their 2 campsites and 3 lottery entries are removed. " +
                "This can't be undone.",
            removeProfileMessage("Maya Singh", 2, 3)
        )
    }

    // This checks that an entrant with no campsites gets a message without the campsites part.
    @Test
    fun removeMessage_entrantNoCampsites() {
        assertEquals(
            "Priya Sharma loses access to Quadle. Their 4 lottery entries are removed. This can't be undone.",
            removeProfileMessage("Priya Sharma", 0, 4)
        )
    }

    // This checks that counts of one use the singular "1 campsite" and "1 lottery entry".
    @Test
    fun removeMessage_singular() {
        assertEquals(
            "Maya Singh loses access to Quadle. Their 1 campsite and 1 lottery entry are removed. " +
                "This can't be undone.",
            removeProfileMessage("Maya Singh", 1, 1)
        )
        assertEquals(
            "Priya Sharma loses access to Quadle. Their 1 lottery entry is removed. This can't be undone.",
            removeProfileMessage("Priya Sharma", 0, 1)
        )
    }
}
