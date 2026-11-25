// Unit test suite verifying SelectCampsiteViewModel data retrieval and state mapping (MVVM pattern).
// implements US 01.04.02, US 02.04.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.ui.waitlist

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying the behavior of [SelectCampsiteViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectCampsiteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val site1 = Campsite(
        id = "site-1",
        name = "Grande Cache Ridge",
        locationName = "Hinton, AB",
        pricePerNight = 45.0
    )
    private val site2 = Campsite(
        id = "site-2",
        name = "Two Jack Lakeside Campground",
        locationName = "Banff, AB",
        pricePerNight = 35.0
    )

    private val lottery = Lottery(
        id = "lottery-multi",
        name = "Rockies Summer Multi-Draw",
        campsiteIds = listOf("site-1", "site-2")
    )

    private lateinit var lotteryRepo: FakeLotteryRepository
    private lateinit var campsiteRepo: FakeCampsiteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        lotteryRepo = FakeLotteryRepository(initial = listOf(lottery))
        campsiteRepo = FakeCampsiteRepository(initial = listOf(site1, site2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsLotteryAndCampsitesSuccessfully() {
        val viewModel = SelectCampsiteViewModel(
            lotteryId = "lottery-multi",
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SelectCampsiteUiState.Success)
        val success = state as SelectCampsiteUiState.Success
        assertEquals("lottery-multi", success.lottery.id)
        assertEquals(2, success.campsites.size)
        assertEquals("site-1", success.campsites[0].id)
        assertEquals("site-2", success.campsites[1].id)
    }

    @Test
    fun init_lotteryNotFound_emitsError() {
        val viewModel = SelectCampsiteViewModel(
            lotteryId = "unknown-lottery",
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SelectCampsiteUiState.Error)
        val error = state as SelectCampsiteUiState.Error
        assertEquals("Lottery not found.", error.message)
    }

    @Test
    fun init_blankLotteryId_emitsError() {
        val viewModel = SelectCampsiteViewModel(
            lotteryId = "",
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SelectCampsiteUiState.Error)
        val error = state as SelectCampsiteUiState.Error
        assertEquals("Lottery identifier cannot be blank.", error.message)
    }

    @Test
    fun init_noCampsitesFound_emitsError() {
        val emptyLottery = Lottery(id = "empty-lottery", name = "Empty Draw", campsiteIds = listOf("missing-1"))
        lotteryRepo = FakeLotteryRepository(initial = listOf(emptyLottery))

        val viewModel = SelectCampsiteViewModel(
            lotteryId = "empty-lottery",
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SelectCampsiteUiState.Error)
        val error = state as SelectCampsiteUiState.Error
        assertEquals("No campsites found for this lottery.", error.message)
    }
}
