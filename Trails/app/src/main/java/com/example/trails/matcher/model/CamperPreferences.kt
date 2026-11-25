// Domain model representing user preferences for the CampMatch recommendation engine (Model in MVVM).
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.model

/**
 * Filter and preference criteria selected by a camper to match against campsites.
 *
 * NOTE FOR TEAMMATES:
 * - Aarav (Profile & Identity): If you want to persist camper preferences to the user profile
 *   in Firestore under users/{id}/preferences, this data class is already Serializable/Firestore-ready.
 * - Ibrahim (Campsite Discovery): You can pass these preferences into [com.example.trails.matcher.engine.CampsiteMatcher]
 *   alongside any Campsite to generate real-time match scores and badge reasons.
 *
 * @property preferredTags set of amenity or facility tags the camper desires (e.g. "Fire pit", "Showers", "Lakeside").
 * @property maxPricePerNight maximum acceptable nightly rate in dollars, or null for no budget limit.
 * @property minOccupancy minimum party size the campsite must accommodate, or null for any size.
 * @property prioritizeHighWinOdds when true, gives a bonus score to campsites whose active lotteries have high odds of winning.
 * @property minRating minimum average star rating desired (0.0 to 5.0), or null for any rating.
 */
data class CamperPreferences(
    val preferredTags: Set<String> = emptySet(),
    val maxPricePerNight: Double? = null,
    val minOccupancy: Int? = null,
    val prioritizeHighWinOdds: Boolean = true,
    val minRating: Double? = null
)
