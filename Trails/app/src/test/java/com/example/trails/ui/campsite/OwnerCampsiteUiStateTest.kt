// Unit tests for the owner campsite page logic (US 02.01.04).
// OWNER: Ibrahim (ibr2309) — Campsite Management
package com.example.trails.ui.campsite

import com.example.trails.data.fake.dateOf
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.DateRange
import com.example.trails.data.model.Lottery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class OwnerCampsiteUiStateTest {

    private lateinit var hinton: Campsite
    private lateinit var mayLottery: Lottery

    @Before
    fun setUp() {
        hinton = Campsite(
            id = "campsite-hinton", ownerId = "owner-01", name = "Wildhorse Lake Campground",
            locationName = "Hinton - Grande Cache, AB", maxOccupancy = 8, pricePerNight = 65.0
        )
        mayLottery = Lottery(id = "lottery-may", name = "May long weekend", campsiteIds = listOf("campsite-hinton"))
    }

    @Test
    fun buildState_nullCampsite_notFound() {
        assertEquals(OwnerCampsiteUiState.NotFound, buildOwnerCampsiteState(null, listOf(mayLottery)))
    }

    @Test
    fun buildState_notInLottery_loadedWithNullName() {
        assertEquals(OwnerCampsiteUiState.Loaded(hinton, null), buildOwnerCampsiteState(hinton, emptyList()))
    }

    @Test
    fun buildState_listedInLottery_loadedWithName() {
        assertEquals(
            OwnerCampsiteUiState.Loaded(hinton, "May long weekend"),
            buildOwnerCampsiteState(hinton, listOf(mayLottery))
        )
    }

    @Test
    fun lotteryFor_activeLotteryIdWins() {
        val summer = Lottery(id = "lottery-summer", name = "Summer Rockies")
        val campsite = hinton.copy(activeLotteryId = "lottery-summer")
        assertEquals(summer, lotteryFor(campsite, listOf(mayLottery, summer)))
    }

    @Test
    fun formatCoordinates_northWest() {
        assertEquals("53.89° N, 118.01° W", formatCoordinates(53.89, -118.01))
    }

    @Test
    fun formatCoordinates_southEast() {
        assertEquals("33.87° S, 151.21° E", formatCoordinates(-33.87, 151.21))
    }

    @Test
    fun formatCoordinates_noPin_null() {
        assertNull(formatCoordinates(0.0, 0.0))
    }

    @Test
    fun formatDateRange_weekend() {
        val range = DateRange(dateOf(2027, 5, 7), dateOf(2027, 5, 9))
        assertEquals("May 7 – May 9, 2027", formatDateRange(range, Locale.US))
    }

    @Test
    fun formatDateRange_missingEnd_null() {
        assertNull(formatDateRange(DateRange(start = dateOf(2027, 5, 7), end = null), Locale.US))
    }

    @Test
    fun formatRating_roundsToOneDecimal() {
        assertEquals("4.6", formatRating(4.56))
        assertEquals("5.0", formatRating(5.0))
    }
}
