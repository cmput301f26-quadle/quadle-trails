// UI state and display logic for the owner's reservations list (US 02.07.01, 02.07.02, 02.07.03).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Plain Kotlin (no Compose), so it can be unit-tested.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.annotation.StringRes
import com.example.trails.R
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.model.User
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

/**
 * The three filter chips on My Reservations (Figma · Owner reservations).
 *
 * Written in assistance with Claude AI by Anthropic.
 */
enum class ReservationFilter(@StringRes val label: Int) {
    /** Active and not yet over (US 02.07.01). */
    ACTIVE(R.string.reservations_filter_active),

    /** Active but already over: completed stays (US 02.07.02). */
    PAST(R.string.reservations_filter_past),

    /** Cancelled by the guest or rejected by the owner (US 02.07.03). */
    CANCELLED(R.string.reservations_filter_cancelled)
}

/**
 * One reservation card.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property reservation the reservation.
 * @property guestName the guest's name, or null if their profile isn't found.
 * @property campsite the reserved campsite, or null if it's gone.
 * @property filter which chip it falls under.
 */
data class OwnerReservationItem(
    val reservation: Reservation,
    val guestName: String?,
    val campsite: Campsite?,
    val filter: ReservationFilter
)

/**
 * Everything My Reservations needs to draw.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property isLoading true until the first data arrives.
 * @property campsites the owner's campsites, for the Campsite dropdown.
 * @property selectedCampsiteId the campsite picked in the dropdown, or null for All campsites.
 * @property filter the selected chip.
 * @property items the cards to show, already filtered and sorted.
 * @property today today's day key, for "As of …".
 */
data class OwnerReservationsUiState(
    val isLoading: Boolean = true,
    val campsites: List<Campsite> = emptyList(),
    val selectedCampsiteId: String? = null,
    val filter: ReservationFilter = ReservationFilter.ACTIVE,
    val items: List<OwnerReservationItem> = emptyList(),
    val today: Long = 0L
)

/**
 * Which chip a reservation falls under: cancelled or rejected ones under Cancelled, active
 * ones under Past once their last day is before [today], otherwise Active. An active
 * reservation without an end date stays Active.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param today today's day key (see [todayKey]).
 */
fun reservationFilterOf(reservation: Reservation, today: Long): ReservationFilter = when {
    reservation.status != ReservationStatus.ACTIVE -> ReservationFilter.CANCELLED
    (reservation.endDate?.toDayKey() ?: Long.MAX_VALUE) < today -> ReservationFilter.PAST
    else -> ReservationFilter.ACTIVE
}

/**
 * The cards for one chip and campsite choice. Active ones are soonest first; past and
 * cancelled ones are most recent first. Reservations for campsites the owner doesn't host
 * are left out.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param reservations reservations for the owner's campsites, in any status.
 * @param campsites the owner's campsites.
 * @param users profiles, to show guest names.
 * @param filter the selected chip.
 * @param campsiteId the selected campsite, or null for all of them.
 * @param today today's day key.
 */
fun buildOwnerReservationItems(
    reservations: List<Reservation>,
    campsites: List<Campsite>,
    users: List<User>,
    filter: ReservationFilter,
    campsiteId: String?,
    today: Long
): List<OwnerReservationItem> {
    val sitesById = campsites.associateBy { it.id }
    val namesById = users.associate { it.deviceId to it.name.takeIf(String::isNotBlank) }
    val items = reservations
        .filter { it.campsiteId in sitesById && (campsiteId == null || it.campsiteId == campsiteId) }
        .map { OwnerReservationItem(it, namesById[it.userId], sitesById[it.campsiteId], reservationFilterOf(it, today)) }
        .filter { it.filter == filter }
    val byStart = compareBy<OwnerReservationItem> { it.reservation.startDate?.toDayKey() ?: Long.MAX_VALUE }
    return if (filter == ReservationFilter.ACTIVE) items.sortedWith(byStart) else items.sortedWith(byStart.reversed())
}

/**
 * Nights in a stay: check-out day minus check-in day, or null if a date is missing.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun nightsOf(reservation: Reservation): Int? {
    val start = reservation.startDate?.toDayKey() ?: return null
    val end = reservation.endDate?.toDayKey() ?: return null
    return (end - start).toInt().coerceAtLeast(0)
}

/**
 * A stay's dates as in Figma: "May 15 – 17, 2027", "May 30 – Jun 2, 2027", or
 * "Dec 30, 2026 – Jan 2, 2027" across years.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param start check-in day key.
 * @param end check-out day key.
 * @param locale the language for month names.
 */
fun formatStayDates(start: Long, end: Long, locale: Locale = Locale.getDefault()): String {
    val s = monthOf(start)
    val e = monthOf(end)
    fun fmt(pattern: String, key: Long): String {
        val m = monthOf(key)
        return SimpleDateFormat(pattern, locale).format(GregorianCalendar(m.year, m.month - 1, dayOfMonth(key)).time)
    }
    return when {
        start == end -> fmt("MMM d, yyyy", start)
        s.year != e.year -> "${fmt("MMM d, yyyy", start)} – ${fmt("MMM d, yyyy", end)}"
        s.month != e.month -> "${fmt("MMM d", start)} – ${fmt("MMM d, yyyy", end)}"
        else -> "${fmt("MMM d", start)} – ${fmt("d, yyyy", end)}"
    }
}

/**
 * Short reservation number shown on the card, e.g. "AB12CD", from the document id.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun reservationReference(id: String): String = id.take(6).uppercase()
