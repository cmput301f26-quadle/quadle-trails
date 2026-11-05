// A confirmed campsite reservation resulting from a won lottery or direct booking.
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * A confirmed booking for a campsite, stored in the reservations collection (US 02.11.01).
 *
 * @property id Firestore document id.
 * @property userId Device id of the entrant holding the reservation.
 * @property campsiteId Id of the campsite reserved.
 * @property lotteryId Id of the lottery won, or null if booked directly.
 * @property partySize Number of guests in the booking.
 * @property startDate Check-in date timestamp.
 * @property endDate Check-out date timestamp.
 * @property totalPrice Total booking price in dollars.
 * @property rulesAccepted True if the entrant agreed to the campsite rules.
 * @property rulesAcceptedAt When the entrant accepted the campsite rules.
 * @property createdAt When the reservation was confirmed.
 */
data class Reservation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val campsiteId: String = "",
    val lotteryId: String? = null,
    val partySize: Int = 1,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val totalPrice: Double = 0.0,
    val rulesAccepted: Boolean = false,
    val rulesAcceptedAt: Timestamp? = null,
    val createdAt: Timestamp? = null
) {
    /**
     * Checks the fields required to confirm a reservation.
     * Kept as a function rather than init checks because Firestore builds objects from defaults.
     *
     * @return error messages keyed by field name; empty if the reservation can be saved.
     */
    fun validate(): Map<String, String> = buildMap {
        if (userId.isBlank()) put("userId", "User ID is required")
        if (campsiteId.isBlank()) put("campsiteId", "Campsite ID is required")
        if (partySize < 1) put("partySize", "Party size must be at least 1")
        if (totalPrice < 0.0) put("totalPrice", "Total price cannot be negative")
        if (!rulesAccepted && rulesAcceptedAt == null) put("rules", "Campsite rules must be accepted")
    }
}
