// sample waitlist entries for the sample lottery
// owner: chitkarsh (chitkarshrathi) - waitlist
// ai helped generate these sample seed entries
package com.example.trails.data.fake

import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus

object TestWaitlistEntries {
    val all = listOf(
        WaitlistEntry(
            userId = TestUsers.ENTRANT_ID,
            userName = "Priya Sharma",
            lotteryId = TestLotteries.LOTTERY_ID,
            partySize = 3,
            joinedAt = daysFromNow(-2),
            status = WaitlistStatus.WAITING,
            rulesAcceptedAt = daysFromNow(-2)
        ),
        WaitlistEntry(
            userId = TestUsers.ADMIN_ID,
            userName = "Morgan Lee",
            lotteryId = TestLotteries.LOTTERY_ID,
            partySize = 2,
            joinedAt = daysFromNow(-1),
            status = WaitlistStatus.WAITING,
            rulesAcceptedAt = daysFromNow(-1)
        )
    )
}
