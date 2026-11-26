package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/** An owner's consent to list their campsites for the lottery's fixed dates (US 02.04.08). */
data class LotteryInvitation(
    @DocumentId val id: String = "",
    val lotteryId: String = "",
    val lotteryName: String = "",
    val inviterId: String = "",
    val inviteeId: String = "",
    val campsiteIds: List<String> = emptyList(),
    val campsiteNames: List<String> = emptyList(),
    val registrationStart: Timestamp? = null,
    val registrationEnd: Timestamp? = null,
    val bookingStart: Timestamp? = null,
    val bookingEnd: Timestamp? = null,
    val status: String = "PENDING"
)
