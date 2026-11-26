// Firestore-backed SentNotificationRepository, in the sentNotifications collection (US 02.10.01, 02.10.02).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: Not unit-tested (needs a live Firestore); the logic is tested through the fake.
// AI assistance: Used AI to help write the Firestore queries for sent notifications.
package com.example.trails.data.firestore

import com.example.trails.data.model.SentNotification
import com.example.trails.data.repository.SentNotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Reads and writes [SentNotification]s in Firestore.
 *
 * @param db the Firestore instance.
 */
class FirestoreSentNotificationRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SentNotificationRepository {

    private val sent = db.collection(SENT_NOTIFICATIONS)

    override fun observeByOwner(ownerId: String): Flow<List<SentNotification>> = callbackFlow {
        // Sorted here rather than with orderBy, so no composite index is needed.
        val registration = sent.whereEqualTo("ownerId", ownerId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(SentNotification::class.java).orEmpty()
            trySend(list.sortedByDescending { it.sentAt?.toDate()?.time ?: 0L })
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getById(id: String): SentNotification? =
        sent.document(id).get().await().toObject(SentNotification::class.java)

    override suspend fun save(notification: SentNotification): String {
        val doc = if (notification.id.isBlank()) sent.document() else sent.document(notification.id)
        doc.set(notification).await()
        return doc.id
    }

    companion object {
        /** Name of the Firestore collection that holds owners' sent notifications. */
        const val SENT_NOTIFICATIONS = "sentNotifications"
    }
}
