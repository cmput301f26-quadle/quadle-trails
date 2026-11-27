// Read/write access to notifications (US 01.03.01, 01.03.02, 01.05.01, 01.07.02, 01.10.01, 02.10.01).
// OWNER: Aarav (aarav-kapoor-cs) — Notifications; first version by Kasey (kcstarlight7)
// Outstanding issues: getNotif cannot return live Firestore data; screens should use observeForUser.
// AI assistance: Used AI to help design the observe and send-once operations.
package com.example.trails.data.repository

import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import kotlinx.coroutines.flow.Flow

/** Storage for [Notification]s. Screens depend on this, never on Firestore directly. */
interface NotificationRepository {

    /**
     * Observes one user's notifications.
     *
     * @param userId the recipient's device id.
     * @return a flow that emits the user's notifications, newest first, on every change.
     */
    fun observeForUser(userId: String): Flow<List<Notification>>

    /**
     * Gets the notifications already loaded for a user, without waiting.
     *
     * @param userId ID of user to fetch notifications for.
     */
    fun getNotif(userId: String): List<Notification>

    /**
     * Sends a notification, unless the recipient has opted out of notifications (US 01.10.01).
     *
     * @param userId ID of the user who is the recipient of this notification.
     * @param title Title of this notification.
     * @param content Content string of this notification.
     * @param type Type of this notification, defined as [NotificationType].
     * @param related The related reservation of this notification, if any.
     */
    fun sendNotif(userId: String, title: String, content: String, type: NotificationType, related: Reservation?)

    /**
     * Sends a notification at most once: if one was already sent under [key], nothing happens.
     * Used for lottery results, so reopening the app never repeats "You won" (US 01.03.01, 01.03.02).
     *
     * @param key a stable id for this event, e.g. "lottery-<lotteryId>-<userId>".
     * @param notification the notification to store.
     * @return true if it was stored now, false if it already existed.
     */
    suspend fun sendOnce(key: String, notification: Notification): Boolean

    /**
     * Checks if any notification for a given reservation exists.
     * Used so duplicate notifications are not sent out.
     *
     * @param related Reservation entry to check for.
     */
    suspend fun checkNotif(related: Reservation): Boolean

    /**
     * Checks if any notification for a cancellation of a reservation exists.
     * Used so duplicate cancel notifications are not sent out.
     *
     * @param related Reservation entry to check for.
     */
    suspend fun checkCancelledNotif(related: Reservation): Boolean
}
