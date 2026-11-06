// unit tests for waitlist view model
// owner: chitkarsh (chitkarshrathi) - waitlist
// used antigravity to generate these test cases
package com.example.trails.ui.waitlist

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.google.firebase.Timestamp
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
import java.io.IOException
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WaitlistViewModelTest {

    // referenced official coroutines test docs for standardtestdispatcher
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var waitlistRepo: FakeWaitlistRepository
    private lateinit var lotteryRepo: FakeLotteryRepository
    private lateinit var viewModel: WaitlistViewModel

    private val lotteryId = "test-lottery-id"
    private val currentUserId = "current-user-device-id"

    private val testLottery = Lottery(
        id = lotteryId,
        ownerId = "owner-01",
        name = "Rockies Summer 2027",
        campsiteIds = listOf("campsite-1"),
        waitlistCapacity = 2
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        waitlistRepo = FakeWaitlistRepository(initial = emptyList())
        lotteryRepo = FakeLotteryRepository(initial = listOf(testLottery))

        viewModel = WaitlistViewModel(
            lotteryId = lotteryId,
            userId = currentUserId,
            waitlistRepository = waitlistRepo,
            lotteryRepository = lotteryRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // initial state and loading

    @Test
    fun initialState_userNotOnWaitlist_emitsNotWaitlisted() {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is WaitlistUiState.NotWaitlisted)
        val notWaitlisted = state as WaitlistUiState.NotWaitlisted
        assertEquals(lotteryId, notWaitlisted.lottery?.id)
        assertFalse(notWaitlisted.isFull)
        assertEquals(0, notWaitlisted.entrantCount)
        assertFalse(notWaitlisted.isRulesAccepted)
    }

    @Test
    fun initialState_userAlreadyOnWaitlist_emitsWaitlisted() {
        val entry = WaitlistEntry(
            userId = currentUserId,
            userName = "Chitkarsh",
            lotteryId = lotteryId,
            partySize = 2,
            rulesAccepted = true
        )
        val prefilledRepo = FakeWaitlistRepository(initial = listOf(entry))
        val vm = WaitlistViewModel(
            lotteryId = lotteryId,
            userId = currentUserId,
            waitlistRepository = prefilledRepo,
            lotteryRepository = lotteryRepo
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WaitlistUiState.Waitlisted)
        val waitlisted = state as WaitlistUiState.Waitlisted
        assertEquals(currentUserId, waitlisted.entry.userId)
        assertEquals(1, waitlisted.entrantCount)
    }

    @Test
    fun initialState_waitlistAtCapacity_emitsNotWaitlistedWithIsFullTrue() {
        // lottery capacity is 2 so fill it with two other users
        val e1 = WaitlistEntry(userId = "user-1", lotteryId = lotteryId, rulesAccepted = true)
        val e2 = WaitlistEntry(userId = "user-2", lotteryId = lotteryId, rulesAccepted = true)
        val fullRepo = FakeWaitlistRepository(initial = listOf(e1, e2))

        val vm = WaitlistViewModel(
            lotteryId = lotteryId,
            userId = currentUserId,
            waitlistRepository = fullRepo,
            lotteryRepository = lotteryRepo
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WaitlistUiState.NotWaitlisted)
        val notWaitlisted = state as WaitlistUiState.NotWaitlisted
        assertTrue(notWaitlisted.isFull)
        assertEquals(2, notWaitlisted.entrantCount)
    }

    // rules agreement flag and validation

    @Test
    fun onRulesAgreementChanged_updatesCheckboxState() {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onRulesAgreementChanged(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as WaitlistUiState.NotWaitlisted
        assertTrue(state.isRulesAccepted)

        viewModel.onRulesAgreementChanged(false)
        testDispatcher.scheduler.advanceUntilIdle()

        val state2 = viewModel.uiState.value as WaitlistUiState.NotWaitlisted
        assertFalse(state2.isRulesAccepted)
    }

    @Test
    fun joinWaitlist_withoutRulesAccepted_emitsErrorState() {
        testDispatcher.scheduler.advanceUntilIdle()

        // attempt to join without agreeing to rules
        viewModel.joinWaitlist(userName = "Chitkarsh", partySize = 2, rulesAccepted = false)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is WaitlistUiState.Error)
        val error = state as WaitlistUiState.Error
        assertTrue(error.message.contains("rules", ignoreCase = true))
    }

    // successful database operations

    @Test
    fun joinWaitlist_withRulesAccepted_successful_transitionsToWaitlisted() {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onRulesAgreementChanged(true)
        viewModel.joinWaitlist(userName = "Chitkarsh", partySize = 3)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("expected waitlisted state but was $state", state is WaitlistUiState.Waitlisted)
        val waitlisted = state as WaitlistUiState.Waitlisted
        assertEquals(currentUserId, waitlisted.entry.userId)
        assertEquals(3, waitlisted.entry.partySize)
        assertEquals(WaitlistStatus.WAITING, waitlisted.entry.status)
        assertEquals(1, waitlisted.entrantCount)
    }

    @Test
    fun leaveWaitlist_successful_transitionsToNotWaitlisted() {
        // start as waitlisted
        viewModel.onRulesAgreementChanged(true)
        viewModel.joinWaitlist(userName = "Chitkarsh", partySize = 2)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is WaitlistUiState.Waitlisted)

        // leave waitlist
        viewModel.leaveWaitlist()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("expected not waitlisted state but was $state", state is WaitlistUiState.NotWaitlisted)
        val notWaitlisted = state as WaitlistUiState.NotWaitlisted
        assertEquals(0, notWaitlisted.entrantCount)
    }

    // full waitlists edge cases

    @Test
    fun joinWaitlist_whenWaitlistFull_emitsErrorState() {
        // pre fill to capacity 2
        val e1 = WaitlistEntry(userId = "user-1", lotteryId = lotteryId, rulesAccepted = true)
        val e2 = WaitlistEntry(userId = "user-2", lotteryId = lotteryId, rulesAccepted = true)
        val fullRepo = FakeWaitlistRepository(initial = listOf(e1, e2))

        val vm = WaitlistViewModel(
            lotteryId = lotteryId,
            userId = currentUserId,
            waitlistRepository = fullRepo,
            lotteryRepository = lotteryRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onRulesAgreementChanged(true)
        vm.joinWaitlist(userName = "Chitkarsh", partySize = 1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("expected error state but was $state", state is WaitlistUiState.Error)
        val error = state as WaitlistUiState.Error
        assertTrue(error.isFull)
        assertTrue(error.message.contains("capacity", ignoreCase = true))
    }

    // firebase network exceptions and failure recovery

    @Test
    fun joinWaitlist_firebaseNetworkException_emitsErrorWithoutCrash() {
        testDispatcher.scheduler.advanceUntilIdle()

        waitlistRepo.failure = IOException("firebase network unavailable")
        viewModel.onRulesAgreementChanged(true)
        viewModel.joinWaitlist(userName = "Chitkarsh", partySize = 2)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is WaitlistUiState.Error)
        val error = state as WaitlistUiState.Error
        assertTrue(error.message.contains("network", ignoreCase = true) || error.message.contains("unavailable", ignoreCase = true))
    }

    @Test
    fun leaveWaitlist_firebaseNetworkException_emitsErrorWithoutCrash() {
        // start as waitlisted
        viewModel.onRulesAgreementChanged(true)
        viewModel.joinWaitlist(userName = "Chitkarsh", partySize = 2)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is WaitlistUiState.Waitlisted)

        // inject network failure
        waitlistRepo.failure = IOException("simulated network timeout")
        viewModel.leaveWaitlist()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is WaitlistUiState.Error)
    }

    @Test
    fun clearError_restoresValidState() {
        testDispatcher.scheduler.advanceUntilIdle()

        // trigger error
        viewModel.joinWaitlist(userName = "Chitkarsh", rulesAccepted = false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is WaitlistUiState.Error)

        // clear error
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is WaitlistUiState.NotWaitlisted)
    }

    @Test
    fun entrants_emitsAllEntrants() {
        val t1 = Timestamp(Date(100000))
        val t2 = Timestamp(Date(200000))
        val e1 = WaitlistEntry(userId = "user-1", lotteryId = lotteryId, joinedAt = t1, rulesAccepted = true)
        val e2 = WaitlistEntry(userId = "user-2", lotteryId = lotteryId, joinedAt = t2, rulesAccepted = true)

        val repo = FakeWaitlistRepository(initial = listOf(e1, e2))
        val vm = WaitlistViewModel(
            lotteryId = lotteryId,
            userId = currentUserId,
            waitlistRepository = repo,
            lotteryRepository = lotteryRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.entrants.value.size)
        assertEquals(listOf("user-1", "user-2"), vm.entrants.value.map { it.userId })
    }

    @Test
    fun entrants_emitsEntrantsStrictlySortedByJoinedAtAscending() {
        val t1 = Timestamp(Date(100000))
        val t2 = Timestamp(Date(200000))
        val t3 = Timestamp(Date(300000))
        val e1 = WaitlistEntry(userId = "user-1", lotteryId = lotteryId, joinedAt = t1, rulesAccepted = true)
        val e2 = WaitlistEntry(userId = "user-2", lotteryId = lotteryId, joinedAt = t2, rulesAccepted = true)
        val e3 = WaitlistEntry(userId = "user-3", lotteryId = lotteryId, joinedAt = t3, rulesAccepted = true)

        // feed unsorted list
        val repo = FakeWaitlistRepository(initial = listOf(e3, e1, e2))
        val vm = WaitlistViewModel(
            lotteryId = lotteryId,
            userId = currentUserId,
            waitlistRepository = repo,
            lotteryRepository = lotteryRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, vm.entrants.value.size)
        assertEquals(listOf("user-1", "user-2", "user-3"), vm.entrants.value.map { it.userId })
    }

    @Test
    fun loadData_campsiteIdOnly_dynamicallyResolvesLotteryAndObserves() {
        val entry = WaitlistEntry(
            userId = "user-campsite",
            userName = "Live User",
            lotteryId = lotteryId,
            rulesAccepted = true
        )
        val waitlistRepo = FakeWaitlistRepository(initial = listOf(entry))
        val campsite = Campsite(
            id = "campsite-dynamically-resolved",
            name = "Live Banff Campsite",
            activeLotteryId = lotteryId
        )
        val campsiteRepo = FakeCampsiteRepository(initial = listOf(campsite))

        val vm = WaitlistViewModel(
            lotteryId = "",
            campsiteId = "campsite-dynamically-resolved",
            userId = currentUserId,
            waitlistRepository = waitlistRepo,
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Live Banff Campsite", vm.campsite.value?.name)
        assertEquals(lotteryId, vm.lottery.value?.id)
        assertEquals(1, vm.entrants.value.size)
        assertEquals("user-campsite", vm.entrants.value.first().userId)
    }

    // error state successfully retains campsite and lottery data to prevent ui state loss
    // used antigravity to generate these test cases
    @Test
    fun uiStateError_retainsCampsiteAndLotteryData_preventsUiStateLoss() {
        val campsite = Campsite(
            id = "campsite-1",
            name = "Grande Cache Ridge",
            activeLotteryId = lotteryId
        )
        val campsiteRepo = FakeCampsiteRepository(initial = listOf(campsite))
        val vm = WaitlistViewModel(
            lotteryId = lotteryId,
            campsiteId = "campsite-1",
            userId = currentUserId,
            waitlistRepository = waitlistRepo,
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // trigger error via network failure
        waitlistRepo.failure = IOException("firestore network timeout")
        vm.onRulesAgreementChanged(true)
        vm.joinWaitlist(userName = "Chitkarsh", partySize = 2)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WaitlistUiState.Error)
        val errorState = state as WaitlistUiState.Error
        assertEquals("Grande Cache Ridge", errorState.campsite?.name)
        assertEquals(lotteryId, errorState.lottery?.id)
        assertTrue(errorState.message.contains("network", ignoreCase = true) || errorState.message.contains("timeout", ignoreCase = true))
    }

    // join waitlist guards against execution when active lottery id is null or empty
    @Test
    fun joinWaitlist_campsiteWithNoActiveLottery_setsErrorMessageAndGuardsExecution() {
        val campsiteWithNoLottery = Campsite(
            id = "campsite-no-lottery",
            name = "Two jack lakeside",
            activeLotteryId = null
        )
        val campsiteRepo = FakeCampsiteRepository(initial = listOf(campsiteWithNoLottery))

        val vm = WaitlistViewModel(
            lotteryId = "",
            campsiteId = "campsite-no-lottery",
            userId = currentUserId,
            waitlistRepository = waitlistRepo,
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // user agrees to rules and tries to join
        vm.onRulesAgreementChanged(true)
        vm.joinWaitlist(userName = "Chitkarsh", partySize = 2)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WaitlistUiState.Error)
        val error = state as WaitlistUiState.Error
        assertEquals("This campsite is not in a lottery right now.", error.message)
        assertEquals("Two jack lakeside", error.campsite?.name)
        // ensure no entrants are present
        assertEquals(0, vm.entrants.value.size)
    }

    // leave waitlist guards against execution when active lottery id is empty
    @Test
    fun leaveWaitlist_noActiveLottery_setsErrorMessage() {
        val campsiteWithNoLottery = Campsite(
            id = "campsite-no-lottery-2",
            name = "Two jack lakeside",
            activeLotteryId = ""
        )
        val campsiteRepo = FakeCampsiteRepository(initial = listOf(campsiteWithNoLottery))

        val vm = WaitlistViewModel(
            lotteryId = "",
            campsiteId = "campsite-no-lottery-2",
            userId = currentUserId,
            waitlistRepository = waitlistRepo,
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.leaveWaitlist()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WaitlistUiState.Error)
        val error = state as WaitlistUiState.Error
        assertEquals("This campsite is not in a lottery right now.", error.message)
    }
}
