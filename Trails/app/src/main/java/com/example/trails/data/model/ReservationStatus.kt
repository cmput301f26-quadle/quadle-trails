// Enum model representing the lifecycle status of a campsite reservation (Model in MVVM).
// implements US 01.04.01, US 01.04.02, US 01.04.03
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.model

/**
 * Lifecycle status of a confirmed campsite reservation.
 */
enum class ReservationStatus {
    /** The reservation is confirmed and active. */
    ACTIVE,
    /** The reservation was cancelled by the entrant or organizer. */
    CANCELLED,
    /** The reservation was rejected or revoked by an administrator or organizer. */
    REJECTED
}
