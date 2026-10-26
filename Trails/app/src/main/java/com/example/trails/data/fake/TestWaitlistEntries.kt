// Sample waitlist entries for the sample lottery.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.fake

import com.example.trails.data.model.WaitlistEntry

object TestWaitlistEntries {
    val all = listOf(
        WaitlistEntry(
            userId = TestUsers.ENTRANT_ID,
            userName = "Priya Sharma",
            lotteryId = TestLotteries.LOTTERY_ID,
            partySize = 3,
            joinedAt = daysFromNow(-2),
            rulesAcceptedAt = daysFromNow(-2)
        ),
        WaitlistEntry(
            userId = TestUsers.ADMIN_ID,
            userName = "Morgan Lee",
            lotteryId = TestLotteries.LOTTERY_ID,
            partySize = 2,
            joinedAt = daysFromNow(-1),
            rulesAcceptedAt = daysFromNow(-1)
        )
    )
}
