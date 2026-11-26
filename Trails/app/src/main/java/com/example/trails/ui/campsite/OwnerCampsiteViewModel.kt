// Controller for the owner's campsite page (US 02.01.04, 02.06.01).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.model.Reservation
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Watches one campsite's reservations, e.g. a ReservationRepository's observeForCampsite.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
typealias CampsiteReservations = (campsiteId: String) -> Flow<List<Reservation>>

/**
 * The app's source of a campsite's reservations: the reservation repository. If reading
 * fails, the calendar just shows no booked days instead of breaking the page.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
val appReservations: CampsiteReservations = { campsiteId ->
    AppContainer.reservationRepository.observeForCampsite(campsiteId).catch { emit(emptyList()) }
}

/**
 * Watches one campsite, the lotteries and the campsite's reservations, and exposes the page state.
 * Because it observes, the page updates by itself after the owner edits the campsite.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param campsiteId the campsite to show.
 * @param campsiteRepository where campsites are stored.
 * @param lotteryRepository where lotteries are stored.
 * @param reservations the campsite's reservations, shown as booked days (US 02.06.01).
 */
class OwnerCampsiteViewModel(
    campsiteId: String,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    reservations: CampsiteReservations = appReservations
) : ViewModel() {

    /** The page state; starts as Loading. */
    val uiState: StateFlow<OwnerCampsiteUiState> =
        combine(
            campsiteRepository.observeById(campsiteId),
            lotteryRepository.observeAll(),
            reservations(campsiteId),
            ::buildOwnerCampsiteState
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwnerCampsiteUiState.Loading)
}
