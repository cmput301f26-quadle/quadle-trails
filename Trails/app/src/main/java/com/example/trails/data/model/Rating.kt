// A user's rating of a campsite, stored under campsites/{id}/ratings.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * One user's score and review of a campsite.
 *
 * @property userId Device id of the reviewer; also the document id, so one rating per user per site.
 * @property campsiteId Id of the campsite being rated.
 * @property score Star score given by the user.
 * @property review Optional written review.
 * @property createdAt When the rating was submitted.
 */
data class Rating(
    @DocumentId val userId: String = "",
    val campsiteId: String = "",
    val score: Int = 0,
    val review: String = "",
    val createdAt: Timestamp? = null
)
