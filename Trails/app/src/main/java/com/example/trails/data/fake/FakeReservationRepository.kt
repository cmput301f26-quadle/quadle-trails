// In-memory test fake implementation of ReservationRepository for hermetic testing and previews (Repository pattern).
// implements US 01.04.01, US 01.04.02, US 01.04.03, US 02.04.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.fake

import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.repository.ReservationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

// hey team (ibrahim, neaz, zainab, tina, kasey):
// here is how to use this fake repo in your tests and compose previews:
//
// 1. how to inject it:
//    pass FakeReservationRepository() directly into your viewmodel constructor or test harness.
//
// 2. ibrahim (availability calendar):
//    call repo.observeForCampsite(campsiteId) to get all bookings for a site.
//    each reservation has startDate and endDate timestamps. when status == ACTIVE,
//    those days are officially booked so you can mark them red on the calendar.
//
// 3. neaz (search & date filters):
//    use observeForCampsite to check whether a campsite has conflicting active bookings
//    matching the arrival and departure dates chosen in the search calendar filter.
//
// 4. zainab, tina & kasey (profile & admin):
//    use repo.observeForUser(userId) to display an entrant's booked campsites on their profile.
//    call repo.updateStatus(id, ReservationStatus.CANCELLED) to test cancellation flows.
//
// 5. testing error edge cases:
//    set repo.failure = IllegalStateException("firestore error") to trigger your screen's error banner.
//
// 6. seeded sample data:
//    pre-populated by default with TestReservations.all (active bookings for banff and jasper).

/**
 * In-memory implementation of [ReservationRepository] for fast local unit testing and Compose previews.
 *
 * @param initial list of reservations used to initialize the in-memory state. Defaults to [TestReservations.all].
 */
class FakeReservationRepository(
    initial: List<Reservation> = TestReservations.all
) : ReservationRepository {

    private val reservations = MutableStateFlow(initial)

    // optional exception to test error handling in viewmodels
    var failure: Exception? = null

    private fun checkFailure() {
        failure?.let { throw it }
    }

    /**
     * Observes all reservations for a given campsite from in-memory state.
     *
     * @param campsiteId unique identifier of the campsite.
     * @return a [Flow] emitting the matching list of reservations.
     */
    override fun observeForCampsite(campsiteId: String): Flow<List<Reservation>> {
        checkFailure()
        return reservations.map { list ->
            list.filter { it.campsiteId == campsiteId }
        }
    }

    /**
     * Observes all reservations held by a specific user from in-memory state.
     *
     * @param userId device identifier of the user.
     * @return a [Flow] emitting the user's reservations.
     */
    override fun observeForUser(userId: String): Flow<List<Reservation>> {
        checkFailure()
        return reservations.map { list ->
            list.filter { it.userId == userId }
        }
    }

    /**
     * Retrieves a single reservation by id from the in-memory list.
     *
     * @param id identifier of the reservation.
     * @return the [Reservation] if found, or null otherwise.
     */
    override suspend fun getById(id: String): Reservation? {
        checkFailure()
        return reservations.value.find { it.id == id }
    }

    /**
     * Saves or updates a reservation in memory.
     *
     * @param reservation the reservation to store.
     * @return the generated or existing reservation id.
     */
    override suspend fun save(reservation: Reservation): String {
        checkFailure()
        val assignedId = reservation.id.ifBlank { "fake-res-${UUID.randomUUID()}" }
        val toStore = reservation.copy(id = assignedId)
        reservations.update { list ->
            val index = list.indexOfFirst { it.id == assignedId }
            if (index >= 0) {
                list.toMutableList().apply { set(index, toStore) }
            } else {
                list + toStore
            }
        }
        return assignedId
    }

    /**
     * Updates the status of an existing reservation in memory.
     *
     * @param id identifier of the reservation.
     * @param status the new [ReservationStatus] value.
     */
    override suspend fun updateStatus(id: String, status: ReservationStatus) {
        checkFailure()
        reservations.update { list ->
            list.map { res ->
                if (res.id == id) res.copy(status = status) else res
            }
        }
    }

    /**
     * Deletes a reservation from the in-memory state.
     *
     * @param id identifier of the reservation to remove.
     */
    override suspend fun delete(id: String) {
        checkFailure()
        reservations.update { list ->
            list.filterNot { it.id == id }
        }
    }
}
