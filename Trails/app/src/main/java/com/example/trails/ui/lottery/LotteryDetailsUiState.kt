// UI state for the owner's Lottery details page (US 02.04.06).
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Plain Kotlin (no Compose), so it can be unit-tested.
package com.example.trails.ui.lottery

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery

/** Every situation the Lottery details page can be in. */
sealed interface LotteryDetailsUiState {
    /** Waiting for the lottery to load. */
    data object Loading : LotteryDetailsUiState

    /** No lottery has that id. */
    data object NotFound : LotteryDetailsUiState

    /**
     * The lottery is loaded.
     *
     * @property lottery the lottery.
     * @property campsites the campsites it covers, in the lottery's order.
     * @property entrantCount people on its waiting list.
     * @property status scheduled, open or closed at [now].
     * @property now when the status was worked out.
     */
    data class Loaded(
        val lottery: Lottery,
        val campsites: List<Campsite>,
        val entrantCount: Int,
        val status: LotteryStatus,
        val now: Long
    ) : LotteryDetailsUiState {
        /** Closed lotteries can't be edited, so the page hides Edit. */
        val canEdit: Boolean get() = !status.isClosed
    }
}

/**
 * Picks the page state from the latest lottery, campsites and entrant count.
 *
 * @param lottery the lottery, or null if it doesn't exist.
 * @param allCampsites campsites to look the lottery's campsite ids up in.
 */
fun buildLotteryDetailsState(lottery: Lottery?, allCampsites: List<Campsite>, entrantCount: Int, now: Long): LotteryDetailsUiState =
    if (lottery == null) {
        LotteryDetailsUiState.NotFound
    } else {
        LotteryDetailsUiState.Loaded(
            lottery = lottery,
            campsites = lottery.campsiteIds.mapNotNull { id -> allCampsites.find { it.id == id } },
            entrantCount = entrantCount,
            status = lotteryStatus(lottery, entrantCount, now),
            now = now
        )
    }
