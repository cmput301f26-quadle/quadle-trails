// In-memory SentNotificationRepository for unit tests and previews (US 02.10.01, 02.10.02).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write the in-memory sent-notification store.
package com.example.trails.data.fake

import com.example.trails.data.model.SentNotification
import com.example.trails.data.repository.SentNotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Keeps sent notifications in memory.
 *
 * @param initial the records to start with.
 */
class FakeSentNotificationRepository(
    initial: List<SentNotification> = emptyList()
) : SentNotificationRepository {

    private val sent = MutableStateFlow(initial)
    private var nextId = 1

    /** When set, every call throws it, to test error states. */
    var failure: Exception? = null

    override fun observeByOwner(ownerId: String): Flow<List<SentNotification>> =
        sent.map { list ->
            failure?.let { throw it }
            list.filter { it.ownerId == ownerId }.sortedByDescending { it.sentAt?.toDate()?.time ?: 0L }
        }

    override suspend fun getById(id: String): SentNotification? {
        failure?.let { throw it }
        return sent.value.find { it.id == id }
    }

    override suspend fun save(notification: SentNotification): String {
        failure?.let { throw it }
        val id = notification.id.ifBlank { "sent-${nextId++}" }
        sent.update { list -> list.filterNot { it.id == id } + notification.copy(id = id) }
        return id
    }
}
