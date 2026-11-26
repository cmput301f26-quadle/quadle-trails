// Controller for the owner's Reservation details screen and rejecting (US 02.08.01).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Outstanding issues: guest names come from AppContainer.userRepository, which is still the
//  fake one, so real guests show as "Guest" until profiles are on Firestore.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.NotificationRepository
import com.example.trails.data.repository.ReservationRepository
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads one reservation with its campsite and guest, and rejects it: confirm, save the
 * REJECTED status, then notify the guest (unless they opted out of notifications).
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param reservationId the reservation to show.
 * @param reservationRepository where reservations are stored.
 * @param campsiteRepository where campsites are stored.
 * @param userRepository where profiles are stored, for the guest.
 * @param notificationRepository sends the guest's "Reservation rejected" notification.
 * @param today returns today's day key; replaceable in tests.
 */
class ReservationDetailsViewModel(
    private val reservationId: String,
    private val reservationRepository: ReservationRepository = AppContainer.reservationRepository,
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val userRepository: UserRepository = AppContainer.userRepository,
    private val notificationRepository: NotificationRepository = AppContainer.notificationRepository,
    private val today: () -> Long = ::todayKey
) : ViewModel() {

    private val _state = MutableStateFlow<ReservationDetailsUiState>(ReservationDetailsUiState.Loading)

    /** What the screen shows. */
    val state: StateFlow<ReservationDetailsUiState> = _state.asStateFlow()

    private val _reject = MutableStateFlow<RejectState>(RejectState.Idle)

    /** Progress of rejecting. */
    val rejectState: StateFlow<RejectState> = _reject.asStateFlow()

    init {
        load()
    }

    /** (Re)loads the reservation, its campsite and its guest. */
    fun load() {
        viewModelScope.launch {
            _state.value = try {
                val r = reservationRepository.getById(reservationId)
                if (r == null) ReservationDetailsUiState.NotFound
                else ReservationDetailsUiState.Loaded(
                    reservation = r,
                    campsite = quietly { campsiteRepository.getById(r.campsiteId) },
                    guest = quietly { userRepository.getById(r.userId) },
                    filter = reservationFilterOf(r, today())
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReservationDetailsUiState.NotFound
            }
        }
    }

    /** Opens the "Reject reservation?" dialog, if this reservation can be rejected. */
    fun onRejectClick() {
        val s = _state.value
        if (s is ReservationDetailsUiState.Loaded && s.canReject && _reject.value == RejectState.Idle) {
            _reject.value = RejectState.Confirming
        }
    }

    /** Closes the confirm or error dialog without changing anything. */
    fun onDismissDialog() {
        if (_reject.value == RejectState.Confirming || _reject.value == RejectState.Failed) {
            _reject.value = RejectState.Idle
        }
    }

    /**
     * Rejects the reservation ("Reject" in the dialog, or "Try again" after a failure). The
     * guest is notified only once the status is saved; a failed notification doesn't undo it.
     */
    fun onConfirmReject() {
        val s = _state.value as? ReservationDetailsUiState.Loaded ?: return
        if (_reject.value != RejectState.Confirming && _reject.value != RejectState.Failed) return
        _reject.value = RejectState.Rejecting
        viewModelScope.launch {
            _reject.value = try {
                reservationRepository.updateStatus(s.reservation.id, ReservationStatus.REJECTED)
                notifyGuest(s)
                RejectState.Done(rejectionSummary(s.guest?.name, s.campsite?.name, s.reservation))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                RejectState.Failed
            }
        }
    }

    /** Sends "Reservation rejected" to the guest, unless they opted out (US 01.10.01). */
    private fun notifyGuest(s: ReservationDetailsUiState.Loaded) {
        if (s.guest?.notificationsOptOut == true) return
        val notice = rejectionNotice(s.campsite?.name, s.reservation)
        try {
            notificationRepository.sendNotif(
                userId = s.reservation.userId,
                title = notice.title,
                content = notice.content,
                type = NotificationType.RESERVE_CANCEL,
                related = s.reservation.copy(status = ReservationStatus.REJECTED)
            )
        } catch (e: Exception) {
            // The rejection is saved; a missed notification shouldn't report it as failed.
        }
    }

    /** Runs [block], returning null instead of failing. */
    private suspend fun <T> quietly(block: suspend () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
