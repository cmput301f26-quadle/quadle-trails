// A user's rating of a campsite, stored under campsites/{id}/ratings.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Rating(
    @DocumentId val userId: String = "",
    val campsiteId: String = "",
    val score: Int = 0,
    val review: String = "",
    val createdAt: Timestamp? = null
)
