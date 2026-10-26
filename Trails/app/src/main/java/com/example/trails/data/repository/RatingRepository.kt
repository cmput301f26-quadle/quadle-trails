// Read/write access to ratings, which live under campsites/{id}/ratings.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
// FIRESTORE IMPLEMENTATION: Zainab (zsaghir)
package com.example.trails.data.repository

import com.example.trails.data.model.Rating
import kotlinx.coroutines.flow.Flow

interface RatingRepository {
    fun observeForCampsite(campsiteId: String): Flow<List<Rating>>
    suspend fun getRating(campsiteId: String, userId: String): Rating?
    suspend fun save(rating: Rating)
}
