// Single place that decides which repository implementation the app uses.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data

import com.example.trails.data.fake.FakeRatingRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.firestore.FirestoreCampsiteRepository
import com.example.trails.data.firestore.FirestoreLotteryRepository
import com.example.trails.data.firestore.FirestoreWaitlistRepository
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.RatingRepository
import com.example.trails.data.repository.UserRepository
import com.example.trails.data.repository.WaitlistRepository

object AppContainer {
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

    // Rating — Zainab swaps this for the Firestore version.
    val ratingRepository: RatingRepository = FakeRatingRepository()
}
