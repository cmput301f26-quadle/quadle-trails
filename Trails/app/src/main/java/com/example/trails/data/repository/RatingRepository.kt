// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.13.01, 03.09.01
// PURPOSE: Read/write access to ratings, which live under campsites/{id}/ratings.
// FIRESTORE IMPLEMENTATION: Zainab (zsaghir)
// Outstanding issues: AppContainer still uses FakeRatingRepository; FirestoreRatingRepository
//   isn't switched on until ratings are seeded in Firestore.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.data.repository

import com.example.trails.data.model.Rating
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to campsite ratings. Each user has at most one rating per campsite.
 */
interface RatingRepository {
    /**
     * Watches every rating of one campsite.
     *
     * @param campsiteId id of the campsite whose ratings to watch.
     * @return a flow that emits the campsite's ratings now and again whenever they change.
     */
    fun observeForCampsite(campsiteId: String): Flow<List<Rating>>

    /**
     * Looks up one user's rating of a campsite.
     *
     * @param campsiteId id of the rated campsite.
     * @param userId device id of the reviewer.
     * @return the rating, or null if this user hasn't rated this campsite.
     */
    suspend fun getRating(campsiteId: String, userId: String): Rating?

    /**
     * Saves a rating, replacing any earlier rating by the same user of the same campsite.
     *
     * @param rating the rating to store; its campsiteId and userId pick where it goes.
     */
    suspend fun save(rating: Rating)

    /**
     * Deletes one user's rating of a campsite, e.g. when an admin removes a rating that
     * breaks app policy (US 03.09.01). Does nothing if that rating doesn't exist.
     *
     * @param campsiteId id of the rated campsite.
     * @param userId device id of the reviewer whose rating is deleted.
     */
    suspend fun delete(campsiteId: String, userId: String)
}
