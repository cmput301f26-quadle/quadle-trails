// In-memory NotificationRepository for unit tests and previews.
// OWNER: Aarav (aarav-kapoor-cs) — Notifications; first version by Kasey (kcstarlight7)
// Outstanding issues: None currently known.
// AI assistance: Used AI to help add observing, send-once and opt-out behaviour to the fake.
package com.example.trails.data.fake

import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import com.example.trails.data.repository.NotificationRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Keeps notifications in memory.
 *
 * @param initial the notifications to start with.
 * @param optedOut returns true for users who turned notifications off; they receive nothing.
 */
class FakeNotificationRepository(
    initial: List<Notification> = TestNotifications.getAll(),
    private val optedOut: (String) -> Boolean = { false }
) : NotificationRepository {
    private val entries = MutableStateFlow(initial)
    private val sentKeys = mutableSetOf<String>()

    /** When set, observing and send-once throw it, to test error states. */
    var failure: Exception? = null

    override fun observeForUser(userId: String): Flow<List<Notification>> =
        entries.map { list ->
            failure?.let { throw it }
            list.filter { it.recipient == userId }.sortedByDescending { it.timestamp }
        }

    override fun getNotif(userId: String): List<Notification> {
        return entries.value.filter { it.recipient == userId }.sortedBy { it.timestamp }
    }

    override fun sendNotif(userId: String, title: String, content: String, type: NotificationType, related: Reservation?) {
        if (optedOut(userId)) return
        entries.update { currentList ->
            currentList + Notification(recipient = userId, title = title, content = content, timestamp = Timestamp.now(), type = type, related = related)
        }
    }

    override suspend fun sendOnce(key: String, notification: Notification): Boolean {
        failure?.let { throw it }
        if (!sentKeys.add(key)) return false
        entries.update { it + notification }
        return true
    }

    override suspend fun checkNotif(related: Reservation): Boolean {
        return true
    }

    override suspend fun checkCancelledNotif(related: Reservation): Boolean {
        return true
    }
}
