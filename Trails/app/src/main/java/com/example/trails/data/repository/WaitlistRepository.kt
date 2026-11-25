// Repository interface defining reactive streams and data access operations for waitlist entries (Repository pattern).
// implements US 01.01.06, US 01.02.01, US 01.02.02, US 01.04.03, US 02.04.02, US 02.09.01, US 02.11.01, and Bonus Rolling Lottery
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.repository

import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import kotlinx.coroutines.flow.Flow

// based on kotlin flow documentation for repository streaming
/**
 * Repository interface for managing and observing waitlist entries for lotteries.
 *
 * Provides real-time flows and asynchronous operations to view entrants,
 * join waitlists with rules agreement and capacity checks, and leave waitlists.
 */
interface WaitlistRepository {
    // observes all entries for a lottery in real time
    /**
     * Observes all waitlist entries for a specified lottery in real time.
     *
     * @param lotteryId the identifier of the lottery.
     * @return a [Flow] emitting the list of [WaitlistEntry] items, ordered by join timestamp.
     */
    fun observeForLottery(lotteryId: String): Flow<List<WaitlistEntry>>

    // observes one user's entry for a lottery in real time
    /**
     * Observes a specific user's waitlist entry for a lottery in real time.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user.
     * @return a [Flow] emitting the user's [WaitlistEntry] if enrolled, or null otherwise.
     */
    fun observeEntry(lotteryId: String, userId: String): Flow<WaitlistEntry?>

    // gets one entry for a user in a lottery
    /**
     * Fetches a single waitlist entry for a user in a given lottery.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user.
     * @return the [WaitlistEntry] if found, or null if the user is not enrolled.
     */
    suspend fun getEntry(lotteryId: String, userId: String): WaitlistEntry?

    // gets all entries for a lottery
    /**
     * Fetches all waitlist entries for a specified lottery.
     *
     * @param lotteryId the identifier of the lottery.
     * @return a list of [WaitlistEntry] items enrolled in the lottery.
     */
    suspend fun getEntriesForLottery(lotteryId: String): List<WaitlistEntry>

    // counts entries for a lottery
    /**
     * Counts the total number of enrolled waitlist entries for a lottery.
     *
     * @param lotteryId the identifier of the lottery.
     * @return the number of entrants currently on the waitlist.
     */
    suspend fun getEntryCount(lotteryId: String): Int

    // saves or updates an entry
    /**
     * Persists or updates a waitlist entry.
     *
     * @param entry the [WaitlistEntry] to save.
     * @throws IllegalArgumentException if required fields in the entry fail validation.
     */
    suspend fun save(entry: WaitlistEntry)

    // deletes an entry by lottery and user id
    /**
     * Deletes a waitlist entry by lottery and user identifier.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the entrant.
     */
    suspend fun deleteEntry(lotteryId: String, userId: String)

    // joins waitlist and checks rules acceptance plus capacity limits
    /**
     * Enrolls a user onto a lottery waitlist, verifying campsite rules acceptance and capacity bounds.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user joining.
     * @param userName display name of the user.
     * @param partySize number of guests in the entrant's party.
     * @param rulesAccepted whether the entrant has explicitly agreed to the campsite rules.
     * @param maxCapacity optional capacity cap; if specified and full, prevents new entries.
     * @return the created or updated [WaitlistEntry].
     * @throws IllegalArgumentException if parameters are blank or partySize is less than 1.
     * @throws RulesNotAcceptedException if rules have not been accepted.
     * @throws WaitlistFullException if the waitlist has reached maximum capacity.
     */
    suspend fun joinWaitlist(
        lotteryId: String,
        userId: String,
        userName: String,
        partySize: Int = 1,
        rulesAccepted: Boolean = true,
        maxCapacity: Int? = null
    ): WaitlistEntry

    // leaves a waitlist
    /**
     * Removes an entrant from a lottery waitlist.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user leaving.
     */
    suspend fun leaveWaitlist(lotteryId: String, userId: String)

    // updates status of an entrant's waitlist entry
    /**
     * Updates the status of an entrant's waitlist entry (e.g. WON, DECLINED, RESERVED).
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user.
     * @param status the new [WaitlistStatus] to set.
     * @return the updated [WaitlistEntry], or null if no entry exists.
     */
    suspend fun updateStatus(
        lotteryId: String,
        userId: String,
        status: WaitlistStatus
    ): WaitlistEntry?

    // --- Rolling Lottery (Fair random re-draws and invite recycling) ---
    // NOTE FOR TEAMMATES:
    // - Skanda (Organizer Lottery Screens): observeForLottery automatically streams newly promoted
    //   replacement winners when an entrant declines, so your entrant counts update in real time.
    // - Zainab (Notifications / Messages): When a replacement is drawn, their status transitions from
    //   WAITING to WON, which triggers observeEntry to emit win notifications.

    /**
     * Conducts a fair random lottery draw from all remaining entrants with status [WaitlistStatus.WAITING],
     * promoting the drawn entrant to [WaitlistStatus.WON].
     *
     * @param lotteryId the identifier of the lottery.
     * @return the newly drawn [WaitlistEntry], or null if no waiting entrants remain.
     */
    suspend fun drawNextReplacement(lotteryId: String): WaitlistEntry?

    /**
     * Conducts a recurring lottery draw round, randomly drawing up to [count] replacement winners
     * from all remaining entrants with status [WaitlistStatus.WAITING].
     *
     * @param lotteryId the identifier of the lottery.
     * @param count the number of vacant spots to fill in this lottery round.
     * @return the list of newly drawn [WaitlistEntry] records promoted to WON.
     */
    suspend fun drawLotteryRound(lotteryId: String, count: Int): List<WaitlistEntry>

    /**
     * Declines a won lottery invitation for an entrant and immediately executes a new lottery draw
     * to pick a random replacement winner from the remaining waiting pool.
     *
     * Updates [userId]'s status to [WaitlistStatus.DECLINED], then runs a random draw across all
     * waiting entrants to preserve full winner capacity.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the declining entrant.
     * @return the randomly drawn replacement [WaitlistEntry] promoted to WON, or null if no waiting entrants remain.
     */
    suspend fun autoRollOnDecline(lotteryId: String, userId: String): WaitlistEntry?

    /**
     * Reconciles expired invitations for a lottery by transitioning stale WON entries to DECLINED
     * and running a recurring lottery draw to fill all freed slots randomly from the waiting pool.
     *
     * @param lotteryId the identifier of the lottery.
     * @param expiredUserIds the list of user identifiers whose invitation acceptance deadline has lapsed.
     * @return the list of newly drawn replacement [WaitlistEntry] records.
     */
    suspend fun reconcileExpiredInvites(lotteryId: String, expiredUserIds: List<String>): List<WaitlistEntry>
}
