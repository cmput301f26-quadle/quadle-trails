package com.example.trails.data.fake

import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import com.example.trails.data.repository.NotificationRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeNotificationRepository(
    initial: List<Notification> = TestNotifications.getAll()
) : NotificationRepository {
    private val entries = MutableStateFlow(initial)

    override fun getNotif(userId: String) : List<Notification> {
        return entries.value.filter { it.recipient == userId }.sortedBy { it.timestamp }
    }

    override fun sendNotif(userId: String, title: String, content: String, type: NotificationType, related: Reservation?) {
        entries.update{ currentList ->
            currentList + Notification(recipient = userId, title = title, content = content, timestamp = Timestamp.now(), type = type, related = related)
        }
    }

    override suspend fun checkNotif(related: Reservation) : Boolean {
        return true
    }

    override suspend fun checkCancelledNotif(related: Reservation): Boolean {
        return true
    }
}