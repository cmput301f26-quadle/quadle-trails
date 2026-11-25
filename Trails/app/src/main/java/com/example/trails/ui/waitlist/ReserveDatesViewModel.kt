// ViewModel controller managing date selection, guest count, and lottery decline operations (MVVM pattern).
// implements US 01.04.01, US 01.04.02, US 01.04.03
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.ui.waitlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.ReservationRepository
import com.example.trails.data.repository.UserRepository
import com.example.trails.data.repository.WaitlistRepository
import com.example.trails.ui.campsite.availabilityToDays
import com.example.trails.ui.campsite.bookedDayKeys
import com.example.trails.ui.campsite.openDayKeys
import com.example.trails.ui.campsite.toDayKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the date reservation screen.
 *
 * @property isLoading whether initial data is being fetched.
 * @property lottery the winning lottery metadata.
 * @property campsite the campsite being booked.
 * @property availableDays day keys offering open booking windows.
 * @property bookedDays day keys locked by active reservations.
 * @property openDays day keys that are open and unreserved.
 * @property selectedDays day keys chosen by the entrant.
 * @property name entrant full name.
 * @property email entrant email address.
 * @property phone entrant contact phone number.
 * @property guests number of guests for the reservation.
 * @property errorMessage active error message, if any.
 * @property isDeclined true if the user confirmed declining the win.
 */
data class ReserveDatesUiState(
    val isLoading: Boolean = true,
    val lottery: Lottery? = null,
    val campsite: Campsite? = null,
    val availableDays: Set<Long> = emptySet(),
    val bookedDays: Set<Long> = emptySet(),
    val openDays: Set<Long> = emptySet(),
    val selectedDays: Set<Long> = emptySet(),
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val guests: Int = 2,
    val errorMessage: String? = null,
    val isDeclined: Boolean = false
) {
    /**
     * Whether the entrant has fulfilled all conditions to continue to confirmation.
     */
    val canProceed: Boolean
        get() = name.isNotBlank() &&
                email.isNotBlank() &&
                phone.isNotBlank() &&
                selectedDays.isNotEmpty() &&
                guests >= 1
}

/**
 * ViewModel orchestrating date availability checks, entrant details, and lottery declining.
 *
 * @param lotteryId identifier of the winning lottery.
 * @param campsiteId identifier of the campsite to reserve.
 * @param userId current user device identifier.
 * @param lotteryRepository repository for lottery lookup.
 * @param campsiteRepository repository for campsite lookup.
 * @param reservationRepository repository for existing reservation lookup.
 * @param waitlistRepository repository for updating entry statuses.
 * @param userRepository repository for retrieving entrant profile details.
 */
class ReserveDatesViewModel(
    private val lotteryId: String,
    private val campsiteId: String,
    private val userId: String = CurrentUser.id(),
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val reservationRepository: ReservationRepository = AppContainer.reservationRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val userRepository: UserRepository = AppContainer.userRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReserveDatesUiState())
    val uiState: StateFlow<ReserveDatesUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeReservations()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val lottery = lotteryRepository.getById(lotteryId)
                val campsite = campsiteRepository.getById(campsiteId)
                val user = userRepository.getById(userId)

                // calculate available days
                val campsiteDays = campsite?.let { availabilityToDays(it.availability) }.orEmpty()
                val lotteryDays = if (lottery?.bookingStart != null && lottery.bookingEnd != null) {
                    val startKey = lottery.bookingStart.toDayKey()
                    val endKey = lottery.bookingEnd.toDayKey()
                    if (startKey <= endKey) (startKey..endKey).toSet() else emptySet()
                } else {
                    emptySet()
                }

                val effectiveAvailable = when {
                    campsiteDays.isNotEmpty() && lotteryDays.isNotEmpty() -> campsiteDays.intersect(lotteryDays)
                    campsiteDays.isNotEmpty() -> campsiteDays
                    lotteryDays.isNotEmpty() -> lotteryDays
                    else -> emptySet()
                }

                val maxGuests = (campsite?.maxOccupancy ?: 6).coerceAtLeast(1)
                val initialGuests = 2.coerceAtMost(maxGuests).coerceAtLeast(1)

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        lottery = lottery,
                        campsite = campsite,
                        availableDays = effectiveAvailable,
                        name = current.name.ifBlank { user?.name.orEmpty() },
                        email = current.email.ifBlank { user?.email.orEmpty() },
                        phone = current.phone.ifBlank { user?.phone.orEmpty() },
                        guests = initialGuests
                    )
                }
                recalculateOpenDays()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load booking info.") }
            }
        }
    }

    private fun observeReservations() {
        viewModelScope.launch {
            reservationRepository.observeForCampsite(campsiteId)
                .catch { /* keep existing state on observe failure */ }
                .collect { reservations ->
                    val booked = bookedDayKeys(reservations, campsiteId)
                    _uiState.update { it.copy(bookedDays = booked) }
                    recalculateOpenDays()
                }
        }
    }

    private fun recalculateOpenDays() {
        _uiState.update { current ->
            val open = openDayKeys(current.availableDays, current.bookedDays)
            // filter out selected days that might have become booked
            val validatedSelected = current.selectedDays.filter { it in open }.toSet()
            current.copy(openDays = open, selectedDays = validatedSelected)
        }
    }

    /**
     * Updates the entrant contact name.
     *
     * @param name full name.
     */
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    /**
     * Updates the entrant contact email.
     *
     * @param email email address.
     */
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    /**
     * Updates the entrant contact phone number.
     *
     * @param phone phone number.
     */
    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    /**
     * Increments the guest count up to the campsite maximum occupancy.
     */
    fun incrementGuests() {
        val maxGuests = _uiState.value.campsite?.maxOccupancy ?: 8
        _uiState.update { current ->
            if (current.guests < maxGuests) current.copy(guests = current.guests + 1) else current
        }
    }

    /**
     * Decrements the guest count down to a minimum of 1.
     */
    fun decrementGuests() {
        _uiState.update { current ->
            if (current.guests > 1) current.copy(guests = current.guests - 1) else current
        }
    }

    /**
     * Toggles or selects a date in the open availability calendar.
     *
     * @param dayKey the day key clicked.
     */
    fun onDayTapped(dayKey: Long) {
        val open = _uiState.value.openDays
        if (dayKey !in open) return

        _uiState.update { current ->
            val selected = current.selectedDays
            val nextSelected = when {
                selected.isEmpty() -> setOf(dayKey)
                selected.size == 1 -> {
                    val start = selected.first()
                    if (dayKey > start) {
                        val range = (start..dayKey).toSet()
                        // only form range if all intervening days are open
                        if (range.all { it in open }) range else setOf(dayKey)
                    } else if (dayKey == start) {
                        emptySet()
                    } else {
                        setOf(dayKey)
                    }
                }
                else -> setOf(dayKey)
            }
            current.copy(selectedDays = nextSelected)
        }
    }

    /**
     * Declines the won lottery invitation, setting entry status to DECLINED.
     *
     * @param onSuccess optional callback invoked upon successful decline.
     */
    fun declineLottery(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                waitlistRepository.updateStatus(lotteryId, userId, WaitlistStatus.DECLINED)
                _uiState.update { it.copy(isDeclined = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to decline lottery.") }
            }
        }
    }
}
