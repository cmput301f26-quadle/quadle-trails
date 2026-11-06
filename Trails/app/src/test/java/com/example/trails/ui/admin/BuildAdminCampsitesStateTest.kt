// Unit tests for buildAdminCampsitesState() and the admin Campsites text helpers (US 03.04.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BuildAdminCampsitesStateTest {

    private lateinit var jasperPines: Campsite
    private lateinit var grandeCache: Campsite
    private lateinit var bowRiver: Campsite
    private lateinit var maya: User
    private lateinit var skanda: User
    private lateinit var ava: User

    @Before
    fun setUp() {
        jasperPines = Campsite(
            id = "jasper-pines", ownerId = "device-maya", name = "Jasper Pines",
            locationName = "Jasper, AB", maxOccupancy = 6
        )
        grandeCache = Campsite(
            id = "grande-cache", ownerId = "device-skanda", name = "Grande Cache Ridge",
            locationName = "Grande Cache, AB", maxOccupancy = 4
        )
        bowRiver = Campsite(
            id = "bow-river", ownerId = "device-ava", name = "Bow River Flats",
            locationName = "Canmore, AB", maxOccupancy = 8
        )
        maya = User(deviceId = "device-maya", name = "Maya Singh")
        skanda = User(deviceId = "device-skanda", name = "Skanda R.")
        ava = User(deviceId = "device-ava", name = "Ava Patel")
    }

    private fun state(query: String = "") =
        buildAdminCampsitesState(listOf(jasperPines, bowRiver, grandeCache), listOf(maya, skanda, ava), query)

    private fun ids(s: AdminCampsitesUiState) = s.items.map { it.campsite.id }

    // This checks that a campsite shows the name of the user whose device id matches its ownerId.
    @Test
    fun ownerId_givesOwnerName() {
        val item = state().items.first { it.campsite.id == "grande-cache" }
        assertEquals("Skanda R.", item.ownerName)
    }

    // This checks that a campsite whose owner isn't in the users list shows "Unknown owner".
    @Test
    fun missingOwner_givesUnknownOwner() {
        val orphan = Campsite(id = "orphan", ownerId = "device-gone", name = "Elk Island Meadow")
        val s = buildAdminCampsitesState(listOf(orphan), listOf(maya, skanda, ava), "")
        assertEquals("Unknown owner", s.items.single().ownerName)
    }

    // This checks that an owner with a blank name shows as "Unknown owner" instead of nothing.
    @Test
    fun blankOwnerName_givesUnknownOwner() {
        val noName = User(deviceId = "device-maya", name = "  ")
        val s = buildAdminCampsitesState(listOf(jasperPines), listOf(noName), "")
        assertEquals("Unknown owner", s.items.single().ownerName)
    }

    // This checks that with no search text, every campsite is shown in A–Z order by name.
    @Test
    fun emptyQuery_showsAllSortedByName() {
        assertEquals(listOf("bow-river", "grande-cache", "jasper-pines"), ids(state()))
    }

    // This checks that searching by campsite name works no matter the upper/lower case.
    @Test
    fun query_matchesNameIgnoringCase() {
        assertEquals(listOf("jasper-pines"), ids(state("jasper PINES")))
    }

    // This checks that searching for a town finds the campsite located there.
    @Test
    fun query_matchesLocation() {
        assertEquals(listOf("bow-river"), ids(state("canmore")))
    }

    // This checks that searching for an owner's name finds the campsites they own.
    @Test
    fun query_matchesOwnerName() {
        assertEquals(listOf("bow-river"), ids(state("PATEL")))
    }

    // This checks that spaces before and after the search text are ignored.
    @Test
    fun query_withSpaces_isTrimmed() {
        assertEquals(listOf("grande-cache"), ids(state("  grande cache  ")))
    }

    // This checks that a search with no matches empties the list but still counts every campsite.
    @Test
    fun query_noMatch_emptyItemsButTotalUnchanged() {
        val s = state("lethbridge")
        assertEquals(0, s.items.size)
        assertEquals(3, s.totalCount)
    }

    // This checks that when there are no campsites at all, the list is empty and the total is zero.
    @Test
    fun noCampsites_emptyAndZeroTotal() {
        val s = buildAdminCampsitesState(emptyList(), listOf(maya, skanda, ava), "")
        assertEquals(0, s.items.size)
        assertEquals(0, s.totalCount)
    }

    // This checks that the card subtitle shows the location, a dot, then the owner.
    @Test
    fun subtitle_showsLocationAndOwner() {
        val item = AdminCampsiteItem(jasperPines, "Maya Singh")
        assertEquals("Jasper, AB · Owner Maya Singh", adminCampsiteSubtitle(item))
    }

    // This checks that a campsite with no location shows only the owner, with no stray dot.
    @Test
    fun subtitle_blankLocation_showsOwnerOnly() {
        val item = AdminCampsiteItem(jasperPines.copy(locationName = ""), "Maya Singh")
        assertEquals("Owner Maya Singh", adminCampsiteSubtitle(item))
    }

    // This checks that one campsite is written as "1 campsite", not "1 campsites".
    @Test
    fun countLine_singularForOne() {
        assertEquals("1 campsite · all owners", adminCampsiteCountLine(1))
    }

    // This checks that zero and many campsites both use the plural "campsites".
    @Test
    fun countLine_pluralForManyAndZero() {
        assertEquals("12 campsites · all owners", adminCampsiteCountLine(12))
        assertEquals("0 campsites · all owners", adminCampsiteCountLine(0))
    }
}
