// Sample lottery, open for registration, covering the Banff and Jasper sites.
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
package com.example.trails.data.fake

import com.example.trails.data.model.Lottery

object TestLotteries {
    const val LOTTERY_ID = "lottery-summer-rockies"

    val all = listOf(
        Lottery(
            id = LOTTERY_ID,
            ownerId = TestUsers.OWNER_ID,
            name = "Summer Rockies Long Weekend",
            campsiteIds = listOf(TestCampsites.BANFF_ID, TestCampsites.JASPER_ID),
            registrationStart = daysFromNow(-3),
            registrationEnd = daysFromNow(10),
            bookingStart = daysFromNow(30),
            bookingEnd = daysFromNow(33),
            numWinners = 2,
            waitlistCapacity = 50
        )
    )
}
