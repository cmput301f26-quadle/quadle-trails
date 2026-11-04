// A campsite listing owned by a host, stored in the campsites collection.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.firestore.DocumentId

/**
 * A campsite that an owner lists and entrants can enter lotteries for.
 *
 * @property id Firestore document id.
 * @property ownerId Device id of the owner who hosts this campsite.
 * @property name Display name of the site.
 * @property description Free-text description shown on the details screen.
 * @property locationName Human-readable location, e.g. "Banff, AB".
 * @property latitude Latitude of the site, in degrees.
 * @property longitude Longitude of the site, in degrees.
 * @property maxOccupancy Maximum number of people the site sleeps.
 * @property pricePerNight Nightly price in dollars.
 * @property tags Amenities and features used for display and filtering.
 * @property rules House rules entrants must agree to.
 * @property cancellationPolicy Cancellation terms shown to entrants.
 * @property imageUrl URL of the listing photo, or null if none.
 * @property availability Open date ranges for the site, as described by the Availability CRC card.
 * @property ratingAverage Mean rating score across all ratings.
 * @property ratingCount Number of ratings received.
 * @property activeLotteryId Id of the lottery currently covering this site, or null if none.
 */
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
) {
    /**
     * Checks the fields an owner fills in when registering or editing (US 02.01.01, 02.01.03).
     * Kept as a function rather than init checks because Firestore builds objects from defaults.
     *
     * @return error messages keyed by field name; empty if the campsite can be saved.
     */
    fun validate(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Name is required")
        if (locationName.isBlank()) put("locationName", "Location is required")
        if (description.isBlank()) put("description", "Describe the campsite and its amenities")
        if (maxOccupancy < 1) put("maxOccupancy", "Must sleep at least 1 person")
        if (pricePerNight < 0) put("pricePerNight", "Price can't be negative")
        if (rules.isBlank()) put("rules", "Rules are required")
        if (cancellationPolicy.isBlank()) put("cancellationPolicy", "Cancellation policy is required")
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) put("location", "Invalid map pin")
    }
}
