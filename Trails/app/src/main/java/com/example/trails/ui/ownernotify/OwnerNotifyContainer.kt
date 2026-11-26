// Holds the sent-notification repository the app uses (US 02.10.01, 02.10.02).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: Kept out of the shared AppContainer to avoid conflicts while other PRs edit it;
//  can be moved there later.
// AI assistance: Used AI to help keep the repository choice in one place.
package com.example.trails.ui.ownernotify

import com.example.trails.data.firestore.FirestoreSentNotificationRepository
import com.example.trails.data.repository.SentNotificationRepository

/** Decides which [SentNotificationRepository] the app uses: Firestore. */
object OwnerNotifyContainer {
    /** Sent notifications, stored in Firestore. Lazy, so unit tests that pass a fake never start Firebase. */
    val sentNotificationRepository: SentNotificationRepository by lazy { FirestoreSentNotificationRepository() }
}
