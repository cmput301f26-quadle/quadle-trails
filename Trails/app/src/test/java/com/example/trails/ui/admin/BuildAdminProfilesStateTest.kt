// Unit tests for buildAdminProfilesState() and the admin Profiles text helpers (US 03.05.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildAdminProfilesStateTest {

    private lateinit var alex: User
    private lateinit var maya: User
    private lateinit var owen: User
    private lateinit var zainab: User
    private lateinit var priya: User
    private lateinit var jasperPines: Campsite
    private lateinit var bowRiver: Campsite
    private lateinit var grandeCache: Campsite

    @Before
    fun setUp() {
        alex = User(
            deviceId = "device-alex", name = "Alex Morgan", email = "alex.morgan@gmail.com",
            roles = listOf(Role.ENTRANT)
        )
        maya = User(
            deviceId = "device-maya", name = "Maya Singh", email = "maya.singh@outlook.com",
            roles = listOf(Role.ENTRANT, Role.OWNER)
        )
        owen = User(
            deviceId = "device-owen", name = "Owen Diaz", email = "odiaz@ualberta.ca",
            roles = listOf(Role.OWNER)
        )
        zainab = User(
            deviceId = "device-zainab", name = "Zainab Saghir", email = "zsaghir@ualberta.ca",
            roles = listOf(Role.OWNER, Role.ADMIN)
        )
        priya = User(
            deviceId = "device-priya", name = "Priya Shah", email = "priya.shah@gmail.com",
            roles = listOf(Role.ENTRANT)
        )
        jasperPines = Campsite(id = "jasper-pines", ownerId = "device-maya", name = "Jasper Pines")
        bowRiver = Campsite(id = "bow-river", ownerId = "device-maya", name = "Bow River Flats")
        grandeCache = Campsite(id = "grande-cache", ownerId = "device-zainab", name = "Grande Cache Ridge")
    }

    private fun state(query: String = "", filter: ProfileFilter = ProfileFilter.ALL) =
        buildAdminProfilesState(
            listOf(priya, zainab, alex, owen, maya),
            listOf(jasperPines, bowRiver, grandeCache),
            query,
            filter
        )

    private fun ids(s: AdminProfilesUiState) = s.items.map { it.user.deviceId }

    // This checks that with no search and the All chip, every user is shown in A–Z order by name.
    @Test
    fun emptyQuery_allFilter_showsEveryoneSortedByName() {
        assertEquals(
            listOf("device-alex", "device-maya", "device-owen", "device-priya", "device-zainab"),
            ids(state())
        )
    }

    // This checks that the Owners chip keeps only users who hold the OWNER role.
    @Test
    fun ownersFilter_keepsOnlyOwners() {
        assertEquals(
            listOf("device-maya", "device-owen", "device-zainab"),
            ids(state(filter = ProfileFilter.OWNERS))
        )
    }

    // This checks that the Entrants chip leaves out owners and admins, even ones who also hold ENTRANT.
    @Test
    fun entrantsFilter_excludesOwnersAndAdmins() {
        val entrantAdmin = User(
            deviceId = "device-ella", name = "Ella Admin", email = "ella@gmail.com",
            roles = listOf(Role.ENTRANT, Role.ADMIN)
        )
        val s = buildAdminProfilesState(
            listOf(maya, owen, zainab, entrantAdmin), emptyList(), "", ProfileFilter.ENTRANTS
        )
        assertEquals(emptyList<String>(), ids(s))
    }

    // This checks that the Entrants chip keeps users who are only entrants.
    @Test
    fun entrantsFilter_keepsPlainEntrants() {
        assertEquals(
            listOf("device-alex", "device-priya"),
            ids(state(filter = ProfileFilter.ENTRANTS))
        )
    }

    // This checks that the Admins chip keeps only users who hold the ADMIN role.
    @Test
    fun adminsFilter_keepsOnlyAdmins() {
        assertEquals(listOf("device-zainab"), ids(state(filter = ProfileFilter.ADMINS)))
    }

    // This checks that a user with two roles shows up under both of those chips.
    @Test
    fun multiRoleUser_appearsUnderEachMatchingFilter() {
        assertTrue("device-zainab" in ids(state(filter = ProfileFilter.OWNERS)))
        assertTrue("device-zainab" in ids(state(filter = ProfileFilter.ADMINS)))
    }

    // This checks that searching by name works no matter the upper/lower case.
    @Test
    fun query_matchesNameIgnoringCase() {
        assertEquals(listOf("device-maya"), ids(state("mAYA sINGH")))
    }

    // This checks that searching for part of an email finds that user.
    @Test
    fun query_matchesEmail() {
        assertEquals(listOf("device-owen", "device-zainab"), ids(state("ualberta.ca")))
    }

    // This checks that spaces before and after the search text are ignored.
    @Test
    fun query_withSpaces_isTrimmed() {
        assertEquals(listOf("device-priya"), ids(state("   priya   ")))
    }

    // This checks that the search and the chip both narrow the list at the same time.
    @Test
    fun queryAndFilter_bothApply() {
        assertEquals(listOf("device-owen", "device-zainab"), ids(state("ualberta")))
        assertEquals(listOf("device-zainab"), ids(state("ualberta", ProfileFilter.ADMINS)))
    }

    // This checks that a search with no matches empties the list but still counts every user.
    @Test
    fun query_noMatch_emptyItemsButTotalUnchanged() {
        val s = state("nobody")
        assertEquals(0, s.items.size)
        assertEquals(5, s.totalCount)
    }

    // This checks that each user's campsite count is the number of campsites they own.
    @Test
    fun campsiteCount_countsOwnersCampsites() {
        val counts = state().items.associate { it.user.deviceId to it.campsiteCount }
        assertEquals(mapOf(
            "device-alex" to 0, "device-maya" to 2, "device-owen" to 0,
            "device-priya" to 0, "device-zainab" to 1
        ), counts)
    }

    // This checks that the row label picks Admin over Owner, and Owner over Entrant.
    @Test
    fun primaryRole_adminBeatsOwnerBeatsEntrant() {
        assertEquals(Role.ADMIN, primaryRole(zainab))
        assertEquals(Role.OWNER, primaryRole(maya))
    }

    // This checks that an owner's subtitle shows how many campsites they have.
    @Test
    fun subtitle_ownerShowsCampsiteCount() {
        assertEquals("Owner · 2 campsites", adminProfileSubtitle(AdminProfileItem(maya, 2)))
        assertEquals("Owner · 0 campsites", adminProfileSubtitle(AdminProfileItem(owen, 0)))
    }

    // This checks that an owner with one campsite says "1 campsite", not "1 campsites".
    @Test
    fun subtitle_ownerWithOneCampsite_singular() {
        assertEquals("Owner · 1 campsite", adminProfileSubtitle(AdminProfileItem(owen, 1)))
    }

    // This checks that entrants and admins get a plain role word with no campsite count.
    @Test
    fun subtitle_entrantAndAdmin() {
        assertEquals("Entrant", adminProfileSubtitle(AdminProfileItem(alex, 0)))
        assertEquals("Admin", adminProfileSubtitle(AdminProfileItem(zainab, 1)))
    }

    // This checks that a user with a blank name shows as "Unnamed user" and sorts after named users.
    @Test
    fun blankName_showsUnnamedUser() {
        val noName = User(deviceId = "device-blank", name = "  ", email = "anon@gmail.com")
        val s = buildAdminProfilesState(listOf(noName, alex), emptyList(), "", ProfileFilter.ALL)
        assertEquals("Unnamed user", adminProfileDisplayName(noName))
        assertEquals(listOf("device-alex", "device-blank"), ids(s))
    }

    // This checks that one profile is written as "1 profile", not "1 profiles".
    @Test
    fun countLine_singularForOne() {
        assertEquals("1 profile", adminProfileCountLine(1))
    }

    // This checks that zero and many profiles both use the plural "profiles".
    @Test
    fun countLine_pluralForManyAndZero() {
        assertEquals("12 profiles", adminProfileCountLine(12))
        assertEquals("0 profiles", adminProfileCountLine(0))
    }
}
