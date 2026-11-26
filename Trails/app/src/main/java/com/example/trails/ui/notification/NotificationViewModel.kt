package com.example.trails.ui.notification

import androidx.lifecycle.ViewModel
import com.example.trails.data.AppContainer
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import com.example.trails.data.repository.NotificationRepository

/**
 * Temporary? Notification view model.
 *
 * @param repository The notification repository.
 */
class NotificationViewModel(
    private val repository: NotificationRepository = AppContainer.notificationRepository,
) : ViewModel() {
    /**
     * Fetch all notifications of a given user.
     *
     * @param userId ID of the user to fetch notifications for.
     */
    fun fetchAll(userId: String) : List<Notification> {
        return repository.getNotif(userId)
    }

    /**
     * Sends a notification.
     *
     * @param userId ID of the recipient user.
     * @param title Title of the notification.
     * @param content Content of the notification.
     * @param type Type of the notification, possible types defined in [NotificationType]
     * @param related (Nullable) The reservation this notification is for, if any.
     */
    fun sendNotif(userId: String, title: String, content: String, type: NotificationType, related: Reservation?) {
        repository.sendNotif(userId, title, content, type, related)
    }

    init {

    }
}