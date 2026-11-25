// Unit test suite verifying ReserveDatesViewModel date availability, guest counts, and decline operations (MVVM pattern).
// implements US 01.04.01, US 01.04.02, US 01.04.03
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.ui.waitlist

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeReservationRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.DateRange
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.model.User
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.ui.campsite.dayKey
import com.example.trails.ui.campsite.dayKeyToTimestamp
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

/**
 * Unit test suite verifying [ReserveDatesViewModel] behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReserveDatesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val lotteryId = "lottery-draw-1"
    private val campsiteId = "campsite-1"
    private val userId = "user-123"

    private val day1 = dayKey(2027, 5, 1)
    private val day2 = dayKey(2027, 5, 2)
    private val day3 = dayKey(2027, 5, 3)
    private val day4 = dayKey(2027, 5, 4)

    private val sampleCampsite = Campsite(
        id = campsiteId,
        name = "Two Jack Lakeside Campground",
        locationName = "Banff, AB",
        maxOccupancy = 4,
        pricePerNight = 40.0,
        availability = listOf(DateRange(dayKeyToTimestamp(day1), dayKeyToTimestamp(day4)))
    )

    private val sampleLottery = Lottery(
        id = lotteryId,
        name = "May Long Weekend",
        campsiteIds = listOf(campsiteId),
        bookingStart = dayKeyToTimestamp(day1),
        bookingEnd = dayKeyToTimestamp(day4)
    )

    private val sampleUser = User(
        deviceId = userId,
        name = "Alex Morgan",
        email = "alex.morgan@email.com",
        phone = "780-555-0142"
    )

    private lateinit var lotteryRepo: FakeLotteryRepository
    private lateinit var campsiteRepo: FakeCampsiteRepository
    private lateinit var reservationRepo: FakeReservationRepository
    private lateinit var waitlistRepo: FakeWaitlistRepository
    private lateinit var userRepo: FakeUserRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        lotteryRepo = FakeLotteryRepository(initial = listOf(sampleLottery))
        campsiteRepo = FakeCampsiteRepository(initial = listOf(sampleCampsite))
        reservationRepo = FakeReservationRepository(initial = emptyList())
        waitlistRepo = FakeWaitlistRepository(
            initial = listOf(
                WaitlistEntry(
                    userId = userId,
                    lotteryId = lotteryId,
                    status = WaitlistStatus.WON,
                    rulesAcceptedAt = Timestamp.now()
                )
            )
        )
        userRepo = FakeUserRepository(initial = listOf(sampleUser))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ReserveDatesViewModel = ReserveDatesViewModel(
        lotteryId = lotteryId,
        campsiteId = campsiteId,
        userId = userId,
        lotteryRepository = lotteryRepo,
        campsiteRepository = campsiteRepo,
        reservationRepository = reservationRepo,
        waitlistRepository = waitlistRepo,
        userRepository = userRepo
    )

    @Test
    fun init_prefillsUserDetailsAndCalculatesAvailableDays() {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Alex Morgan", state.name)
        assertEquals("alex.morgan@email.com", state.email)
        assertEquals("780-555-0142", state.phone)
        assertEquals(2, state.guests)
        assertEquals(setOf(day1, day2, day3, day4), state.availableDays)
        assertEquals(setOf(day1, day2, day3, day4), state.openDays)
        assertTrue(state.bookedDays.isEmpty())
    }

    @Test
    fun observeReservations_updatesBookedDaysAndExcludesFromOpen() {
        val existingBooking = Reservation(
            id = "res-1",
            campsiteId = campsiteId,
            userId = "other-user",
            startDate = dayKeyToTimestamp(day2),
            endDate = dayKeyToTimestamp(day2),
            status = ReservationStatus.ACTIVE
        )
        reservationRepo = FakeReservationRepository(initial = listOf(existingBooking))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(day2 in state.bookedDays)
        assertFalse(day2 in state.openDays)
        assertEquals(setOf(day1, day3, day4), state.openDays)
    }

    @Test
    fun onDayTapped_selectsDayAndFormsRange() {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Tap day 1
        viewModel.onDayTapped(day1)
        assertEquals(setOf(day1), viewModel.uiState.value.selectedDays)

        // Tap day 3 (range 1..3)
        viewModel.onDayTapped(day3)
        assertEquals(setOf(day1, day2, day3), viewModel.uiState.value.selectedDays)
        assertTrue(viewModel.uiState.value.canProceed)
    }

    @Test
    fun onDayTapped_bookedDayIgnored() {
        val existingBooking = Reservation(
            id = "res-1",
            campsiteId = campsiteId,
            startDate = dayKeyToTimestamp(day2),
            endDate = dayKeyToTimestamp(day2),
            status = ReservationStatus.ACTIVE
        )
        reservationRepo = FakeReservationRepository(initial = listOf(existingBooking))
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDayTapped(day2) // booked day
        assertTrue(viewModel.uiState.value.selectedDays.isEmpty())
    }

    @Test
    fun guestCounter_incrementsAndDecrementsWithinBounds() {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.guests)
        viewModel.incrementGuests()
        assertEquals(3, viewModel.uiState.value.guests)
        viewModel.incrementGuests()
        assertEquals(4, viewModel.uiState.value.guests)
        viewModel.incrementGuests() // should clamp at maxOccupancy 4
        assertEquals(4, viewModel.uiState.value.guests)

        viewModel.decrementGuests()
        viewModel.decrementGuests()
        viewModel.decrementGuests()
        assertEquals(1, viewModel.uiState.value.guests)
        viewModel.decrementGuests() // should clamp at min 1
        assertEquals(1, viewModel.uiState.value.guests)
    }

    @Test
    fun declineLottery_updatesStatusToDeclined() {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        var declinedCallbackCalled = false
        viewModel.declineLottery { declinedCallbackCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(declinedCallbackCalled)
        assertTrue(viewModel.uiState.value.isDeclined)

        val entry = kotlinx.coroutines.runBlocking { waitlistRepo.getEntry(lotteryId, userId) }
        assertEquals(WaitlistStatus.DECLINED, entry?.status)
    }
}
