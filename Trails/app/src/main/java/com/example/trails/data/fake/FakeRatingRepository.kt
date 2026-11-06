// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.13.01
// PURPOSE: In-memory RatingRepository seeded from TestRatings.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.data.fake

import com.example.trails.data.model.Rating
import com.example.trails.data.repository.RatingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [RatingRepository] for previews and tests. Changes are lost when the app closes.
 *
 * @param initial ratings to start with; defaults to [TestRatings.all].
 */
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
