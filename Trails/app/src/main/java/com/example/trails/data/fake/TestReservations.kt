// Sample confirmed reservations used to seed in-memory fakes and compose previews (Model in MVVM).
// implements US 01.04.01, US 01.04.02, US 01.04.03
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.fake

import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus

/**
 * Sample reservation datasets for unit tests and local previews.
 */
object TestReservations {
    const val RESERVATION_BANFF_ID = "res-banff-sample-01"
    const val RESERVATION_JASPER_ID = "res-jasper-sample-02"

    /**
     * Default list of sample reservations: one in Banff and one in Jasper.
     */
    val all = listOf(
        Reservation(
            id = RESERVATION_BANFF_ID,
            userId = TestUsers.ENTRANT_ID,
            campsiteId = TestCampsites.BANFF_ID,
            lotteryId = TestLotteries.LOTTERY_ID,
            partySize = 2,
            startDate = daysFromNow(35),
            endDate = daysFromNow(38),
            totalPrice = 126.0,
            rulesAccepted = true,
            rulesAcceptedAt = daysFromNow(-1),
            createdAt = daysFromNow(-1),
            status = ReservationStatus.ACTIVE
        ),
        Reservation(
            id = RESERVATION_JASPER_ID,
            userId = TestUsers.ENTRANT_ID,
            campsiteId = TestCampsites.JASPER_ID,
            lotteryId = TestLotteries.LOTTERY_ID,
            partySize = 4,
            startDate = daysFromNow(45),
            endDate = daysFromNow(48),
            totalPrice = 115.5,
            rulesAccepted = true,
            rulesAcceptedAt = daysFromNow(-2),
            createdAt = daysFromNow(-2),
            status = ReservationStatus.ACTIVE
        )
    )
}
