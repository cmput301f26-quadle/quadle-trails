// Single place that decides which repository implementation the app uses.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data

import com.example.trails.data.fake.FakeRatingRepository
import com.example.trails.data.fake.FakeReportRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.firestore.FirestoreCampsiteRepository
import com.example.trails.data.firestore.FirestoreLotteryRepository
import com.example.trails.data.firestore.FirestoreNotificationRepository
import com.example.trails.data.firestore.FirestoreMessageRepository
import com.example.trails.data.firestore.FirestoreReservationRepository
import com.example.trails.data.firestore.FirestoreWaitlistRepository
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.NotificationRepository
import com.example.trails.data.repository.MessageRepository
import com.example.trails.data.repository.RatingRepository
import com.example.trails.data.repository.ReportRepository
import com.example.trails.data.repository.ReservationRepository
import com.example.trails.data.repository.UserRepository
import com.example.trails.data.repository.WaitlistRepository

object AppContainer {
    val lotteryCollaborationRepository: com.example.trails.data.repository.LotteryCollaborationRepository by lazy {
        com.example.trails.data.firestore.FirestoreLotteryCollaborationRepository()
    }
    // User — Aarav swaps this for the Firestore version.
    val userRepository: UserRepository = FakeUserRepository()

    // Campsites, lotteries and waitlists are on Firestore together, since campsites point at
    // lottery ids and waitlist entries live under each lottery. Lazy so code that never uses
    // one of them, like JVM unit tests that pass fakes, never starts Firebase.

    // Campsite — Ibrahim.
    val campsiteRepository: CampsiteRepository by lazy { FirestoreCampsiteRepository() }

    // Lottery — Skanda.
    val lotteryRepository: LotteryRepository by lazy { FirestoreLotteryRepository() }

    // Waitlist — Chitkarsh.
    val waitlistRepository: WaitlistRepository by lazy { FirestoreWaitlistRepository() }

    // Reservation — Chitkarsh.
    val reservationRepository: ReservationRepository by lazy { FirestoreReservationRepository() }

    // Rating — Zainab swaps this for the Firestore version.
    val ratingRepository: RatingRepository = FakeRatingRepository()

    // Report — Zainab swaps this for the Firestore version when reports exist in Firestore.
    val reportRepository: ReportRepository = FakeReportRepository()

    //Temporary version made by Kasey, can be changed/removed
    val notificationRepository: NotificationRepository by lazy { FirestoreNotificationRepository() }
    // Messages — Neaz.
    val messageRepository: MessageRepository by lazy { FirestoreMessageRepository() }
}
