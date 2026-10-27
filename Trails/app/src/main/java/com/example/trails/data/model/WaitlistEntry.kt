// A user's entry in a lottery waitlist, stored under lotteries/{id}/entries.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * A user's place on a lottery's waitlist.
 *
 * @property userId Device id of the entrant; also the document id, so one entry per user per lottery.
 * @property userName Entrant's display name at the time they joined.
 * @property lotteryId Id of the lottery joined.
 * @property partySize Number of people in the entrant's party.
 * @property joinedAt When the entrant joined the waitlist.
 * @property status Where the entry stands in the lottery.
 * @property rulesAcceptedAt When the entrant agreed to the campsite rules.
 */
data class WaitlistEntry(
    @DocumentId val userId: String = "",
    val userName: String = "",
    val lotteryId: String = "",
    val partySize: Int = 1,
    val joinedAt: Timestamp? = null,
    val status: WaitlistStatus = WaitlistStatus.WAITING,
    val rulesAcceptedAt: Timestamp? = null
)
