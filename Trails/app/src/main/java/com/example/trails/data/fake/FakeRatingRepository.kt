// In-memory RatingRepository seeded from TestRatings.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
package com.example.trails.data.fake

import com.example.trails.data.model.Rating
import com.example.trails.data.repository.RatingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeRatingRepository(initial: List<Rating> = TestRatings.all) : RatingRepository {
    private val ratings = MutableStateFlow(initial)

    override fun observeForCampsite(campsiteId: String): Flow<List<Rating>> =
        ratings.map { list -> list.filter { it.campsiteId == campsiteId } }

    override suspend fun getRating(campsiteId: String, userId: String): Rating? =
        ratings.value.find { it.campsiteId == campsiteId && it.userId == userId }

    override suspend fun save(rating: Rating) {
        ratings.update { list ->
            list.filterNot { it.campsiteId == rating.campsiteId && it.userId == rating.userId } + rating
        }
    }
}
