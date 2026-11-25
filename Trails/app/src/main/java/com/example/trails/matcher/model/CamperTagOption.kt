// Predefined amenity and preference tag options for CampMatch dating-app style preference chips.
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.model

/**
 * Visual preference option presented to campers when selecting their camping style.
 * Uses tasteful, relevant outdoor symbols modeled after modern preference badges.
 *
 * @property tag raw tag string matching campsite tags stored in Firestore (e.g. "Fire pit").
 * @property label human-readable label with relevant symbol (e.g. "🔥 Fire pit").
 * @property category grouping category for organized browsing.
 */
data class CamperTagOption(
    val tag: String,
    val label: String,
    val category: TagCategory = TagCategory.AMENITY
)

/**
 * Grouping categories for amenity search and organization.
 */
enum class TagCategory(val title: String) {
    AMENITY("Amenities & Facilities"),
    NATURE("Nature & Scenery"),
    ACTIVITY("Activities"),
    CAMPING_STYLE("Camping Style")
}

/**
 * Standard registry of selectable camping amenities for the CampMatch discovery experience.
 */
object CamperTagRegistry {
    val options: List<CamperTagOption> = listOf(
        // Amenities & Facilities
        CamperTagOption(tag = "Fire pit", label = "🔥 Fire pit", category = TagCategory.AMENITY),
        CamperTagOption(tag = "Showers", label = "🚿 Showers", category = TagCategory.AMENITY),
        CamperTagOption(tag = "Power hookup", label = "⚡ Power hookup", category = TagCategory.AMENITY),
        CamperTagOption(tag = "Drinking water", label = "💧 Drinking water", category = TagCategory.AMENITY),
        CamperTagOption(tag = "Flush toilets", label = "🚽 Flush toilets", category = TagCategory.AMENITY),
        CamperTagOption(tag = "Firewood", label = "🪵 Firewood", category = TagCategory.AMENITY),
        CamperTagOption(tag = "Cookhouse", label = "🍳 Cookhouse", category = TagCategory.AMENITY),
        CamperTagOption(tag = "Wi-Fi", label = "📶 Wi-Fi", category = TagCategory.AMENITY),
        CamperTagOption(tag = "Bear-proof cache", label = "🐻 Bear cache", category = TagCategory.AMENITY),
        CamperTagOption(tag = "Accessible", label = "♿ Accessible", category = TagCategory.AMENITY),

        // Nature & Scenery
        CamperTagOption(tag = "Lakeside", label = "🌊 Lakeside", category = TagCategory.NATURE),
        CamperTagOption(tag = "Mountain view", label = "🏔️ Mountain view", category = TagCategory.NATURE),
        CamperTagOption(tag = "Deep forest", label = "🌲 Deep forest", category = TagCategory.NATURE),
        CamperTagOption(tag = "Riverfront", label = "🏞️ Riverfront", category = TagCategory.NATURE),
        CamperTagOption(tag = "Stargazing", label = "🌌 Stargazing", category = TagCategory.NATURE),
        CamperTagOption(tag = "Beach access", label = "🏖️ Beach access", category = TagCategory.NATURE),

        // Activities
        CamperTagOption(tag = "Hiking", label = "🥾 Hiking trails", category = TagCategory.ACTIVITY),
        CamperTagOption(tag = "Fishing", label = "🎣 Fishing", category = TagCategory.ACTIVITY),
        CamperTagOption(tag = "Boating", label = "🚣 Boat launch", category = TagCategory.ACTIVITY),
        CamperTagOption(tag = "Biking", label = "🚴 Biking trails", category = TagCategory.ACTIVITY),
        CamperTagOption(tag = "Wildlife viewing", label = "🦌 Wildlife viewing", category = TagCategory.ACTIVITY),
        CamperTagOption(tag = "Canoeing", label = "🛶 Canoe rental", category = TagCategory.ACTIVITY),

        // Camping Style
        CamperTagOption(tag = "Pet friendly", label = "🐾 Pet friendly", category = TagCategory.CAMPING_STYLE),
        CamperTagOption(tag = "Quiet hours", label = "🌙 Quiet hours", category = TagCategory.CAMPING_STYLE),
        CamperTagOption(tag = "RV pull-through", label = "🚐 RV pull-through", category = TagCategory.CAMPING_STYLE),
        CamperTagOption(tag = "Tent only", label = "⛺ Tent only", category = TagCategory.CAMPING_STYLE)
    )

    /**
     * Filters available tag options by a search query string.
     *
     * @param query user input query; matches tag or label case-insensitively.
     * @return list of matching [CamperTagOption] items.
     */
    fun search(query: String): List<CamperTagOption> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return options
        return options.filter {
            it.tag.contains(trimmed, ignoreCase = true) ||
                it.label.contains(trimmed, ignoreCase = true)
        }
    }
}
