// a user's entry in a lottery waitlist stored under lotteries/{id}/entries
// owner: chitkarsh (chitkarshrathi) - waitlist
// ai helped scaffold this data class boilerplate
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// represents a user's place on a lottery waitlist
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
