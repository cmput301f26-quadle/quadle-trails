package com.example.trails.data.fake

import com.example.trails.data.AppContainer
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val currentTime = Timestamp.now()
val minutesPrevious = Timestamp(currentTime.seconds - 120, currentTime.nanoseconds)
val hourPrevious = Timestamp(currentTime.seconds - 3600, currentTime.nanoseconds)
val dayPrevious = Timestamp(currentTime.seconds - 86400, currentTime.nanoseconds)


object TestNotifications {

    fun getAll(): List<Notification> {
        var campsite : Campsite? = null
        runBlocking {
            //campsite = //TODO: ReservationRepository.getById("1")
        }

         return listOf(
            Notification(
                recipient = TestUsers.ENTRANT_ID,
                title = "You've won the lottery for Two Jack Lakeside Campground",
                content = "Date Block - All May Weekends\nYour next trip is one step closer. Select your dates to complete your booking.",
                timestamp = hourPrevious,
                type = NotificationType.LOTTERY_SINGLE,
                related = null
            ),
            Notification(
                recipient = TestUsers.ENTRANT_ID,
                title = "Lottery not picked for Jasper Pines this time",
                content = "Date Block - All May Weekends\nYou were not selected for Jasper Pines this time.",
                timestamp = dayPrevious,
                type = NotificationType.LOTTERY_LOST,
                related = null
            ),
//            Notification(
//                recipient = TestUsers.ENTRANT_ID,
//                title = "Reservation Confirmed",
//                content = "",
//                timestamp = minutesPrevious,
//                type = NotificationType.RESERVE_CONFIRM,
//                related = null
//            )
        )
    }
}