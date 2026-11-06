// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01
// PURPOSE: Unit tests for the admin Campsite page's state and display text.
// Outstanding issues: none.
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class AdminCampsiteStateTest {

    private val jasperPines = Campsite(
        id = "jasper-pines",
        ownerId = "device-maya",
        name = "Jasper Pines",
        locationName = "Jasper, AB",
        maxOccupancy = 6,
        pricePerNight = 40.0,
        ratingAverage = 4.5,
        ratingCount = 12
    )
    private val maya = User(
        deviceId = "device-maya", name = "Maya Singh", email = "maya.singh@example.com",
        roles = listOf(Role.ENTRANT, Role.OWNER)
    )
    private val priya = User(deviceId = "device-priya", name = "Priya Sharma", email = "priya.sharma@example.com")

    // This checks that an existing campsite gives a Loaded state with its owner's name and the removal counts.
    @Test
    fun buildAdminCampsiteState_loaded_hasOwnerAndCounts() {
        val state = buildAdminCampsiteState(jasperPines, listOf(priya, maya), CampsiteImpact("Jasper Pines", 2, 5))
        assertEquals(AdminCampsiteUiState.Loaded(jasperPines, "Maya Singh", 2, 5), state)
    }

    // This checks that a missing campsite gives the NotFound state.
    @Test
    fun buildAdminCampsiteState_missingCampsite_isNotFound() {
        assertEquals(AdminCampsiteUiState.NotFound, buildAdminCampsiteState(null, listOf(maya), null))
    }

    // This checks that unknown counts show as 0 rather than failing.
    @Test
    fun buildAdminCampsiteState_noImpact_countsAreZero() {
        val state = buildAdminCampsiteState(jasperPines, listOf(maya), null) as AdminCampsiteUiState.Loaded
        assertEquals(0, state.lotteryCount)
        assertEquals(0, state.entryCount)
    }

    // This checks that the owner's name comes from the matching user, and "Unknown owner" if none or blank.
    @Test
    fun ownerName_matchOrUnknown() {
        assertEquals("Maya Singh", ownerName("device-maya", listOf(priya, maya)))
        assertEquals("Unknown owner", ownerName("device-gone", listOf(priya, maya)))
        assertEquals("Unknown owner", ownerName("device-maya", listOf(maya.copy(name = "  "))))
    }

    // This checks that the rating shows the average to one decimal with the count.
    @Test
    fun ratingLabel_withRatings_showsAverageAndCount() {
        assertEquals("★ 4.5 (12)", ratingLabel(jasperPines))
        assertEquals("★ 3.7 (3)", ratingLabel(jasperPines.copy(ratingAverage = 11.0 / 3, ratingCount = 3)))
    }

    // This checks that a campsite nobody has rated shows "No ratings".
    @Test
    fun ratingLabel_noRatings_saysSo() {
        assertEquals("No ratings", ratingLabel(jasperPines.copy(ratingAverage = 0.0, ratingCount = 0)))
    }

    // This checks the dialog text names the campsite and the counts, using plurals.
    @Test
    fun removeCampsiteMessage_plural_listsCounts() {
        assertEquals(
            "Jasper Pines is removed for everyone. Its 2 lotteries and 5 lottery entries are removed too. " +
                "This can't be undone.",
            removeCampsiteMessage("Jasper Pines", 2, 5)
        )
    }

    // This checks the dialog text uses the singular for one lottery and one entry, and handles zero.
    @Test
    fun removeCampsiteMessage_singularAndZero_readsNaturally() {
        assertEquals(
            "Wapiti Flats is removed for everyone. Its 1 lottery and 1 lottery entry are removed too. " +
                "This can't be undone.",
            removeCampsiteMessage("Wapiti Flats", 1, 1)
        )
        assertEquals(
            "Wapiti Flats is removed for everyone. Its 0 lotteries and 0 lottery entries are removed too. " +
                "This can't be undone.",
            removeCampsiteMessage("Wapiti Flats", 0, 0)
        )
    }
}
