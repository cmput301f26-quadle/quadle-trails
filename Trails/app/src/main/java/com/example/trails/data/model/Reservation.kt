// Data model representing a confirmed campsite reservation from winning a lottery or direct booking (Model in MVVM).
// implements US 01.04.01, US 01.04.02, US 01.04.03
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Confirmed booking for a campsite stored in the reservations collection.
 *
 * @property id firestore document id
 * @property userId device id of entrant holding the reservation
 * @property campsiteId id of campsite reserved
 * @property lotteryId id of lottery won or null if booked directly
 * @property partySize number of guests
 * @property startDate check-in date timestamp
 * @property endDate check-out date timestamp
 * @property totalPrice total price in dollars
 * @property rulesAccepted true if entrant agreed to campsite rules
 * @property rulesAcceptedAt timestamp when entrant accepted rules
 * @property createdAt timestamp when reservation was confirmed
 * @property status current lifecycle status of the reservation (ACTIVE, CANCELLED, REJECTED)
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
    val createdAt: Timestamp? = null,
    val status: ReservationStatus = ReservationStatus.ACTIVE
) {
    // checks required fields to confirm reservation
    // returns field error messages
    /**
     * Validates required fields before persisting or confirming a reservation.
     *
     * @return a map of property names to error message strings, empty if valid.
     */
    fun validate(): Map<String, String> = buildMap {
        if (userId.isBlank()) put("userId", "User ID is required")
        if (campsiteId.isBlank()) put("campsiteId", "Campsite ID is required")
        if (partySize < 1) put("partySize", "Party size must be at least 1")
        if (totalPrice < 0.0) put("totalPrice", "Total price cannot be negative")
        if (!rulesAccepted && rulesAcceptedAt == null) put("rules", "Campsite rules must be accepted")
    }
}
