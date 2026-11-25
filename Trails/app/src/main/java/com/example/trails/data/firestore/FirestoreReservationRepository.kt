// Firestore implementation of ReservationRepository managing reservation documents in the reservations collection (Repository pattern).
// implements US 01.04.01, US 01.04.02, US 01.04.03, US 02.04.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.firestore

import android.util.Log
import com.example.trails.data.model.Collections
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.repository.ReservationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of [ReservationRepository] for persisting and querying bookings.
 *
 * Firebase is only touched on first use, so creating this class (e.g. as a default
 * argument in a ViewModel under a JVM unit test) does not start Firebase prematurely.
 *
 * @param db the Firestore instance; defaults to lazy app instance.
 */
class FirestoreReservationRepository(
    db: Lazy<FirebaseFirestore> = lazy { FirebaseFirestore.getInstance() }
) : ReservationRepository {

    private val reservations by lazy { db.value.collection(Collections.RESERVATIONS) }

    /**
     * Observes real-time reservation updates for a specified campsite.
     *
     * @param campsiteId unique identifier of the campsite.
     * @return a [Flow] emitting the list of [Reservation] records.
     */
    override fun observeForCampsite(campsiteId: String): Flow<List<Reservation>> {
        if (campsiteId.isBlank()) {
            return callbackFlow {
                trySend(emptyList())
                close()
            }
        }
        return observe(reservations.whereEqualTo("campsiteId", campsiteId))
    }

    /**
     * Observes real-time reservation updates for a specified user.
     *
     * @param userId device identifier of the user.
     * @return a [Flow] emitting the list of [Reservation] records.
     */
    override fun observeForUser(userId: String): Flow<List<Reservation>> {
        if (userId.isBlank()) {
            return callbackFlow {
                trySend(emptyList())
                close()
            }
        }
        return observe(reservations.whereEqualTo("userId", userId))
    }

    /**
     * Retrieves a single reservation document by id.
     *
     * @param id identifier of the reservation document.
     * @return the [Reservation] if found, or null otherwise.
     */
    override suspend fun getById(id: String): Reservation? = try {
        val snapshot = reservations.document(id).get().await()
        if (snapshot.exists()) {
            snapshot.toObject(Reservation::class.java)?.copy(id = snapshot.id)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.w(TAG, "Couldn't read reservation $id", e)
        null
    }

    /**
     * Saves or overwrites a reservation document in the reservations collection.
     *
     * @param reservation the reservation data to store.
     * @return the document id of the stored reservation.
     */
    override suspend fun save(reservation: Reservation): String {
        val doc = if (reservation.id.isBlank()) reservations.document() else reservations.document(reservation.id)
        val toSave = reservation.copy(id = doc.id)
        doc.set(toSave).await()
        return doc.id
    }

    /**
     * Updates the status string of an existing reservation document.
     *
     * @param id identifier of the reservation document.
     * @param status the new [ReservationStatus] to assign.
     */
    override suspend fun updateStatus(id: String, status: ReservationStatus) {
        reservations.document(id).update("status", status.name).await()
    }

    /**
     * Deletes a reservation document permanently from Firestore.
     *
     * @param id identifier of the reservation document to remove.
     */
    override suspend fun delete(id: String) {
        reservations.document(id).delete().await()
    }

    private fun observe(query: Query): Flow<List<Reservation>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Couldn't observe reservations", error)
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                doc.toObject(Reservation::class.java)?.copy(id = doc.id)
            }
            trySend(items)
        }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val TAG = "FirestoreReservationRepo"
    }
}
