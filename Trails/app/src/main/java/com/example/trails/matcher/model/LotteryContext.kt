// Domain model holding lottery and waitlist capacity context for matching calculations (Model in MVVM).
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.model

/**
 * Contextual lottery and waitlist statistics used to calculate entrant win odds.
 *
 * NOTE FOR TEAMMATES:
 * - Skanda (Lottery Setup): Pulls [numWinners] from Lottery and waitlist count from WaitlistRepository.
 * - Chitkarsh (Waitlist): Used by the matching engine to favor lotteries where entrants have high probability of winning.
 *
 * @property lotteryId unique identifier of the active lottery.
 * @property numWinners total number of winners drawn for this lottery.
 * @property currentWaitlistCount total number of entrants currently enrolled on the waitlist.
 * @property waitlistCapacity optional maximum capacity cap configured on the lottery, or null if unlimited.
 */
data class LotteryContext(
    val lotteryId: String,
    val numWinners: Int = 1,
    val currentWaitlistCount: Int = 0,
    val waitlistCapacity: Int? = null
) {
    /**
     * Estimated probability (0.0 to 1.0) of winning if an entrant joins right now.
     * If the waitlist is smaller than the winner count, odds are 100% (1.0).
     */
    val estimatedWinProbability: Double
        get() = when {
            currentWaitlistCount <= 0 -> 1.0
            currentWaitlistCount < numWinners -> 1.0
            else -> (numWinners.toDouble() / currentWaitlistCount).coerceIn(0.0, 1.0)
        }
}
