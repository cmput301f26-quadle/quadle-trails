// Unit tests for Reservation details and rejecting a reservation (US 02.08.01).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeNotificationRepository
import com.example.trails.data.fake.FakeReservationRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.dateOf
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ReservationDetailsTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = dayKey(2027, 5, 1)
    private val lake = Campsite(id = "lake", ownerId = "owner", name = "Two Jack Lakeside")
    private val priya = User(deviceId = "priya", name = "Priya Shah")
    private val stay = Reservation(
        id = "qr1048", userId = "priya", campsiteId = "lake", partySize = 2, totalPrice = 110.0,
        startDate = dateOf(2027, 6, 5), endDate = dateOf(2027, 6, 7)
    )

    private lateinit var reservations: FakeReservationRepository
    private lateinit var notifications: FakeNotificationRepository

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        reservation: Reservation = stay,
        guest: User = priya
    ): ReservationDetailsViewModel {
        reservations = FakeReservationRepository(listOf(reservation))
        notifications = FakeNotificationRepository(emptyList())
        return ReservationDetailsViewModel(
            reservationId = reservation.id,
            reservationRepository = reservations,
            campsiteRepository = FakeCampsiteRepository(listOf(lake)),
            userRepository = FakeUserRepository(listOf(guest)),
            notificationRepository = notifications,
            today = { today }
        )
    }

    private suspend fun storedStatus() = reservations.getById(stay.id)?.status

    // --- text, as in Figma ---

    @Test
    fun summary_matchesFigmaBanner() {
        assertEquals("Priya Shah · Two Jack Lakeside · Jun 5 – 7, 2027", rejectionSummary("Priya Shah", "Two Jack Lakeside", stay, Locale.US))
    }

    @Test
    fun summary_leavesOutUnknownParts() {
        assertEquals("Jun 5 – 7, 2027", rejectionSummary(null, " ", stay, Locale.US))
    }

    @Test
    fun notice_namesCampsiteAndDates() {
        val n = rejectionNotice("Two Jack Lakeside", stay, Locale.US)
        assertEquals("Reservation rejected", n.title)
        assertEquals("Your reservation at Two Jack Lakeside for Jun 5 – 7, 2027 was rejected by the campsite owner.", n.content)
    }

    // --- loading ---

    @Test
    fun loads_reservationCampsiteAndGuest() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val s = vm.state.value as ReservationDetailsUiState.Loaded
        assertEquals(lake, s.campsite)
        assertEquals(priya, s.guest)
        assertTrue(s.canReject)
    }

    @Test
    fun missingReservation_notFound() = runTest(dispatcher) {
        reservations = FakeReservationRepository(emptyList())
        val vm = ReservationDetailsViewModel(
            "nope", reservations, FakeCampsiteRepository(emptyList()), FakeUserRepository(emptyList()),
            FakeNotificationRepository(emptyList()), { today }
        )
        advanceUntilIdle()
        assertEquals(ReservationDetailsUiState.NotFound, vm.state.value)
    }

    // --- rejecting ---

    // US 02.08.01: confirm → REJECTED, guest notified, banner text ready.
    @Test
    fun reject_confirmed_savesNotifiesAndFinishes() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onRejectClick()
        assertEquals(RejectState.Confirming, vm.rejectState.value)
        vm.onConfirmReject()
        advanceUntilIdle()
        assertEquals(ReservationStatus.REJECTED, storedStatus())
        val done = vm.rejectState.value as RejectState.Done
        assertTrue(done.summary.startsWith("Priya Shah · Two Jack Lakeside · "))
        val sent = notifications.getNotif("priya").single()
        assertEquals(NotificationType.RESERVE_CANCEL, sent.type)
        assertEquals("Reservation rejected", sent.title)
        assertEquals(ReservationStatus.REJECTED, sent.related?.status)
    }

    // Cancel in the dialog changes nothing.
    @Test
    fun reject_cancelled_changesNothing() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onRejectClick()
        vm.onDismissDialog()
        advanceUntilIdle()
        assertEquals(RejectState.Idle, vm.rejectState.value)
        assertEquals(ReservationStatus.ACTIVE, storedStatus())
        assertTrue(notifications.getNotif("priya").isEmpty())
    }

    // A failed save shows Try again; the reservation stays as it was and nobody is notified.
    @Test
    fun reject_fails_thenTryAgainSucceeds() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onRejectClick()
        reservations.failure = IOException("offline")
        vm.onConfirmReject()
        advanceUntilIdle()
        assertEquals(RejectState.Failed, vm.rejectState.value)
        assertTrue(notifications.getNotif("priya").isEmpty())

        reservations.failure = null
        vm.onConfirmReject() // Try again
        advanceUntilIdle()
        assertTrue(vm.rejectState.value is RejectState.Done)
        assertEquals(ReservationStatus.REJECTED, storedStatus())
    }

    // Completed stays can't be rejected (Figma).
    @Test
    fun completedStay_cantReject() = runTest(dispatcher) {
        val vm = viewModel(stay.copy(startDate = dateOf(2027, 4, 16), endDate = dateOf(2027, 4, 18)))
        advanceUntilIdle()
        val s = vm.state.value as ReservationDetailsUiState.Loaded
        assertFalse(s.canReject)
        assertEquals(ReservationFilter.PAST, s.filter)
        vm.onRejectClick()
        assertEquals(RejectState.Idle, vm.rejectState.value)
    }

    // An already-cancelled reservation can't be rejected again.
    @Test
    fun cancelled_cantReject() = runTest(dispatcher) {
        val vm = viewModel(stay.copy(status = ReservationStatus.CANCELLED))
        advanceUntilIdle()
        assertFalse((vm.state.value as ReservationDetailsUiState.Loaded).canReject)
    }

    // A guest who opted out of notifications (US 01.10.01) isn't notified, but the rejection still happens.
    @Test
    fun reject_guestOptedOut_noNotification() = runTest(dispatcher) {
        val vm = viewModel(guest = priya.copy(notificationsOptOut = true))
        advanceUntilIdle()
        vm.onRejectClick()
        vm.onConfirmReject()
        advanceUntilIdle()
        assertEquals(ReservationStatus.REJECTED, storedStatus())
        assertTrue(notifications.getNotif("priya").isEmpty())
    }
}
