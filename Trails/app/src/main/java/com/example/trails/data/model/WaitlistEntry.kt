// a user's entry in a lottery waitlist stored under lotteries/{id}/entries
// owner: chitkarsh (chitkarshrathi) - waitlist
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * a user's place on a lottery waitlist
 *
 * @property userId device id of the entrant and document id
 * @property userName entrant display name when joining
 * @property lotteryId id of the lottery joined
 * @property partySize number of people in the entrant's party
 * @property joinedAt when the entrant joined the waitlist
 * @property status current lottery status
 * @property rulesAcceptedAt when rules were agreed to
 * @property rulesAccepted true if rules were agreed to
 */
data class WaitlistEntry(
    @DocumentId val userId: String = "",
    val userName: String = "",
    val lotteryId: String = "",
    val partySize: Int = 1,
    val joinedAt: Timestamp? = null,
    val status: WaitlistStatus = WaitlistStatus.WAITING,
    val rulesAcceptedAt: Timestamp? = null,
    val rulesAccepted: Boolean = rulesAcceptedAt != null
) {
    // checks required fields when joining or updating
    // returns map of error messages
    fun validate(): Map<String, String> = buildMap {
        if (userId.isBlank()) put("userId", "User ID is required")
        if (lotteryId.isBlank()) put("lotteryId", "Lottery ID is required")
        if (partySize < 1) put("partySize", "Party size must be at least 1")
        if (!rulesAccepted && rulesAcceptedAt == null) put("rules", "You must accept the campsite rules")
    }
}
