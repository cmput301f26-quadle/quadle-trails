// Firestore-backed NotificationRepository for the notifications collection.
// OWNER: Aarav (aarav-kapoor-cs) — Notifications; first version by Kasey (kcstarlight7)
// Outstanding issues: Not unit-tested (needs a live Firestore); the logic is tested through the fake.
//  Notifications are shown in the app only; there are no system push notifications.
// AI assistance: Used AI to help write the live query, the send-once write and the opt-out check.
package com.example.trails.data.firestore

import com.example.trails.data.model.Collections
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import com.example.trails.data.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Does things as needed to the [Collections.NOTIFICATIONS] collection in Firestore.
 *
 * @param db the Firestore instance; defaults to the app's default instance.
 */
class FirestoreNotificationRepository(
    db: Lazy<FirebaseFirestore> = lazy { FirebaseFirestore.getInstance() }
) : NotificationRepository {

    private val firestore by db

    // The latest list per user, kept by observeForUser so getNotif can answer without waiting.
    private val latest = mutableMapOf<String, List<Notification>>()

    override fun observeForUser(userId: String): Flow<List<Notification>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = firestore.collection(Collections.NOTIFICATIONS)
            .whereEqualTo("recipient", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // A document that can't be read (e.g. an unknown type) is skipped, not fatal.
                val list = snapshot?.documents.orEmpty()
                    .mapNotNull { doc -> runCatching { doc.toObject(Notification::class.java) }.getOrNull() }
                    .sortedByDescending { it.timestamp }
                latest[userId] = list
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Gets the notifications last loaded by [observeForUser] for a user; empty before that.
     *
     * @param userId ID of user to fetch notifications for.
     */
    override fun getNotif(userId: String): List<Notification> = latest[userId].orEmpty()

    /**
     * Sends notification/adds notification document, unless the recipient opted out (US 01.10.01).
     *
     * @param userId ID of the user who is the recipient of this notification.
     * @param title Title of this notification.
     * @param content Content string of this notification.
     * @param type Type of this notification, defined as [NotificationType].
     * @param related The related reservation of this notification, if any.
     */
    override fun sendNotif(
        userId: String,
        title: String,
        content: String,
        type: NotificationType,
        related: Reservation?
    ) {
        val notification = Notification(
            recipient = userId,
            title = title,
            content = content,
            type = type,
            related = related
        )
        val add = { firestore.collection(Collections.NOTIFICATIONS).add(notification) }
        if (userId.isBlank()) {
            add()
            return
        }
        // If the opt-out setting can't be read, the notification is still sent.
        firestore.collection(Collections.USERS).document(userId).get()
            .addOnSuccessListener { user -> if (user.getBoolean("notificationsOptOut") != true) add() }
            .addOnFailureListener { add() }
    }

    override suspend fun sendOnce(key: String, notification: Notification): Boolean {
        val doc = firestore.collection(Collections.NOTIFICATIONS).document(key)
        if (doc.get().await().exists()) return false
        doc.set(notification).await()
        return true
    }

    /**
     * Checks if any notification for a given reservation exists.
     * Used so duplicate notifications are not sent out.
     *
     * @param related Reservation entry to check for.
     */
    override suspend fun checkNotif(
        related: Reservation
    ): Boolean {
        return firestore.collection(Collections.NOTIFICATIONS)
            .whereEqualTo("related", related)
            .limit(1)
            .get()
            .await().isEmpty
    }

    /**
     * Checks if any notification for a cancellation of a reservation exists.
     * Used so duplicate cancel notifications are not sent out.
     *
     * @param related Reservation entry to check for.
     */
    override suspend fun checkCancelledNotif(
        related: Reservation
    ): Boolean {
        return firestore.collection(Collections.NOTIFICATIONS)
            .whereEqualTo("related", related)
            .whereEqualTo("status", "CANCELLED")
            .limit(1)
            .get()
            .await().isEmpty
    }
}
