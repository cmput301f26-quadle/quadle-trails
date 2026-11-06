// Sample lotteries, one per status: open (Banff + Jasper), scheduled (Hinton) and closed.
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
package com.example.trails.data.fake

import com.example.trails.data.model.Lottery
import com.google.firebase.Timestamp
import java.util.Calendar

object TestLotteries {
    const val LOTTERY_ID = "lottery-summer-rockies"
    const val SCHEDULED_LOTTERY_ID = "lottery-may-long-weekend"
    const val CLOSED_LOTTERY_ID = "lottery-spring-opening"

    // Relative dates at a fixed time of day, so the Figma-style "Apr 1, 09:00" times show up.
    private fun daysFromNowAt(days: Int, hour: Int): Timestamp {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Timestamp(cal.time)
    }

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
        ),
        // Registration opens in 12 days, as in the Figma "May long weekend" mockups.
        Lottery(
            id = SCHEDULED_LOTTERY_ID,
            ownerId = TestUsers.OWNER_ID,
            name = "May long weekend",
            campsiteIds = listOf(TestCampsites.HINTON_ID),
            registrationStart = daysFromNowAt(12, 9),
            registrationEnd = daysFromNowAt(19, 9),
            bookingStart = daysFromNowAt(19, 9),
            bookingEnd = daysFromNowAt(21, 18),
            numWinners = 5,
            waitlistCapacity = 100
        ),
        // Closed last month with nobody on the waitlist.
        Lottery(
            id = CLOSED_LOTTERY_ID,
            ownerId = TestUsers.OWNER_ID,
            name = "Spring opening",
            campsiteIds = listOf(TestCampsites.JASPER_ID),
            registrationStart = daysFromNowAt(-50, 9),
            registrationEnd = daysFromNowAt(-40, 9),
            bookingStart = daysFromNowAt(-40, 9),
            bookingEnd = daysFromNowAt(-38, 18),
            numWinners = 5
        )
    )
}
