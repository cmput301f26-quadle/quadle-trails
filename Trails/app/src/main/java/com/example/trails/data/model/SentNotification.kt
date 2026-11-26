// A notification an owner sent to a lottery's waitlist, kept for the owner's history (US 02.10.01, 02.10.02).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help design the sent-notification record.
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * One person a notification was sent to.
 *
 * @property userId the entrant's device id.
 * @property name the entrant's name when it was sent.
 * @property partySize the entrant's party size on the waitlist.
 */
data class SentRecipient(
    val userId: String = "",
    val name: String = "",
    val partySize: Int = 1
)

/**
 * A notification an owner sent to the entrants on one lottery's waitlist. Each entrant
 * also gets their own [Notification]; this record is the owner's copy, for Sent notifications.
 *
 * @property id Firestore document id.
 * @property ownerId device id of the owner who sent it.
 * @property lotteryId the lottery whose waitlist was notified.
 * @property lotteryName the lottery's name when it was sent, e.g. "June 2027".
 * @property campsiteName the lottery's (first) campsite, e.g. "Grande Cache Ridge".
 * @property subject the notification's title.
 * @property message the notification's text.
 * @property recipients everyone it was sent to.
 * @property sentAt when it was sent.
 */
data class SentNotification(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val lotteryId: String = "",
    val lotteryName: String = "",
    val campsiteName: String = "",
    val subject: String = "",
    val message: String = "",
    val recipients: List<SentRecipient> = emptyList(),
    val sentAt: Timestamp? = null
)
