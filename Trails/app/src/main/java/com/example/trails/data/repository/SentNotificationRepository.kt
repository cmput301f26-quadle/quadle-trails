// Read/write access to the notifications owners have sent to waitlists (US 02.10.01, 02.10.02).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help design the sent-notification repository.
package com.example.trails.data.repository

import com.example.trails.data.model.SentNotification
import kotlinx.coroutines.flow.Flow

/** Storage for [SentNotification]s. Screens depend on this, never on Firestore directly. */
interface SentNotificationRepository {

    /**
     * Observes everything one owner has sent.
     *
     * @param ownerId the owner's device id.
     * @return a flow that emits the owner's sent notifications, newest first, on every change.
     */
    fun observeByOwner(ownerId: String): Flow<List<SentNotification>>

    /**
     * Reads one sent notification.
     *
     * @param id its id.
     * @return the notification, or null if it does not exist.
     */
    suspend fun getById(id: String): SentNotification?

    /**
     * Stores a sent notification.
     *
     * @param notification the record; a blank id creates a new one.
     * @return the record's id.
     */
    suspend fun save(notification: SentNotification): String
}
