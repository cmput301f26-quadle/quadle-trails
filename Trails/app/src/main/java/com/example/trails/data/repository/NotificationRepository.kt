package com.example.trails.data.repository

import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation

/**
 * Temporary? Notification repository.
 */
interface NotificationRepository {
    /**
     * Gets all notifications for a specific user.
     *
     * @param userId ID of user to fetch notifications for.
     */
    fun getNotif(userId: String) : List<Notification>

    /**
     * Sends notification/adds notification document.
     *
     * @param userId ID of the user who is the recipient of this notification.
     * @param title Title of this notification.
     * @param content Content string of this notification.
     * @param type Type of this notification, defined as [NotificationType].
     * @param related The related reservation of this notification, if any.
     */
    fun sendNotif(userId: String, title: String, content: String, type: NotificationType, related: Reservation?)

    /**
     * Checks if any notification for a given reservation exists.
     * Used so duplicate notifications are not sent out.
     *
     * @param related Reservation entry to check for.
     */
    suspend fun checkNotif(related: Reservation) : Boolean

    /**
     * Checks if any notification for a cancellation of a reservation exists.
     * Used so duplicate cancel notifications are not sent out.
     *
     * @param related Reservation entry to check for.
     */
    suspend fun checkCancelledNotif(related: Reservation): Boolean
}