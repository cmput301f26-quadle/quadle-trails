// UI state and logic for the Booked dates screen (US 02.06.01).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Plain Kotlin (no Compose), so it can be unit-tested.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus

/**
 * Everything the Booked dates screen needs to draw (Figma · US 02.06.01 · View booked dates).
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property isLoading true until the first data arrives.
 * @property campsites the owner's campsites, for the Campsite dropdown.
 * @property campsite the campsite shown, or null if the owner has none.
 * @property month the month shown.
 * @property available the campsite's available day keys.
 * @property booked day keys taken by active reservations.
 * @property reservations the campsite's active reservations.
 * @property guestNames device id to name, for the selected reservation.
 * @property selectedDay the booked day the owner tapped, or null.
 */
data class BookedDatesUiState(
    val isLoading: Boolean = true,
    val campsites: List<Campsite> = emptyList(),
    val campsite: Campsite? = null,
    val month: CalendarMonth = monthOf(todayKey()),
    val available: Set<Long> = emptySet(),
    val booked: Set<Long> = emptySet(),
    val reservations: List<Reservation> = emptyList(),
    val guestNames: Map<String, String> = emptyMap(),
    val selectedDay: Long? = null
) {
    /** Counts for the shown month. */
    val summary: BookedMonthSummary get() = monthSummary(month, available, booked, reservations)

    /** The reservation on [selectedDay], or null. */
    val selectedReservation: Reservation? get() = selectedDay?.let { reservationOn(it, reservations) }
}

/**
 * The shown month in numbers, for "1 reservation · 2 of 10 open nights booked".
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property reservations reservations with at least one night in the month.
 * @property bookedNights booked days in the month.
 * @property openNights days in the month the campsite is open (available or booked).
 */
data class BookedMonthSummary(val reservations: Int, val bookedNights: Int, val openNights: Int)

/**
 * Counts reservations and nights for [month].
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun monthSummary(
    month: CalendarMonth,
    available: Set<Long>,
    booked: Set<Long>,
    reservations: List<Reservation>
): BookedMonthSummary {
    val days = monthCells(month).filterNotNull().toSet()
    return BookedMonthSummary(
        reservations = reservations.count { r -> bookedDayKeys(listOf(r)).any { it in days } },
        bookedNights = days.count { it in booked },
        openNights = days.count { it in available || it in booked }
    )
}

/**
 * The active reservation covering [day], or null.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun reservationOn(day: Long, reservations: List<Reservation>): Reservation? =
    reservations.firstOrNull { it.status == ReservationStatus.ACTIVE && day in bookedDayKeys(listOf(it)) }

/**
 * The month to open on: the next booking from [today], else the next open day, else this month.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun initialBookedMonth(booked: Set<Long>, available: Set<Long>, today: Long): CalendarMonth =
    monthOf(booked.filter { it >= today }.minOrNull() ?: available.filter { it >= today }.minOrNull() ?: today)
