// Single place that decides which repository implementation the app uses.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeRatingRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.RatingRepository
import com.example.trails.data.repository.UserRepository
import com.example.trails.data.repository.WaitlistRepository

object AppContainer {
    // User — Aarav swaps this for the Firestore version.
    val userRepository: UserRepository = FakeUserRepository()

    // Campsite — Ibrahim swaps this for the Firestore version.
    val campsiteRepository: CampsiteRepository = FakeCampsiteRepository()

    // Lottery — Skanda swaps this for the Firestore version.
    val lotteryRepository: LotteryRepository = FakeLotteryRepository()

    // Waitlist — Chitkarsh swaps this for the Firestore version.
    val waitlistRepository: WaitlistRepository = FakeWaitlistRepository()

    // Rating — Zainab swaps this for the Firestore version.
    val ratingRepository: RatingRepository = FakeRatingRepository()
}
