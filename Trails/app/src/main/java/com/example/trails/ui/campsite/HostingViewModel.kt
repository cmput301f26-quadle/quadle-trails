// Controller for the owner's My Campsites screen (US 02.03.01).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Outstanding issues: Firestore listener errors are not surfaced to the screen yet.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * One row on the My Campsites list.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property campsite the campsite shown.
 * @property lotteryName name of the lottery it is in, or null if it is not in one.
 */
data class HostingItem(val campsite: Campsite, val lotteryName: String?)

/**
 * Everything [HostingContent] needs to draw.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property items campsites matching [query], sorted by name.
 * @property query the current search text.
 * @property totalCount all of the owner's campsites, ignoring the search.
 * @property inLotteryCount how many of those are in a lottery.
 */
data class HostingUiState(
    val items: List<HostingItem>,
    val query: String,
    val totalCount: Int,
    val inLotteryCount: Int
)

/**
 * Pairs each campsite with its lottery, applies the search, and counts.
 * Kept free of coroutines so it can be unit-tested directly.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param campsites the owner's campsites.
 * @param lotteries the owner's lotteries.
 * @param query search text matched against name and location, ignoring case.
 * @return the state for the given inputs.
 */
fun buildHostingState(
    campsites: List<Campsite>,
    lotteries: List<Lottery>,
    query: String
): HostingUiState {
    val all = campsites.map { campsite ->
        val lottery = lotteries.find { it.id == campsite.activeLotteryId }
            ?: lotteries.find { campsite.id in it.campsiteIds }
        HostingItem(campsite, lottery?.name)
    }
    val q = query.trim()
    val shown = all
        .filter {
            q.isEmpty() ||
                it.campsite.name.contains(q, ignoreCase = true) ||
                it.campsite.locationName.contains(q, ignoreCase = true)
        }
        .sortedBy { it.campsite.name.lowercase() }
    return HostingUiState(shown, query, all.size, all.count { it.lotteryName != null })
}

/**
 * Controller for [HostingScreen]: combines the owner's campsites and lotteries with the search text.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param campsiteRepository where campsites are stored.
 * @param lotteryRepository where lotteries are stored, used for each campsite's lottery status.
 * @param ownerId the owner whose campsites to show.
 */
class HostingViewModel(
    campsiteRepository: CampsiteRepository,
    lotteryRepository: LotteryRepository,
    ownerId: String
) : ViewModel() {

    private val query = MutableStateFlow("")

    /** The screen state; null while the first load is in progress. */
    val state: StateFlow<HostingUiState?> = combine(
        campsiteRepository.observeByOwner(ownerId),
        lotteryRepository.observeByOwner(ownerId),
        query,
        ::buildHostingState
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Updates the search text; the list re-filters immediately. */
    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }
}
