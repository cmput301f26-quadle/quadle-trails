// Unit tests for buildHostingState(), the My Campsites controller logic (US 02.03.01).
// OWNER: Ibrahim (ibr2309) — Campsite Management
package com.example.trails.ui.campsite

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BuildHostingStateTest {

    private lateinit var alpine1: Campsite
    private lateinit var alpine2: Campsite
    private lateinit var twoJack: Campsite
    private lateinit var mayLongWeekend: Lottery

    @Before
    fun setUp() {
        alpine1 = Campsite(
            id = "alpine-1", ownerId = "owner-01", name = "Alpine Lake · Site 01",
            locationName = "Banff, AB", maxOccupancy = 6, activeLotteryId = "lottery-may"
        )
        alpine2 = Campsite(
            id = "alpine-2", ownerId = "owner-01", name = "Alpine Lake · Site 02",
            locationName = "Banff, AB", maxOccupancy = 8
        )
        twoJack = Campsite(
            id = "two-jack", ownerId = "owner-01", name = "Two Jack Lakeside",
            locationName = "Banff National Park, AB", maxOccupancy = 4
        )
        mayLongWeekend = Lottery(
            id = "lottery-may", ownerId = "owner-01", name = "May long weekend",
            campsiteIds = listOf("alpine-1", "alpine-2")
        )
    }

    private fun state(query: String = "") =
        buildHostingState(listOf(twoJack, alpine2, alpine1), listOf(mayLongWeekend), query)

    private fun lotteryOf(s: HostingUiState, id: String) =
        s.items.first { it.campsite.id == id }.lotteryName

    @Test
    fun activeLotteryId_givesLotteryName() {
        assertEquals("May long weekend", lotteryOf(state(), "alpine-1"))
    }

    @Test
    fun listedInLotteryCampsiteIds_givesLotteryName() {
        assertEquals("May long weekend", lotteryOf(state(), "alpine-2"))
    }

    @Test
    fun notInAnyLottery_nullLotteryName() {
        assertNull(lotteryOf(state(), "two-jack"))
    }

    @Test
    fun noQuery_allSortedByName() {
        assertEquals(listOf("alpine-1", "alpine-2", "two-jack"), state().items.map { it.campsite.id })
    }

    @Test
    fun query_matchesNameIgnoringCase() {
        assertEquals(listOf("two-jack"), state("two JACK").items.map { it.campsite.id })
    }

    @Test
    fun query_matchesLocation() {
        assertEquals(listOf("two-jack"), state("national park").items.map { it.campsite.id })
    }

    @Test
    fun query_noMatch_emptyItemsButCountsUnchanged() {
        val s = state("jasper")
        assertEquals(0, s.items.size)
        assertEquals(3, s.totalCount)
        assertEquals(2, s.inLotteryCount)
    }

    @Test
    fun noCampsites_zeroCounts() {
        val s = buildHostingState(emptyList(), listOf(mayLongWeekend), "")
        assertEquals(0, s.totalCount)
        assertEquals(0, s.inLotteryCount)
    }

    @Test
    fun pluralCampsites_singularAndPlural() {
        assertEquals("1 campsite", pluralCampsites(1))
        assertEquals("3 campsites", pluralCampsites(3))
    }
}
