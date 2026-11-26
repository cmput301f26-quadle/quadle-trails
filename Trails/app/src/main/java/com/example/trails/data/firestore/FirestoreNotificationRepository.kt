package com.example.trails.data.firestore

import com.example.trails.data.model.Collections
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import com.example.trails.data.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Does things as needed to the [Collections.NOTIFICATIONS] collection in Firestore.
 *
 * @param db the Firestore instance; defaults to the app's default instance.
 */
class FirestoreNotificationRepository(
    db: Lazy<FirebaseFirestore> = lazy { FirebaseFirestore.getInstance()}
) : NotificationRepository {

    private val firestore by db

    /**
     * Gets all notifications for a specific user.
     *
     * @param userId ID of user to fetch notifications for.
     */
    override fun getNotif(userId: String): List<Notification> {
        var reserveList: List<Notification> = listOf()
        firestore
            .collection(Collections.NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                reserveList = documents.toObjects(Notification::class.java)
                return@addOnSuccessListener
            }
        return reserveList
    }

    /**
     * Sends notification/adds notification document.
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
        firestore.collection(Collections.NOTIFICATIONS).add(notification)
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