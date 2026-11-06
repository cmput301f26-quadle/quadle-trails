// Where a lottery is in its life: scheduled, open, or closed (US 02.04.06).
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Plain Kotlin, so it is unit-tested directly.
package com.example.trails.ui.lottery

import com.example.trails.data.model.Lottery

/** The badge on lottery cards and the details page (Figma F06, F19, F23–F25). */
enum class LotteryStatus {
    /** Registration hasn't opened yet; everything can still change. */
    SCHEDULED,

    /** Campers can join the waiting list now. */
    OPEN,

    /** Registration closed and winners were drawn from the entrants. */
    DRAWN,

    /** Registration closed with nobody on the waiting list. */
    NO_ENTRANTS;

    /** True once registration has closed; closed lotteries can't be edited. */
    val isClosed: Boolean get() = this == DRAWN || this == NO_ENTRANTS
}

/**
 * Works out a lottery's status at [now].
 *
 * @param entrantCount how many people are on its waiting list.
 * @param now current time in milliseconds.
 */
fun lotteryStatus(lottery: Lottery, entrantCount: Int, now: Long): LotteryStatus {
    val start = lottery.registrationStart.millis()
    val end = lottery.registrationEnd.millis()
    return when {
        end != null && now >= end -> if (entrantCount > 0) LotteryStatus.DRAWN else LotteryStatus.NO_ENTRANTS
        start != null && now < start -> LotteryStatus.SCHEDULED
        else -> LotteryStatus.OPEN
    }
}

/** The filter chips above the lottery list (Figma F06). */
enum class LotteryFilter {
    ALL, SCHEDULED, OPEN, DONE;

    /** True if a lottery with [status] belongs under this chip. */
    fun matches(status: LotteryStatus): Boolean = when (this) {
        ALL -> true
        SCHEDULED -> status == LotteryStatus.SCHEDULED
        OPEN -> status == LotteryStatus.OPEN
        DONE -> status.isClosed
    }
}
