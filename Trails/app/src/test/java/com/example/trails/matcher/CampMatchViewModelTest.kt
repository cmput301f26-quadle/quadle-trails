// Unit test suite verifying CampMatchViewModel search, tag toggles, and reactive ranking.
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.matcher.data.InMemoryPreferencesRepository
import com.example.trails.matcher.model.CamperPreferences
import com.example.trails.matcher.ui.CampMatchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [CampMatchViewModel] behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CampMatchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val siteLakeside = Campsite(
        id = "site-lakeside",
        name = "Lake Louise Camp",
        locationName = "Banff, AB",
        tags = listOf("Fire pit", "Lakeside", "Showers"),
        pricePerNight = 40.0,
        activeLotteryId = "lottery-1"
    )

    private val siteForest = Campsite(
        id = "site-forest",
        name = "Deep Forest Camp",
        locationName = "Jasper, AB",
        tags = listOf("Deep forest", "Hiking"),
        pricePerNight = 35.0
    )

    private val sampleLottery = Lottery(
        id = "lottery-1",
        name = "Lake Draw",
        numWinners = 5
    )

    private lateinit var campsiteRepo: FakeCampsiteRepository
    private lateinit var lotteryRepo: FakeLotteryRepository
    private lateinit var waitlistRepo: FakeWaitlistRepository
    private lateinit var prefsRepo: InMemoryPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepo = FakeCampsiteRepository(initial = listOf(siteLakeside, siteForest))
        lotteryRepo = FakeLotteryRepository(initial = listOf(sampleLottery))
        waitlistRepo = FakeWaitlistRepository(initial = emptyList())
        prefsRepo = InMemoryPreferencesRepository(
            initial = CamperPreferences(preferredTags = setOf("Lakeside"))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CampMatchViewModel = CampMatchViewModel(
        campsiteRepository = campsiteRepo,
        lotteryRepository = lotteryRepo,
        waitlistRepository = waitlistRepo,
        preferencesRepository = prefsRepo
    )

    @Test
    fun init_loadsRankedCampsites_matchingPreferredTagOnTop() {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(setOf("Lakeside"), state.selectedTags)
        assertEquals(2, state.rankedMatches.size)

        // Lakeside site should be ranked first
        assertEquals("site-lakeside", state.rankedMatches[0].campsite.id)
        assertTrue(state.rankedMatches[0].matchResult.matchPercentage > state.rankedMatches[1].matchResult.matchPercentage)
    }

    @Test
    fun onSearchQueryChanged_filtersAvailableAmenities() {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onSearchQueryChanged("Showers")
        val state = vm.uiState.value
        assertEquals("Showers", state.searchQuery)
        assertTrue(state.availableTags.any { it.tag == "Showers" })
        assertFalse(state.availableTags.any { it.tag == "Fishing" })

        // Clear search
        vm.clearSearchQuery()
        assertEquals("", vm.uiState.value.searchQuery)
        assertTrue(vm.uiState.value.availableTags.size > 5)
    }

    @Test
    fun onTagToggled_addsAndRemovesTag_updatingPreferences() {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle "Fire pit" ON
        vm.onTagToggled("Fire pit")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Fire pit should be selected", "Fire pit" in vm.uiState.value.selectedTags)
        assertTrue("Lakeside should still be selected", "Lakeside" in vm.uiState.value.selectedTags)

        // Toggle "Fire pit" OFF
        vm.onTagToggled("Fire pit")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("Fire pit should be deselected", "Fire pit" in vm.uiState.value.selectedTags)
    }

    @Test
    fun onClearAllTags_removesAllSelectedPreferences() {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onClearAllTags()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.selectedTags.isEmpty())
    }

    @Test
    fun onMaxPriceChanged_updatesBudgetFilter() {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onMaxPriceChanged(50.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(50.0, vm.uiState.value.maxPricePerNight)

        vm.onMaxPriceChanged(null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, vm.uiState.value.maxPricePerNight)
    }

    @Test
    fun onTogglePrioritizeOdds_invertsFlag() {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val initial = vm.uiState.value.prioritizeHighWinOdds
        vm.onTogglePrioritizeOdds()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(!initial, vm.uiState.value.prioritizeHighWinOdds)
    }
}
