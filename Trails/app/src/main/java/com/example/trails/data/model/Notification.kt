// A notification shown in a user's Notifications tab, stored in the notifications collection
// (US 01.03.01, 01.03.02, 01.05.01, 01.07.02, 02.08.01, 02.10.01).
// OWNER: Aarav (aarav-kapoor-cs) — Notifications; first version by Kasey (kcstarlight7)
// Outstanding issues: None currently known.
// AI assistance: Used AI to help give every field a default so Firestore can read notifications back.
package com.example.trails.data.model

import com.google.firebase.Timestamp

/**
 * One notification for one user. Every field has a default, which Firestore needs to
 * turn a stored document back into this class.
 *
 * @property recipient device id of the user it is for.
 * @property title the headline.
 * @property content the body text; may be empty.
 * @property timestamp when it was sent.
 * @property type what kind of notification it is; decides which card is shown.
 * @property related the reservation it is about, or null.
 */
data class Notification(
    val recipient: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val type: NotificationType = NotificationType.OWNER_MESSAGE,
    val related: Reservation? = null
)
