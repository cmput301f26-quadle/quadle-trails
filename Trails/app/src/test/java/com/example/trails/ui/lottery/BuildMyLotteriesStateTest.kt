// Unit tests for the Lotteries tab list logic and its ViewModel's live entrant counts.
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.fake.TestUsers
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
class BuildMyLotteriesStateTest {

    private val day = 86_400_000L
    private val now = 1_800_000_000_000L
    private val dispatcher = StandardTestDispatcher()

    // The three lotteries from Figma F06, plus a second scheduled one that opens sooner.
    private val mayLongWeekend = lottery("may", "May long weekend", opensIn = 12)
    private val juneWeekend = lottery("june", "June weekend", opensIn = 3)
    private val summerWeekends = lottery("summer", "Summer weekends", opensIn = -5)
    private val springOpening = lottery("spring", "Spring opening", opensIn = -60)

    private fun lottery(id: String, name: String, opensIn: Int) = Lottery(
        id = id, ownerId = TestUsers.OWNER_ID, name = name, campsiteIds = listOf("a", "b"),
        registrationStart = timestampOf(now + opensIn * day), registrationEnd = timestampOf(now + (opensIn + 7) * day), numWinners = 5
    )

    private val all = listOf(springOpening, mayLongWeekend, summerWeekends, juneWeekend)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // Scheduled first (soonest first), then open, then closed.
    @Test
    fun allChip_sortsScheduledThenOpenThenClosed() {
        val state = buildMyLotteriesState(all, mapOf("summer" to 24, "spring" to 31), LotteryFilter.ALL, now)
        assertEquals(listOf("june", "may", "summer", "spring"), state.items.map { it.lottery.id })
        assertEquals(LotteryStatus.DRAWN, state.items.last().status)
    }

    @Test
    fun doneChip_onlyClosedLotteries() {
        val state = buildMyLotteriesState(all, emptyMap(), LotteryFilter.DONE, now)
        assertEquals(listOf("spring"), state.items.map { it.lottery.id })
        assertEquals(LotteryStatus.NO_ENTRANTS, state.items.single().status)
    }

    // A chip with nothing under it is different from having no lotteries at all.
    @Test
    fun emptyChip_stillHasAny() {
        val state = buildMyLotteriesState(listOf(mayLongWeekend), emptyMap(), LotteryFilter.OPEN, now)
        assertTrue(state.items.isEmpty())
        assertTrue(state.hasAny)
        assertFalse(buildMyLotteriesState(emptyList(), emptyMap(), LotteryFilter.ALL, now).hasAny)
    }

    // "5 winners drawn from 31 entrants", but never more winners than entrants.
    @Test
    fun winnersDrawn_capsAtEntrants() {
        assertEquals(5, LotteryCardItem(springOpening, LotteryStatus.DRAWN, 31).winnersDrawn)
        assertEquals(2, LotteryCardItem(springOpening, LotteryStatus.DRAWN, 2).winnersDrawn)
    }

    // The ViewModel only lists the owner's lotteries and counts their waiting lists live.
    @Test
    fun viewModel_listsOwnersLotteriesWithEntrantCounts() = runTest(dispatcher) {
        val lotteries = FakeLotteryRepository(listOf(summerWeekends, mayLongWeekend.copy(ownerId = "someone-else")))
        val waitlist = FakeWaitlistRepository(List(24) { WaitlistEntry(userId = "u$it", lotteryId = "summer") })
        val vm = MyLotteriesViewModel(lotteries, waitlist, TestUsers.OWNER_ID, clock = { now })
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value as MyLotteriesUiState.Loaded
        assertEquals(listOf("summer"), state.items.map { it.lottery.id })
        assertEquals(24, state.items.single().entrantCount)

        vm.onFilterChange(LotteryFilter.SCHEDULED)
        advanceUntilIdle()
        assertTrue((vm.uiState.value as MyLotteriesUiState.Loaded).items.isEmpty())
    }
}
