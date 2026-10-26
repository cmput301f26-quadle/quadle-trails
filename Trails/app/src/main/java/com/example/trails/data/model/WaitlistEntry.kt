// A user's entry in a lottery waitlist, stored under lotteries/{id}/entries.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class WaitlistEntry(
    @DocumentId val userId: String = "",
    val userName: String = "",
    val lotteryId: String = "",
    val partySize: Int = 1,
    val joinedAt: Timestamp? = null,
    val status: String = "WAITING",
    val rulesAcceptedAt: Timestamp? = null
)
