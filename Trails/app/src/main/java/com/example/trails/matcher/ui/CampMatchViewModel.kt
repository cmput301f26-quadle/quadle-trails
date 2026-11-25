// ViewModel managing CampMatch tag search, preference filtering, and real-time campsite ranking (MVVM pattern).
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.WaitlistRepository
import com.example.trails.matcher.data.InMemoryPreferencesRepository
import com.example.trails.matcher.data.PreferencesRepository
import com.example.trails.matcher.engine.CampsiteMatcher
import com.example.trails.matcher.model.CamperPreferences
import com.example.trails.matcher.model.CamperTagOption
import com.example.trails.matcher.model.CamperTagRegistry
import com.example.trails.matcher.model.LotteryContext
import com.example.trails.matcher.model.MatchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presentation model pairing a [Campsite] with its computed [MatchResult].
 *
 * @property campsite the campsite listing entity.
 * @property matchResult compatibility score, highlights, and breakdown.
 */
data class CampMatchItem(
    val campsite: Campsite,
    val matchResult: MatchResult
)

/**
 * UI state rendered by [CampMatchScreen].
 *
 * @property isLoading true while initial campsite and lottery streams are loading.
 * @property searchQuery text currently typed into the amenity search bar.
 * @property selectedTags set of tags currently active in the camper's preferences.
 * @property maxPricePerNight optional budget cap in dollars.
 * @property prioritizeHighWinOdds true if high lottery win probabilities receive a score boost.
 * @property availableTags list of tag options matching the current search query.
 * @property rankedMatches list of campsites sorted descending by compatibility percentage.
 * @property errorMessage failure message if loading or calculations encounter an error.
 */
data class CampMatchUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val maxPricePerNight: Double? = null,
    val prioritizeHighWinOdds: Boolean = true,
    val availableTags: List<CamperTagOption> = CamperTagRegistry.options,
    val rankedMatches: List<CampMatchItem> = emptyList(),
    val errorMessage: String? = null
)

/**
 * ViewModel controller coordinating amenity search, preference persistence, and campsite ranking.
 *
 * NOTE FOR TEAMMATES:
 * - Default arguments use [AppContainer] repositories, so this ViewModel can be constructed
 *   with zero arguments in Compose or with test fakes in JVM unit tests.
 *
 * @param campsiteRepository repository streaming all campsite listings.
 * @param lotteryRepository repository streaming active lotteries.
 * @param waitlistRepository repository used to count entrants for active lotteries.
 * @param preferencesRepository repository persisting camper preference criteria.
 */
class CampMatchViewModel(
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val preferencesRepository: PreferencesRepository = InMemoryPreferencesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampMatchUiState())
    val uiState: StateFlow<CampMatchUiState> = _uiState.asStateFlow()

    private var cachedCampsites: List<Campsite> = emptyList()
    private var cachedLotteries: List<Lottery> = emptyList()
    private var cachedLotteryContexts: Map<String, LotteryContext> = emptyMap()

    init {
        observeDataStreams()
    }

    private fun observeDataStreams() {
        viewModelScope.launch {
            combine(
                campsiteRepository.observeAll(),
                lotteryRepository.observeAll(),
                preferencesRepository.observePreferences()
            ) { sites, lotteries, prefs ->
                Triple(sites, lotteries, prefs)
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load campsite recommendations.") }
            }.collect { (sites, lotteries, prefs) ->
                cachedCampsites = sites
                cachedLotteries = lotteries

                // Build lottery context map for active lotteries
                val contexts = mutableMapOf<String, LotteryContext>()
                for (lottery in lotteries) {
                    val count = try {
                        waitlistRepository.getEntryCount(lottery.id)
                    } catch (_: Exception) {
                        0
                    }
                    contexts[lottery.id] = LotteryContext(
                        lotteryId = lottery.id,
                        numWinners = lottery.numWinners,
                        currentWaitlistCount = count,
                        waitlistCapacity = lottery.waitlistCapacity
                    )
                }
                cachedLotteryContexts = contexts

                recalculateRanking(prefs)
            }
        }
    }

    private fun recalculateRanking(preferences: CamperPreferences) {
        val rankedResults = CampsiteMatcher.rankCampsites(
            campsites = cachedCampsites,
            preferences = preferences,
            lotteryContexts = cachedLotteryContexts
        )

        val items = rankedResults.mapNotNull { result ->
            val site = cachedCampsites.firstOrNull { it.id == result.campsiteId }
            site?.let { CampMatchItem(it, result) }
        }

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                selectedTags = preferences.preferredTags,
                maxPricePerNight = preferences.maxPricePerNight,
                prioritizeHighWinOdds = preferences.prioritizeHighWinOdds,
                rankedMatches = items,
                availableTags = CamperTagRegistry.search(current.searchQuery)
            )
        }
    }

    /**
     * Updates the search filter query and filters the available amenity tag list.
     *
     * @param query text typed into the amenity search input.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                availableTags = CamperTagRegistry.search(query)
            )
        }
    }

    /**
     * Clears the active search query string.
     */
    fun clearSearchQuery() {
        onSearchQueryChanged("")
    }

    /**
     * Toggles a tag on or off in the camper's active preferences and updates ranking.
     *
     * @param tag the amenity tag string to toggle (e.g. "Fire pit").
     */
    fun onTagToggled(tag: String) {
        viewModelScope.launch {
            val currentPrefs = preferencesRepository.getPreferences()
            val newTags = if (tag in currentPrefs.preferredTags) {
                currentPrefs.preferredTags - tag
            } else {
                currentPrefs.preferredTags + tag
            }
            val updated = currentPrefs.copy(preferredTags = newTags)
            preferencesRepository.savePreferences(updated)
            recalculateRanking(updated)
        }
    }

    /**
     * Removes all selected tags from the active preferences.
     */
    fun onClearAllTags() {
        viewModelScope.launch {
            val currentPrefs = preferencesRepository.getPreferences()
            val updated = currentPrefs.copy(preferredTags = emptySet())
            preferencesRepository.savePreferences(updated)
            recalculateRanking(updated)
        }
    }

    /**
     * Updates the maximum nightly price budget filter.
     *
     * @param price maximum nightly rate in dollars, or null to remove budget constraint.
     */
    fun onMaxPriceChanged(price: Double?) {
        viewModelScope.launch {
            val currentPrefs = preferencesRepository.getPreferences()
            val updated = currentPrefs.copy(maxPricePerNight = price)
            preferencesRepository.savePreferences(updated)
            recalculateRanking(updated)
        }
    }

    /**
     * Toggles whether lotteries with higher entrant win probabilities receive a scoring boost.
     */
    fun onTogglePrioritizeOdds() {
        viewModelScope.launch {
            val currentPrefs = preferencesRepository.getPreferences()
            val updated = currentPrefs.copy(prioritizeHighWinOdds = !currentPrefs.prioritizeHighWinOdds)
            preferencesRepository.savePreferences(updated)
            recalculateRanking(updated)
        }
    }
}
