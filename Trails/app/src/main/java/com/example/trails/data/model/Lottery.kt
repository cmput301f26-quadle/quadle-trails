// A booking lottery for one or more campsites, stored in the lotteries collection.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * A lottery an owner runs to decide who gets to book one or more campsites.
 *
 * @property id Firestore document id.
 * @property ownerId Device id of the owner running the lottery.
 * @property name Display name of the lottery.
 * @property campsiteIds Ids of the campsites the lottery covers.
 * @property registrationStart When entrants can start joining the waitlist.
 * @property registrationEnd When the waitlist closes and the draw can run.
 * @property bookingStart First day of the stay being booked.
 * @property bookingEnd Last day of the stay being booked.
 * @property numWinners How many entrants are drawn.
 * @property waitlistCapacity Maximum waitlist size, or null for unlimited.
 */
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
