// Unit tests for LotteryFormViewModel: the wizard, publishing, editing and save failures.
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.WaitlistEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LotteryFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val day = 86_400_000L
    private val now = 1_800_000_000_000L
    private val owner = TestUsers.OWNER_ID

    // Summer is still running on Jasper; Spring closed last month on Hinton, so Hinton is free again.
    private val summer = Lottery(id = "lottery-summer", ownerId = owner, name = "Summer weekends",
        campsiteIds = listOf("jasper"), registrationStart = timestampOf(now - 3 * day), registrationEnd = timestampOf(now + 10 * day))
    private val spring = Lottery(id = "lottery-spring", ownerId = owner, name = "Spring opening",
        campsiteIds = listOf("hinton"), registrationStart = timestampOf(now - 50 * day), registrationEnd = timestampOf(now - 40 * day),
        bookingStart = timestampOf(now - 40 * day), bookingEnd = timestampOf(now - 38 * day), numWinners = 3)
    private val banff = Campsite(id = "banff", ownerId = owner, name = "Tunnel Mountain Site 14", locationName = "Banff, AB", maxOccupancy = 6)
    private val jasper = Campsite(id = "jasper", ownerId = owner, name = "Whistlers Riverside Site 7", locationName = "Jasper, AB", activeLotteryId = summer.id)
    private val hinton = Campsite(id = "hinton", ownerId = owner, name = "Wildhorse Lake Site 3", locationName = "Hinton, AB", activeLotteryId = spring.id)

    private lateinit var lotteries: FakeLotteryRepository
    private lateinit var campsites: FakeCampsiteRepository
    private lateinit var waitlist: FakeWaitlistRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        lotteries = FakeLotteryRepository(listOf(summer, spring))
        campsites = FakeCampsiteRepository(listOf(banff, jasper, hinton))
        waitlist = FakeWaitlistRepository(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(lotteryId: String? = null, campsiteId: String? = null, reuseId: String? = null): LotteryFormViewModel {
        val vm = LotteryFormViewModel(lotteries, campsites, waitlist, owner, lotteryId, campsiteId, reuseId, clock = { now })
        advanceUntilIdle()
        return vm
    }

    // Fills in valid dates: registration opens in 12 days for a week, booking right after.
    private fun LotteryFormViewModel.setValidDates() {
        onRegistrationChange(now + 12 * day, now + 19 * day)
        onBookingChange(now + 19 * day, now + 21 * day)
    }

    // "Create Lottery" on a campsite page passes its id, which starts ticked (US 02.04.01).
    @Test
    fun campsiteFromRoute_startsTicked() = runTest(dispatcher) {
        val vm = viewModel(campsiteId = "banff")
        assertEquals(listOf("banff"), vm.state.value.draft.campsiteIds)
    }

    @Test
    fun campsiteInRunningLottery_cantBeTicked() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onToggleCampsite("jasper")
        val options = vm.state.value.campsites.associateBy { it.campsite.id }
        assertEquals("Summer weekends", options.getValue("jasper").takenBy)
        assertTrue(vm.state.value.draft.campsiteIds.isEmpty())
    }

    // A campsite whose lottery already closed can go in a new one.
    @Test
    fun campsiteInClosedLottery_isFree() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onToggleCampsite("hinton")
        vm.onToggleCampsite("banff")
        assertEquals(listOf("hinton", "banff"), vm.state.value.draft.campsiteIds)
    }

    // Each step's button only works once that step is complete (Figma F07: "Next stays disabled").
    @Test
    fun next_onlyMovesOnWhenTheStepIsComplete() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onNext()
        assertEquals(LotteryStep.CAMPSITES, vm.state.value.step)
        vm.onToggleCampsite("banff")
        vm.onNext()
        assertEquals(LotteryStep.DATES, vm.state.value.step)
        vm.onNext()
        assertEquals(LotteryStep.DATES, vm.state.value.step)
        vm.setValidDates()
        vm.onNext()
        vm.onNext()
        assertEquals(LotteryStep.REVIEW, vm.state.value.step)
    }

    @Test
    fun back_goesToThePreviousStepThenLeaves() = runTest(dispatcher) {
        val vm = viewModel(campsiteId = "banff")
        vm.onNext()
        assertTrue(vm.onBack())
        assertEquals(LotteryStep.CAMPSITES, vm.state.value.step)
        assertFalse(vm.onBack())
    }

    // Edit on the review page opens that step, and its button returns straight to the review.
    @Test
    fun editFromReview_comesBackToReview() = runTest(dispatcher) {
        val vm = viewModel(campsiteId = "banff")
        vm.onNext(); vm.setValidDates(); vm.onNext(); vm.onNext()
        vm.onEditStep(LotteryStep.CAMPSITES)
        assertEquals(LotteryStep.CAMPSITES, vm.state.value.step)
        vm.onNext()
        assertEquals(LotteryStep.REVIEW, vm.state.value.step)
    }

    // Publishing saves every setting and points the chosen campsites at the new lottery.
    @Test
    fun publish_savesLotteryAndLinksCampsites() = runTest(dispatcher) {
        val vm = viewModel(campsiteId = "banff")
        vm.onToggleCampsite("hinton")
        vm.setValidDates()
        vm.onWinnersChange(5)
        vm.onWaitlistLimitChange(true)
        vm.onNameChange("May long weekend")
        vm.onSave()
        advanceUntilIdle()

        val id = vm.state.value.savedId
        assertNotNull(id)
        val saved = lotteries.getById(id!!)!!
        assertEquals("May long weekend", saved.name)
        assertEquals(owner, saved.ownerId)
        assertEquals(listOf("banff", "hinton"), saved.campsiteIds)
        assertEquals(now + 19 * day, saved.registrationEnd.millis())
        assertEquals(5, saved.numWinners)
        assertEquals(DEFAULT_WAITLIST, saved.waitlistCapacity)
        assertEquals(id, campsites.getById("banff")?.activeLotteryId)
        assertEquals(id, campsites.getById("hinton")?.activeLotteryId)
    }

    @Test
    fun publish_withoutName_doesNothing() = runTest(dispatcher) {
        val vm = viewModel(campsiteId = "banff")
        vm.setValidDates()
        vm.onSave()
        advanceUntilIdle()
        assertNull(vm.state.value.savedId)
        assertEquals(2, lotteries.observeAll().first().size)
    }

    // Editing loads the lottery; removing a campsite frees it (US 02.04.06).
    @Test
    fun edit_removingACampsiteFreesIt() = runTest(dispatcher) {
        val vm = viewModel(lotteryId = summer.id)
        vm.onToggleCampsite("banff")
        vm.onToggleCampsite("jasper")
        vm.onBookingChange(now + 10 * day, now + 12 * day)
        vm.onSave()
        advanceUntilIdle()

        assertEquals(summer.id, vm.state.value.savedId)
        assertEquals(listOf("banff"), lotteries.getById(summer.id)!!.campsiteIds)
        assertNull(campsites.getById("jasper")?.activeLotteryId)
        assertEquals(summer.id, campsites.getById("banff")?.activeLotteryId)
    }

    @Test
    fun edit_oneChange_isDescribedForTheBanner() = runTest(dispatcher) {
        lotteries.save(summer.copy(bookingStart = timestampOf(now + 10 * day), bookingEnd = timestampOf(now + 12 * day)))
        val vm = viewModel(lotteryId = summer.id)
        vm.onWinnersChange(6)
        vm.onSave()
        advanceUntilIdle()
        assertEquals(LotteryChange.Winners(6), vm.state.value.savedChange)
    }

    // Registration already opened, so the limit can't drop below the people who joined.
    @Test
    fun edit_countsEntrantsForTheWaitlistLimit() = runTest(dispatcher) {
        waitlist = FakeWaitlistRepository(List(3) { WaitlistEntry(userId = "u$it", lotteryId = summer.id) })
        val vm = viewModel(lotteryId = summer.id)
        assertEquals(3, vm.state.value.entrantCount)
        assertEquals(now - 3 * day, vm.state.value.openedAt)
    }

    @Test
    fun closedLottery_isLocked() = runTest(dispatcher) {
        val vm = viewModel(lotteryId = spring.id)
        assertTrue(vm.state.value.isLocked)
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun unknownLottery_isNotFound() = runTest(dispatcher) {
        val vm = viewModel(lotteryId = "missing")
        assertTrue(vm.state.value.notFound)
    }

    // Figma F21: a failed save keeps the edits and Try again saves them.
    @Test
    fun failedSave_keepsEditsAndRetryWorks() = runTest(dispatcher) {
        val vm = viewModel(campsiteId = "banff")
        vm.setValidDates()
        vm.onNameChange("May long weekend")
        lotteries.failure = IllegalStateException("offline")
        vm.onSave()
        advanceUntilIdle()
        assertTrue(vm.state.value.saveFailed)
        assertNull(vm.state.value.savedId)
        assertEquals("May long weekend", vm.state.value.draft.name)

        vm.onSave()
        advanceUntilIdle()
        assertFalse(vm.state.value.saveFailed)
        assertNotNull(vm.state.value.savedId)
    }

    // "Reuse settings in a new lottery" copies everything but the dates (Figma F25).
    @Test
    fun reuse_copiesSettingsButNotDates() = runTest(dispatcher) {
        val vm = viewModel(reuseId = spring.id)
        val draft = vm.state.value.draft
        assertFalse(vm.state.value.isEditing)
        assertEquals("Spring opening", draft.name)
        assertEquals(listOf("hinton"), draft.campsiteIds)
        assertEquals(3, draft.numWinners)
        assertNull(draft.registrationStart)
        assertNull(draft.bookingEnd)
    }
}
