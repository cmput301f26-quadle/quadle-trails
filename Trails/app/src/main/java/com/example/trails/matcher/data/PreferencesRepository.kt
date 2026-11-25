// Repository contract and local implementation for persisting camper matching preferences (Repository pattern).
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.data

import android.content.Context
import android.content.SharedPreferences
import com.example.trails.matcher.model.CamperPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repository interface providing reactive observation and persistence for [CamperPreferences].
 *
 * NOTE FOR TEAMMATES:
 * - Aarav (Profile): If you want to connect user profile tags with CampMatch preferences,
 *   you can inject or observe this repository.
 */
interface PreferencesRepository {
    /**
     * Observes current camper preferences in real time.
     */
    fun observePreferences(): Flow<CamperPreferences>

    /**
     * Returns the current snapshot of camper preferences.
     */
    fun getPreferences(): CamperPreferences

    /**
     * Updates and persists camper preferences.
     *
     * @param preferences updated preference settings.
     */
    suspend fun savePreferences(preferences: CamperPreferences)
}

/**
 * In-memory implementation of [PreferencesRepository] for unit tests and preview controllers.
 *
 * @param initial default preferences to initialize with.
 */
class InMemoryPreferencesRepository(
    initial: CamperPreferences = CamperPreferences()
) : PreferencesRepository {

    private val _prefs = MutableStateFlow(initial)

    override fun observePreferences(): Flow<CamperPreferences> = _prefs.asStateFlow()

    override fun getPreferences(): CamperPreferences = _prefs.value

    override suspend fun savePreferences(preferences: CamperPreferences) {
        _prefs.value = preferences
    }
}

/**
 * Android SharedPreferences implementation persisting camper preferences across app restarts.
 *
 * @param context Android application context used to access SharedPreferences.
 */
class LocalPreferencesRepository(
    context: Context
) : PreferencesRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val stateFlow = MutableStateFlow(loadFromPrefs())

    override fun observePreferences(): Flow<CamperPreferences> = stateFlow.asStateFlow()

    override fun getPreferences(): CamperPreferences = stateFlow.value

    override suspend fun savePreferences(preferences: CamperPreferences) {
        prefs.edit()
            .putStringSet(KEY_TAGS, preferences.preferredTags)
            .putFloat(KEY_MAX_PRICE, preferences.maxPricePerNight?.toFloat() ?: -1f)
            .putInt(KEY_MIN_OCCUPANCY, preferences.minOccupancy ?: -1)
            .putBoolean(KEY_PRIORITIZE_ODDS, preferences.prioritizeHighWinOdds)
            .putFloat(KEY_MIN_RATING, preferences.minRating?.toFloat() ?: -1f)
            .apply()

        stateFlow.value = preferences
    }

    private fun loadFromPrefs(): CamperPreferences {
        val tags = prefs.getStringSet(KEY_TAGS, emptySet()) ?: emptySet()
        val price = prefs.getFloat(KEY_MAX_PRICE, -1f)
        val occupancy = prefs.getInt(KEY_MIN_OCCUPANCY, -1)
        val prioritizeOdds = prefs.getBoolean(KEY_PRIORITIZE_ODDS, true)
        val rating = prefs.getFloat(KEY_MIN_RATING, -1f)

        return CamperPreferences(
            preferredTags = tags,
            maxPricePerNight = if (price >= 0f) price.toDouble() else null,
            minOccupancy = if (occupancy > 0) occupancy else null,
            prioritizeHighWinOdds = prioritizeOdds,
            minRating = if (rating >= 0f) rating.toDouble() else null
        )
    }

    companion object {
        private const val PREFS_NAME = "trails_camper_match_prefs"
        private const val KEY_TAGS = "key_preferred_tags"
        private const val KEY_MAX_PRICE = "key_max_price"
        private const val KEY_MIN_OCCUPANCY = "key_min_occupancy"
        private const val KEY_PRIORITIZE_ODDS = "key_prioritize_odds"
        private const val KEY_MIN_RATING = "key_min_rating"
    }
}
