// Read/write access to ratings, which live under campsites/{id}/ratings.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
// FIRESTORE IMPLEMENTATION: Zainab (zsaghir)
package com.example.trails.data.repository

import com.example.trails.data.model.Rating
import kotlinx.coroutines.flow.Flow

interface RatingRepository {
    fun observeForCampsite(campsiteId: String): Flow<List<Rating>>
    suspend fun getRating(campsiteId: String, userId: String): Rating?
    suspend fun save(rating: Rating)
}
