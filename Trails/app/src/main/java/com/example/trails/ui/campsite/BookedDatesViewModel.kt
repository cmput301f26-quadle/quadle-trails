// Controller for the Booked dates screen (US 02.06.01).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Outstanding issues: guest names come from AppContainer.userRepository, which is still the
//  fake one, so real guests show as "Guest" until profiles are on Firestore.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.ReservationRepository
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Watches the owner's campsites, the chosen campsite's active reservations and guest names.
 * The owner picks a campsite, moves between months and taps a booked day to see its booking.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param initialCampsiteId the campsite to open on, or null for the owner's first campsite.
 * @param ownerId the signed-in owner.
 * @param campsiteRepository where campsites are stored.
 * @param reservationRepository where reservations are stored.
 * @param userRepository where profiles are stored, for guest names.
 * @param today returns today's day key; replaceable in tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookedDatesViewModel(
    initialCampsiteId: String? = null,
    ownerId: String = CurrentUser.id(),
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    reservationRepository: ReservationRepository = AppContainer.reservationRepository,
    userRepository: UserRepository = AppContainer.userRepository,
    private val today: () -> Long = ::todayKey
) : ViewModel() {

    private val pickedCampsite = MutableStateFlow(initialCampsiteId)
    private val pickedMonth = MutableStateFlow<CalendarMonth?>(null) // null: open on the next booking
    private val selectedDay = MutableStateFlow<Long?>(null)

    private val campsites = campsiteRepository.observeByOwner(ownerId)

    private val campsite = combine(campsites, pickedCampsite) { sites, id ->
        sites.find { it.id == id } ?: sites.firstOrNull()
    }

    private val reservations = campsite.map { it?.id }.distinctUntilChanged().flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else reservationRepository.observeForCampsite(id)
            .map { list -> list.filter { it.status == ReservationStatus.ACTIVE } }
            .catch { emit(emptyList()) }
    }

    private val guestNames = userRepository.observeAll()
        .map { users -> users.filter { it.name.isNotBlank() }.associate { it.deviceId to it.name } }
        .catch { emit(emptyMap()) }

    private val choices = combine(pickedMonth, selectedDay) { month, day -> month to day }

    /** The screen state; starts loading. */
    val state: StateFlow<BookedDatesUiState> =
        combine(campsites, campsite, reservations, guestNames, choices) { sites, site, active, names, (month, day) ->
            val available = site?.let { availabilityToDays(it.availability) }.orEmpty()
            val booked = bookedDayKeys(active, site?.id)
            BookedDatesUiState(
                isLoading = false,
                campsites = sites,
                campsite = site,
                month = month ?: initialBookedMonth(booked, available, today()),
                available = available,
                booked = booked,
                reservations = active,
                guestNames = names,
                selectedDay = day?.takeIf { it in booked }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookedDatesUiState())

    /** Shows another campsite, starting again from its next booking. */
    fun onCampsiteChange(id: String) {
        pickedCampsite.value = id
        pickedMonth.value = null
        selectedDay.value = null
    }

    /** Moves [months] months forward (negative for back) and clears the selection. */
    fun onMonthChange(months: Int) {
        pickedMonth.value = state.value.month.plus(months)
        selectedDay.value = null
    }

    /** Selects a booked day to show its reservation; tapping it again, or any other day, clears it. */
    fun onDayClick(day: Long) {
        selectedDay.value = if (day in state.value.booked && selectedDay.value != day) day else null
    }
}
