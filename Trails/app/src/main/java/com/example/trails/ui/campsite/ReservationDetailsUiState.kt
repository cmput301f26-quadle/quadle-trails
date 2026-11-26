// UI state and logic for the owner's Reservation details screen and rejecting (US 02.08.01).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Plain Kotlin (no Compose), so it can be unit-tested.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.User
import java.util.Locale

/**
 * What Reservation details shows (Figma · US 02.08.01 · Reservation details).
 *
 * Written in assistance with Claude AI by Anthropic.
 */
sealed interface ReservationDetailsUiState {
    /** Still loading. */
    data object Loading : ReservationDetailsUiState

    /** The reservation doesn't exist (deleted, or a bad link). */
    data object NotFound : ReservationDetailsUiState

    /**
     * The reservation is ready to show.
     *
     * @property reservation the reservation.
     * @property campsite the reserved campsite, or null if it's gone.
     * @property guest the guest's profile, or null if it isn't found.
     * @property filter Active, Past or Cancelled, as on My Reservations.
     */
    data class Loaded(
        val reservation: Reservation,
        val campsite: Campsite?,
        val guest: User?,
        val filter: ReservationFilter
    ) : ReservationDetailsUiState {
        /** Only active stays that aren't over can be rejected; completed ones can't (Figma). */
        val canReject: Boolean get() = filter == ReservationFilter.ACTIVE
    }
}

/**
 * Progress of rejecting: idle, asking to confirm, saving, failed (with Try again), or done.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
sealed interface RejectState {
    /** Nothing in progress. */
    data object Idle : RejectState

    /** The "Reject reservation?" dialog is open. */
    data object Confirming : RejectState

    /** The rejection is being saved. */
    data object Rejecting : RejectState

    /** Saving failed; the reservation is unchanged and the error dialog offers Try again. */
    data object Failed : RejectState

    /**
     * Rejected. The screen goes back to My Reservations, which shows [summary] in a banner.
     *
     * @property summary e.g. "Priya Shah · Two Jack Lakeside · Jun 5 – 7, 2027".
     */
    data class Done(val summary: String) : RejectState
}

/**
 * The banner line after rejecting, as in Figma: "Priya Shah · Two Jack Lakeside · Jun 5 – 7, 2027".
 * Parts that are unknown are left out.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param guestName the guest's name, or null.
 * @param campsiteName the campsite's name, or null.
 * @param reservation the rejected reservation, for its dates.
 */
fun rejectionSummary(
    guestName: String?,
    campsiteName: String?,
    reservation: Reservation,
    locale: Locale = Locale.getDefault()
): String {
    val start = reservation.startDate?.toDayKey()
    val end = reservation.endDate?.toDayKey()
    val dates = if (start != null && end != null) formatStayDates(start, end, locale) else null
    return listOfNotNull(guestName?.takeIf(String::isNotBlank), campsiteName?.takeIf(String::isNotBlank), dates)
        .joinToString(" · ")
}

/**
 * The notification sent to the guest when the owner rejects their reservation.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property title shown in bold.
 * @property content the message.
 */
data class RejectionNotice(val title: String, val content: String)

/**
 * Builds the guest's notification, e.g. "Your reservation at Two Jack Lakeside for
 * Jun 5 – 7, 2027 was rejected by the campsite owner."
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun rejectionNotice(campsiteName: String?, reservation: Reservation, locale: Locale = Locale.getDefault()): RejectionNotice {
    val start = reservation.startDate?.toDayKey()
    val end = reservation.endDate?.toDayKey()
    val where = campsiteName?.takeIf(String::isNotBlank)?.let { " at $it" }.orEmpty()
    val whenText = if (start != null && end != null) " for ${formatStayDates(start, end, locale)}" else ""
    return RejectionNotice(
        title = "Reservation rejected",
        content = "Your reservation$where$whenText was rejected by the campsite owner."
    )
}
