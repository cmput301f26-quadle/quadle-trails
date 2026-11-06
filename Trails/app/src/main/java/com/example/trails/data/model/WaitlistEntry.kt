// Domain model representing a user's entry on a lottery waitlist under lotteries/{id}/entries (Model in MVVM).
// implements US 01.01.06, US 01.02.01, US 01.02.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// represents a user's place on a lottery waitlist
/**
 * Represents a user's place on a lottery waitlist.
 *
 * @property userId the unique identifier of the entrant user, matching their device id.
 * @property userName display name of the entrant.
 * @property lotteryId the identifier of the lottery whose waitlist this entry belongs to.
 * @property partySize number of guests included in this entrant's reservation request.
 * @property joinedAt timestamp when the entrant joined the waitlist.
 * @property status current state of the entry (WAITING, WON, LOST, DECLINED, RESERVED).
 * @property rulesAcceptedAt timestamp when the entrant accepted campsite rules.
 * @property rulesAccepted whether the entrant has explicitly agreed to the campsite rules.
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
    /**
     * Validates required fields before persisting or updating this waitlist entry.
     *
     * @return a map of validation errors keyed by field name, empty if valid.
     */
    fun validate(): Map<String, String> = buildMap {
        if (userId.isBlank()) put("userId", "User ID is required")
        if (lotteryId.isBlank()) put("lotteryId", "Lottery ID is required")
        if (partySize < 1) put("partySize", "Party size must be at least 1")
        if (!rulesAccepted && rulesAcceptedAt == null) put("rules", "You must accept the campsite rules")
    }
}
