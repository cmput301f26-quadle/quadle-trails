// A campsite listing owned by a host, stored in the campsites collection.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.firestore.DocumentId

data class Campsite(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val maxOccupancy: Int = 1,
    val pricePerNight: Double = 0.0,
    val tags: List<String> = emptyList(),
    val rules: String = "",
    val cancellationPolicy: String = "",
    val imageUrl: String? = null,
    val availability: List<DateRange> = emptyList(),
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val activeLotteryId: String? = null
)
