// Repository interface defining reactive streams and operations for campsite reservations (Repository pattern).
// implements US 01.04.01, US 01.04.02, US 01.04.03, US 02.04.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.repository

import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Storage and reactive streaming contract for [Reservation] entities.
 *
 * Provides real-time flows for observing bookings by campsite or user,
 * creating new confirmed reservations, updating reservation statuses,
 * and deleting reservations.
 */
interface ReservationRepository {

    /**
     * Observes all reservations confirmed for a specific campsite in real time.
     *
     * Used by campsite availability calendars and search filtering to determine booked dates.
     *
     * @param campsiteId unique identifier of the campsite.
     * @return a [Flow] emitting the list of [Reservation] records associated with this campsite.
     */
    fun observeForCampsite(campsiteId: String): Flow<List<Reservation>>

    /**
     * Observes all reservations held by a specific user in real time.
     *
     * Used by entrant profile and trips screens to display booked campsites.
     *
     * @param userId device identifier of the user.
     * @return a [Flow] emitting the list of [Reservation] records held by this user.
     */
    fun observeForUser(userId: String): Flow<List<Reservation>>

    /**
     * Retrieves a single reservation by its unique identifier.
     *
     * @param id document identifier of the reservation.
     * @return the [Reservation] if found, or null otherwise.
     */
    suspend fun getById(id: String): Reservation?

    /**
     * Persists a reservation. If [Reservation.id] is blank, a new unique id is generated.
     *
     * @param reservation the reservation data to persist.
     * @return the document id of the saved reservation.
     */
    suspend fun save(reservation: Reservation): String

    /**
     * Updates the status of an existing reservation.
     *
     * @param id identifier of the reservation document.
     * @param status the new [ReservationStatus] to assign (e.g. CANCELLED, REJECTED).
     */
    suspend fun updateStatus(id: String, status: ReservationStatus)

    /**
     * Deletes a reservation record permanently.
     *
     * @param id identifier of the reservation document to remove.
     */
    suspend fun delete(id: String)
}
