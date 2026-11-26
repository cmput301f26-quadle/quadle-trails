// Controller for the owner's reservations list (US 02.07.01, 02.07.02, 02.07.03).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Outstanding issues: guest names come from AppContainer.userRepository, which is still the
//  fake one, so real guests show as "Guest" until profiles are on Firestore.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Reservation
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.ReservationRepository
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Watches the owner's campsites, their reservations and guest profiles, and exposes the list
 * for the selected chip and campsite. Updates live as reservations change.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param ownerId the signed-in owner.
 * @param campsiteRepository where campsites are stored.
 * @param reservationRepository where reservations are stored.
 * @param userRepository where profiles are stored, for guest names.
 * @param today returns today's day key; replaceable in tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerReservationsViewModel(
    ownerId: String = CurrentUser.id(),
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    reservationRepository: ReservationRepository = AppContainer.reservationRepository,
    userRepository: UserRepository = AppContainer.userRepository,
    private val today: () -> Long = ::todayKey
) : ViewModel() {

    private val filter = MutableStateFlow(ReservationFilter.ACTIVE)
    private val selectedCampsite = MutableStateFlow<String?>(null)

    private val campsites = campsiteRepository.observeByOwner(ownerId)

    /** Every reservation for any of the owner's campsites, in any status. */
    private val reservations: Flow<List<Reservation>> = campsites.flatMapLatest { sites ->
        if (sites.isEmpty()) flowOf(emptyList())
        else combine(sites.map { site ->
            reservationRepository.observeForCampsite(site.id).catch { emit(emptyList()) }
        }) { perSite -> perSite.toList().flatten() }
    }

    private val users = userRepository.observeAll().catch { emit(emptyList()) }

    /** The screen state; starts loading. */
    val state: StateFlow<OwnerReservationsUiState> =
        combine(campsites, reservations, users, filter, selectedCampsite) { sites, all, people, chip, siteId ->
            val day = today()
            OwnerReservationsUiState(
                isLoading = false,
                campsites = sites,
                selectedCampsiteId = siteId,
                filter = chip,
                items = buildOwnerReservationItems(all, sites, people, chip, siteId, day),
                today = day
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwnerReservationsUiState())

    /** Selects a chip: Active, Past or Cancelled. */
    fun onFilterChange(value: ReservationFilter) {
        filter.value = value
    }

    /** Selects a campsite in the dropdown, or null for All campsites. */
    fun onCampsiteChange(campsiteId: String?) {
        selectedCampsite.value = campsiteId
    }
}
