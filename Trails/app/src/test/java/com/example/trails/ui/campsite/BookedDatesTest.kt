// Unit tests for the Booked dates screen logic (US 02.06.01).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeReservationRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.dateOf
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.DateRange
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookedDatesTest {

    private val may = CalendarMonth(2027, 5)
    private val today = dayKey(2027, 5, 1)

    // May 2027 weekends, as in Figma: 10 open nights.
    private val weekends = listOf(1, 2, 8, 9, 15, 16, 22, 23, 29, 30).map { dayKey(2027, 5, it) }.toSet()
    private val alexStay = Reservation(
        id = "qr1042", userId = "alex", campsiteId = "ridge", partySize = 4, totalPrice = 130.0,
        startDate = dateOf(2027, 5, 15), endDate = dateOf(2027, 5, 16)
    )

    // Figma: "1 reservation · 2 of 10 open nights booked".
    @Test
    fun monthSummary_matchesFigma() {
        assertEquals(
            BookedMonthSummary(reservations = 1, bookedNights = 2, openNights = 10),
            monthSummary(may, weekends, bookedDayKeys(listOf(alexStay)), listOf(alexStay))
        )
    }

    // A month with no stays counts nothing, so the empty state shows.
    @Test
    fun monthSummary_otherMonth_noReservations() {
        assertEquals(0, monthSummary(CalendarMonth(2027, 7), weekends, bookedDayKeys(listOf(alexStay)), listOf(alexStay)).reservations)
    }

    // Tapping a booked night finds its reservation; an unbooked night finds nothing.
    @Test
    fun reservationOn_findsCoveringStay() {
        assertEquals(alexStay, reservationOn(dayKey(2027, 5, 16), listOf(alexStay)))
        assertNull(reservationOn(dayKey(2027, 5, 22), listOf(alexStay)))
    }

    // A cancelled stay doesn't count for a tapped night.
    @Test
    fun reservationOn_ignoresCancelled() {
        assertNull(reservationOn(dayKey(2027, 5, 15), listOf(alexStay.copy(status = ReservationStatus.CANCELLED))))
    }

    // Opens on the next booking; without bookings, the next open day; otherwise this month.
    @Test
    fun initialMonth_nextBookingThenOpenDayThenToday() {
        val june = dayKey(2027, 6, 5)
        assertEquals(CalendarMonth(2027, 6), initialBookedMonth(setOf(june), weekends, today))
        assertEquals(may, initialBookedMonth(emptySet(), weekends, today))
        assertEquals(may, initialBookedMonth(emptySet(), emptySet(), today))
    }

    // --- ViewModel ---

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // Opens on the campsite asked for, shows its booked nights, selects a booked night, and
    // starts over when another campsite is picked.
    @Test
    fun viewModel_flow() = runTest(dispatcher) {
        val ridge = Campsite(
            id = "ridge", ownerId = "owner", name = "Grande Cache Ridge",
            availability = listOf(DateRange(dateOf(2027, 5, 15), dateOf(2027, 5, 16)))
        )
        val lake = Campsite(id = "lake", ownerId = "owner", name = "Two Jack Lakeside")
        val vm = BookedDatesViewModel(
            initialCampsiteId = "ridge",
            ownerId = "owner",
            campsiteRepository = FakeCampsiteRepository(listOf(ridge, lake)),
            reservationRepository = FakeReservationRepository(listOf(alexStay)),
            userRepository = FakeUserRepository(listOf(User(deviceId = "alex", name = "Alex Morgan"))),
            today = { today }
        )
        val job = launch { vm.state.collect {} }
        advanceUntilIdle()
        assertEquals("ridge", vm.state.value.campsite?.id)
        assertEquals(may, vm.state.value.month)
        assertEquals(setOf(dayKey(2027, 5, 15), dayKey(2027, 5, 16)), vm.state.value.booked)

        vm.onDayClick(dayKey(2027, 5, 15))
        advanceUntilIdle()
        assertEquals(alexStay, vm.state.value.selectedReservation)
        assertEquals("Alex Morgan", vm.state.value.guestNames["alex"])

        vm.onMonthChange(1)
        advanceUntilIdle()
        assertEquals(CalendarMonth(2027, 6), vm.state.value.month)
        assertNull(vm.state.value.selectedDay)

        vm.onCampsiteChange("lake")
        advanceUntilIdle()
        assertEquals("lake", vm.state.value.campsite?.id)
        assertEquals(emptySet<Long>(), vm.state.value.booked)
        job.cancel()
    }
}
