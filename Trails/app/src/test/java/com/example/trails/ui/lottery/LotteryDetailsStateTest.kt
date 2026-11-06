// Unit tests for the Lottery details page state.
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestLotteries
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.WaitlistEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LotteryDetailsStateTest {

    private val day = 86_400_000L
    private val now = 1_800_000_000_000L
    private val dispatcher = StandardTestDispatcher()
    private val mayLongWeekend = Lottery(
        id = "lottery-may", name = "May long weekend",
        campsiteIds = listOf(TestCampsites.JASPER_ID, TestCampsites.BANFF_ID),
        registrationStart = timestampOf(now + 12 * day), registrationEnd = timestampOf(now + 19 * day)
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun missingLottery_isNotFound() {
        assertEquals(LotteryDetailsUiState.NotFound, buildLotteryDetailsState(null, TestCampsites.all, 0, now))
    }

    // Campsites come back in the lottery's order, not the repository's.
    @Test
    fun loaded_campsitesInLotteryOrder() {
        val state = buildLotteryDetailsState(mayLongWeekend, TestCampsites.all, 0, now) as LotteryDetailsUiState.Loaded
        assertEquals(listOf(TestCampsites.JASPER_ID, TestCampsites.BANFF_ID), state.campsites.map { it.id })
        assertEquals(LotteryStatus.SCHEDULED, state.status)
        assertTrue(state.canEdit)
    }

    // US 02.04.06: a lottery can be edited until the draw, then Edit goes away.
    @Test
    fun afterTheDraw_cantEdit() {
        val state = buildLotteryDetailsState(mayLongWeekend, TestCampsites.all, 4, now + 20 * day) as LotteryDetailsUiState.Loaded
        assertEquals(LotteryStatus.DRAWN, state.status)
        assertFalse(state.canEdit)
    }

    // The page updates by itself when someone joins the waiting list.
    @Test
    fun viewModel_updatesWhenSomeoneJoins() = runTest(dispatcher) {
        val waitlist = FakeWaitlistRepository(emptyList())
        val vm = LotteryDetailsViewModel(
            TestLotteries.LOTTERY_ID, FakeLotteryRepository(), FakeCampsiteRepository(), waitlist, clock = { System.currentTimeMillis() }
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(0, (vm.uiState.value as LotteryDetailsUiState.Loaded).entrantCount)

        waitlist.save(WaitlistEntry(userId = "u1", lotteryId = TestLotteries.LOTTERY_ID, rulesAccepted = true))
        advanceUntilIdle()
        assertEquals(1, (vm.uiState.value as LotteryDetailsUiState.Loaded).entrantCount)
    }

    // A closed lottery nobody entered can be deleted from its details page.
    @Test
    fun viewModel_deletesClosedLotteryNobodyEntered() = runTest(dispatcher) {
        val lotteries = FakeLotteryRepository()
        val campsites = FakeCampsiteRepository()
        val vm = LotteryDetailsViewModel(TestLotteries.CLOSED_LOTTERY_ID, lotteries, campsites, FakeWaitlistRepository(emptyList()))
        vm.onDelete()
        advanceUntilIdle()
        assertTrue(vm.deleteState.value.deleted)
        assertEquals(null, lotteries.getById(TestLotteries.CLOSED_LOTTERY_ID))
        // Jasper still belongs to the running summer lottery, so it isn't touched.
        assertEquals(TestLotteries.LOTTERY_ID, campsites.getById(TestCampsites.JASPER_ID)?.activeLotteryId)
    }
}
