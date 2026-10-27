// Sample waitlist entries for the sample lottery.
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
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
