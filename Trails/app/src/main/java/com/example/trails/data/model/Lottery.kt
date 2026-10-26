// A booking lottery for one or more campsites, stored in the lotteries collection.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Lottery(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val campsiteIds: List<String> = emptyList(),
    val registrationStart: Timestamp? = null,
    val registrationEnd: Timestamp? = null,
    val bookingStart: Timestamp? = null,
    val bookingEnd: Timestamp? = null,
    val numWinners: Int = 1,
    val waitlistCapacity: Int? = null   // null = unlimited
)
