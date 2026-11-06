// Read/write access to lotteries.
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
// FIRESTORE IMPLEMENTATION: Skanda (skandarajeev)
package com.example.trails.data.repository

import com.example.trails.data.model.Lottery
import kotlinx.coroutines.flow.Flow

/** Storage for [Lottery] records. Screens depend on this, never on Firestore directly. */
interface LotteryRepository {
    /** Emits every lottery, and again whenever any of them changes. */
    fun observeAll(): Flow<List<Lottery>>

    /** Emits the lotteries run by [ownerId], and again on every change (US 02.04.06). */
    fun observeByOwner(ownerId: String): Flow<List<Lottery>>

    /** Emits the lottery with [id] (null if it doesn't exist), and again on every change (US 02.04.06). */
    fun observeById(id: String): Flow<Lottery?>

    /** Returns the lottery with [id], or null if it does not exist. */
    suspend fun getById(id: String): Lottery?

    /**
     * Creates the lottery if [Lottery.id] is blank (US 02.04.01), otherwise overwrites
     * the existing one (US 02.04.06).
     *
     * @return the lottery's id, newly generated when it was created.
     */
    suspend fun save(lottery: Lottery): String
}
