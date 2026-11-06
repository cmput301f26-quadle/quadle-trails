// UI state and list logic for the owner's Lotteries tab (supports US 02.04.06).
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Plain Kotlin (no Compose), so it can be unit-tested.
package com.example.trails.ui.lottery

import com.example.trails.data.model.Lottery

/**
 * One card on the Lotteries tab.
 *
 * @property lottery the lottery.
 * @property status its badge.
 * @property entrantCount people on its waiting list.
 */
data class LotteryCardItem(val lottery: Lottery, val status: LotteryStatus, val entrantCount: Int) {
    /** Winners actually drawn: never more than the people who entered. */
    val winnersDrawn: Int get() = minOf(lottery.numWinners, entrantCount)
}

/** Every situation the Lotteries tab can be in. */
sealed interface MyLotteriesUiState {
    /** Waiting for the owner's lotteries. */
    data object Loading : MyLotteriesUiState

    /**
     * Lotteries are loaded.
     *
     * @property items cards that match [filter], upcoming first.
     * @property filter the selected chip.
     * @property hasAny false when the owner has no lotteries at all, as opposed to none under this chip.
     */
    data class Loaded(val items: List<LotteryCardItem>, val filter: LotteryFilter, val hasAny: Boolean) : MyLotteriesUiState
}

/**
 * Builds the tab: works out each lottery's status, sorts scheduled → open → closed
 * (soonest first, most recently closed first) and applies the chip.
 *
 * @param entrantCounts waiting list size by lottery id; missing ids count as 0.
 */
fun buildMyLotteriesState(lotteries: List<Lottery>, entrantCounts: Map<String, Int>, filter: LotteryFilter, now: Long): MyLotteriesUiState.Loaded {
    val all = lotteries
        .map { lottery ->
            val count = entrantCounts[lottery.id] ?: 0
            LotteryCardItem(lottery, lotteryStatus(lottery, count, now), count)
        }
        .sortedWith(compareBy<LotteryCardItem>({ rank(it.status) }, { sortTime(it) }))
    return MyLotteriesUiState.Loaded(all.filter { filter.matches(it.status) }, filter, all.isNotEmpty())
}

private fun rank(status: LotteryStatus) = when (status) {
    LotteryStatus.SCHEDULED -> 0
    LotteryStatus.OPEN -> 1
    LotteryStatus.DRAWN, LotteryStatus.NO_ENTRANTS -> 2
}

private fun sortTime(item: LotteryCardItem): Long {
    val l = item.lottery
    return when (item.status) {
        LotteryStatus.SCHEDULED -> l.registrationStart.millis() ?: Long.MAX_VALUE
        LotteryStatus.OPEN -> l.registrationEnd.millis() ?: Long.MAX_VALUE
        LotteryStatus.DRAWN, LotteryStatus.NO_ENTRANTS -> -(l.registrationEnd.millis() ?: 0L)
    }
}
