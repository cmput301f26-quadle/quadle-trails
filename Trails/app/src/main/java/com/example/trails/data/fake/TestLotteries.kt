// Sample lottery, open for registration, covering the Banff and Jasper sites.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
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
