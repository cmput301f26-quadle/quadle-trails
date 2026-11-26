// Unit tests for the owner's reservations list (US 02.07.01, 02.07.02, 02.07.03).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeReservationRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.dateOf
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.model.User
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerReservationsTest {

    private val today = dayKey(2027, 5, 1)
    private val lake = Campsite(id = "lake", ownerId = "owner", name = "Two Jack Lakeside")
    private val ridge = Campsite(id = "ridge", ownerId = "owner", name = "Grande Cache Ridge")
    private val notMine = Campsite(id = "other", ownerId = "someone-else", name = "Not mine")
    private val alex = User(deviceId = "alex", name = "Alex Morgan")

    private fun res(
        id: String, site: String, from: Int, to: Int, month: Int = 5,
        status: ReservationStatus = ReservationStatus.ACTIVE, user: String = "alex"
    ) = Reservation(
        id = id, userId = user, campsiteId = site,
        startDate = dateOf(2027, month, from), endDate = dateOf(2027, month, to), status = status
    )

    private val upcomingLate = res("a2", "lake", 5, 7, month = 6)
    private val upcomingSoon = res("a1", "ridge", 15, 17)
    private val today1Night = res("a3", "ridge", 1, 1)
    private val finished = res("p1", "ridge", 16, 18, month = 4)
    private val cancelled = res("c1", "lake", 22, 24, status = ReservationStatus.CANCELLED)
    private val rejected = res("c2", "lake", 12, 14, month = 6, status = ReservationStatus.REJECTED)
    private val all = listOf(upcomingLate, upcomingSoon, today1Night, finished, cancelled, rejected)

    private fun items(filter: ReservationFilter, campsiteId: String? = null, reservations: List<Reservation> = all) =
        buildOwnerReservationItems(reservations, listOf(lake, ridge), listOf(alex), filter, campsiteId, today)

    // --- which chip a reservation is under ---

    @Test
    fun filterOf_activeNotOver_isActive() {
        assertEquals(ReservationFilter.ACTIVE, reservationFilterOf(upcomingSoon, today))
    }

    // A stay ending today is still active.
    @Test
    fun filterOf_endsToday_isActive() {
        assertEquals(ReservationFilter.ACTIVE, reservationFilterOf(today1Night, today))
    }

    @Test
    fun filterOf_activeAlreadyOver_isPast() {
        assertEquals(ReservationFilter.PAST, reservationFilterOf(finished, today))
    }

    @Test
    fun filterOf_cancelledOrRejected_isCancelled() {
        assertEquals(ReservationFilter.CANCELLED, reservationFilterOf(cancelled, today))
        assertEquals(ReservationFilter.CANCELLED, reservationFilterOf(rejected, today))
    }

    @Test
    fun filterOf_noEndDate_staysActive() {
        assertEquals(ReservationFilter.ACTIVE, reservationFilterOf(Reservation(startDate = dateOf(2027, 4, 1)), today))
    }

    // --- building the list ---

    // US 02.07.01: active reservations, soonest first.
    @Test
    fun active_soonestFirst() {
        assertEquals(listOf("a3", "a1", "a2"), items(ReservationFilter.ACTIVE).map { it.reservation.id })
    }

    // US 02.07.02: past reservations only.
    @Test
    fun past_onlyFinished() {
        assertEquals(listOf("p1"), items(ReservationFilter.PAST).map { it.reservation.id })
    }

    // US 02.07.03: cancelled and rejected, most recent first.
    @Test
    fun cancelled_includesRejected_mostRecentFirst() {
        assertEquals(listOf("c2", "c1"), items(ReservationFilter.CANCELLED).map { it.reservation.id })
    }

    // Picking a campsite in the dropdown narrows the list.
    @Test
    fun campsiteFilter_onlyThatCampsite() {
        assertEquals(listOf("a2"), items(ReservationFilter.ACTIVE, campsiteId = "lake").map { it.reservation.id })
    }

    // Reservations at someone else's campsite are never shown.
    @Test
    fun otherOwnersCampsite_leftOut() {
        val foreign = res("x", notMine.id, 20, 21)
        assertTrue(items(ReservationFilter.ACTIVE, reservations = listOf(foreign)).isEmpty())
    }

    // Each card gets the guest's name and campsite; unknown guests get null.
    @Test
    fun items_joinGuestAndCampsite() {
        val known = items(ReservationFilter.ACTIVE, reservations = listOf(upcomingSoon)).single()
        assertEquals("Alex Morgan", known.guestName)
        assertEquals(ridge, known.campsite)
        val unknown = items(ReservationFilter.ACTIVE, reservations = listOf(res("u", "ridge", 20, 21, user = "nobody"))).single()
        assertNull(unknown.guestName)
    }

    // --- formatting, as in Figma ---

    @Test
    fun nights_endMinusStart() {
        assertEquals(2, nightsOf(upcomingSoon))
        assertNull(nightsOf(Reservation(startDate = dateOf(2027, 5, 1))))
    }

    @Test
    fun formatStayDates_sameMonth() {
        assertEquals("May 15 – 17, 2027", formatStayDates(dayKey(2027, 5, 15), dayKey(2027, 5, 17), Locale.US))
    }

    @Test
    fun formatStayDates_acrossMonths() {
        assertEquals("May 30 – Jun 2, 2027", formatStayDates(dayKey(2027, 5, 30), dayKey(2027, 6, 2), Locale.US))
    }

    @Test
    fun formatStayDates_acrossYears() {
        assertEquals("Dec 30, 2026 – Jan 2, 2027", formatStayDates(dayKey(2026, 12, 30), dayKey(2027, 1, 2), Locale.US))
    }

    @Test
    fun reference_shortUppercase() {
        assertEquals("AB12CD", reservationReference("ab12cdEF99"))
    }

    // --- ViewModel ---

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // The ViewModel lists reservations across all the owner's campsites, and the chip and
    // dropdown change what's shown.
    @Test
    fun viewModel_listsAcrossCampsites_andFilters() = runTest(dispatcher) {
        val vm = OwnerReservationsViewModel(
            ownerId = "owner",
            campsiteRepository = FakeCampsiteRepository(listOf(lake, ridge, notMine)),
            reservationRepository = FakeReservationRepository(all + res("x", notMine.id, 20, 21)),
            userRepository = FakeUserRepository(listOf(alex)),
            today = { today }
        )
        val job = launch { vm.state.collect {} }
        advanceUntilIdle()
        assertEquals(listOf("a3", "a1", "a2"), vm.state.value.items.map { it.reservation.id })
        assertEquals(listOf(lake, ridge).map { it.id }.toSet(), vm.state.value.campsites.map { it.id }.toSet())

        vm.onFilterChange(ReservationFilter.CANCELLED)
        advanceUntilIdle()
        assertEquals(listOf("c2", "c1"), vm.state.value.items.map { it.reservation.id })

        vm.onFilterChange(ReservationFilter.ACTIVE)
        vm.onCampsiteChange("lake")
        advanceUntilIdle()
        assertEquals(listOf("a2"), vm.state.value.items.map { it.reservation.id })
        job.cancel()
    }
}
