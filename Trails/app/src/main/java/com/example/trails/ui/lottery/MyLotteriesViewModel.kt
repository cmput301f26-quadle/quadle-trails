// Controller for the owner's Lotteries tab (supports US 02.04.06).
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Outstanding issues: none.
package com.example.trails.ui.lottery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Lottery
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Lists the owner's lotteries with live entrant counts, filtered by the selected chip.
 *
 * @param lotteryRepository where lotteries are stored.
 * @param waitlistRepository used to count each lottery's entrants.
 * @param ownerId whose lotteries to list.
 * @param clock current time; tests pass a fixed one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyLotteriesViewModel(
    lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    ownerId: String = CurrentUser.id(),
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val filter = MutableStateFlow(LotteryFilter.ALL)

    private val lotteriesWithCounts: Flow<Pair<List<Lottery>, Map<String, Int>>> =
        lotteryRepository.observeByOwner(ownerId).flatMapLatest { lotteries ->
            if (lotteries.isEmpty()) {
                flowOf(lotteries to emptyMap())
            } else {
                combine(lotteries.map { l -> waitlistRepository.observeForLottery(l.id).map { l.id to it.size } }) { counts ->
                    lotteries to counts.toMap()
                }
            }
        }

    /** What the tab should show. */
    val uiState: StateFlow<MyLotteriesUiState> =
        combine(lotteriesWithCounts, filter) { (lotteries, counts), chip ->
            buildMyLotteriesState(lotteries, counts, chip, clock())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyLotteriesUiState.Loading)

    /** Selects a filter chip. */
    fun onFilterChange(chip: LotteryFilter) {
        filter.value = chip
    }
}
