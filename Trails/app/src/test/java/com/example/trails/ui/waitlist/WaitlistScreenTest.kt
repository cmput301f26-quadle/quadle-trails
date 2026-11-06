// Unit test suite for WaitlistScreen UI controller state bindings and flows (UI Controller in MVVM).
// implements US 01.01.06, US 01.02.01, US 01.02.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.ui.waitlist

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for waitlist screen view model state observation and interactions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WaitlistScreenTest {

    private val testDispatcher = StandardTestDispatcher()

    private val lotteryId = "test-lottery-id"
    private val campsiteId = "test-campsite-id"
    private val userId = "test-user-id"

    private lateinit var waitlistRepo: FakeWaitlistRepository
    private lateinit var lotteryRepo: FakeLotteryRepository
    private lateinit var campsiteRepo: FakeCampsiteRepository
    private lateinit var viewModel: WaitlistViewModel

    private val campsite = Campsite(
        id = campsiteId,
        ownerId = "owner-01",
        name = "Grande Cache Ridge",
        locationName = "Hinton – Grande Cache, AB",
        pricePerNight = 65.0,
        rules = "Quiet hours 10pm-7am."
    )

    private val lottery = Lottery(
        id = lotteryId,
        ownerId = "owner-01",
        name = "All May weekends · 2027",
        campsiteIds = listOf(campsiteId),
        waitlistCapacity = 5
    )

    // used agent for the repetitive setup here
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        waitlistRepo = FakeWaitlistRepository(initial = emptyList())
        lotteryRepo = FakeLotteryRepository(initial = listOf(lottery))
        campsiteRepo = FakeCampsiteRepository(initial = listOf(campsite))

        viewModel = WaitlistViewModel(
            lotteryId = lotteryId,
            userId = userId,
            waitlistRepository = waitlistRepo,
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo,
            campsiteId = campsiteId
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // screen loads campsite and lottery details correctly
    @Test
    fun loadData_populatesCampsiteAndLotteryInState() {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is WaitlistUiState.NotWaitlisted)
        val notWaitlisted = state as WaitlistUiState.NotWaitlisted
        assertEquals("Grande Cache Ridge", notWaitlisted.campsite?.name)
        assertEquals("All May weekends · 2027", notWaitlisted.lottery?.name)
        assertFalse(notWaitlisted.isFull)
        assertEquals(0, notWaitlisted.entrantCount)
    }

    // checking rules box updates state so user can join
    @Test
    fun rulesAgreement_togglesStateInViewModel() {
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as WaitlistUiState.NotWaitlisted
        assertFalse(state.isRulesAccepted)

        viewModel.onRulesAgreementChanged(true)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value as WaitlistUiState.NotWaitlisted
        assertTrue(state.isRulesAccepted)
    }

    // clicking join button with rules accepted triggers join flow
    @Test
    fun joinButton_triggersBackendFlowAndUpdatesState() {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onRulesAgreementChanged(true)
        viewModel.joinWaitlist(userName = "Chitkarsh", partySize = 2)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is WaitlistUiState.Waitlisted)
        val waitlisted = state as WaitlistUiState.Waitlisted
        assertEquals(userId, waitlisted.entry.userId)
        assertEquals(2, waitlisted.entry.partySize)
        assertEquals(WaitlistStatus.WAITING, waitlisted.entry.status)
        assertEquals("Grande Cache Ridge", waitlisted.campsite?.name)
    }

    // clicking leave button removes entry and returns to not waitlisted
    @Test
    fun leaveButton_triggersLeaveFlow() {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onRulesAgreementChanged(true)
        viewModel.joinWaitlist(userName = "Chitkarsh", partySize = 2)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is WaitlistUiState.Waitlisted)

        viewModel.leaveWaitlist()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is WaitlistUiState.NotWaitlisted)
        val notWaitlisted = state as WaitlistUiState.NotWaitlisted
        assertEquals(0, notWaitlisted.entrantCount)
    }

    // full waitlist capacity prevents joining and shows full state
    @Test
    fun fullWaitlist_capacityFlaggedInState() {
        // fill waitlist to capacity 5
        val fullEntries = (1..5).map { i ->
            WaitlistEntry(userId = "entrant-$i", lotteryId = lotteryId, rulesAccepted = true)
        }
        val fullRepo = FakeWaitlistRepository(initial = fullEntries)

        val vm = WaitlistViewModel(
            lotteryId = lotteryId,
            userId = userId,
            waitlistRepository = fullRepo,
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo,
            campsiteId = campsiteId
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WaitlistUiState.NotWaitlisted)
        val notWaitlisted = state as WaitlistUiState.NotWaitlisted
        assertTrue(notWaitlisted.isFull)
        assertEquals(5, notWaitlisted.entrantCount)
    }
}
