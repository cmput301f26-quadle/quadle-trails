// Firestore implementation of WaitlistRepository managing real-time entry streams and subcollections (Repository pattern).
// implements US 01.01.06, US 01.02.01, US 01.02.02, US 01.04.03, US 02.04.02, US 02.09.01, US 02.11.01, and Bonus Rolling Lottery
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.firestore

import com.example.trails.data.model.Collections
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.data.repository.RulesNotAcceptedException
import com.example.trails.data.repository.WaitlistFullException
import com.example.trails.data.repository.WaitlistRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Firestore implementation of [WaitlistRepository] managing waitlist documents under lotteries/{id}/entries.
 *
 * @param db the [FirebaseFirestore] database instance used for queries and operations.
 */
class FirestoreWaitlistRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : WaitlistRepository {

    // helper pointing to subcollection lotteries/{id}/entries
    private fun entriesCollection(lotteryId: String): CollectionReference =
        db.collection(Collections.LOTTERIES).document(lotteryId).collection(Collections.ENTRIES)

    // adapted from official firestore docs for snapshot listener with callbackflow
    /**
     * Observes real-time updates for all waitlist entries enrolled in a lottery.
     *
     * @param lotteryId the identifier of the lottery.
     * @return a [Flow] emitting the updated list of [WaitlistEntry] records ordered by join time.
     */
    override fun observeForLottery(lotteryId: String): Flow<List<WaitlistEntry>> = callbackFlow {
        if (lotteryId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = entriesCollection(lotteryId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            try {
                // referenced stackoverflow for sorting firestore documents in memory by date
                val entries = snapshot?.toObjects(WaitlistEntry::class.java).orEmpty()
                    .sortedWith(compareBy(nullsLast()) { it.joinedAt?.toDate() })
                trySend(entries)
            } catch (e: Exception) {
                close(e)
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Observes real-time updates for a single user's waitlist entry in a lottery.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user.
     * @return a [Flow] emitting the user's [WaitlistEntry] or null if not enrolled.
     */
    override fun observeEntry(lotteryId: String, userId: String): Flow<WaitlistEntry?> = callbackFlow {
        if (lotteryId.isBlank() || userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = entriesCollection(lotteryId).document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            try {
                val entry = if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(WaitlistEntry::class.java)
                } else {
                    null
                }
                trySend(entry)
            } catch (e: Exception) {
                close(e)
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Fetches a single user's waitlist entry from Firestore.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user.
     * @return the [WaitlistEntry] if document exists, null otherwise.
     */
    override suspend fun getEntry(lotteryId: String, userId: String): WaitlistEntry? {
        if (lotteryId.isBlank() || userId.isBlank()) return null
        val snapshot = entriesCollection(lotteryId).document(userId).get().await()
        return if (snapshot.exists()) snapshot.toObject(WaitlistEntry::class.java) else null
    }

    /**
     * Fetches all entries for a lottery from Firestore, sorted by join timestamp.
     *
     * @param lotteryId the identifier of the lottery.
     * @return a list of [WaitlistEntry] records enrolled in the lottery.
     */
    override suspend fun getEntriesForLottery(lotteryId: String): List<WaitlistEntry> {
        if (lotteryId.isBlank()) return emptyList()
        val snapshot = entriesCollection(lotteryId).get().await()
        return snapshot.toObjects(WaitlistEntry::class.java)
            .sortedWith(compareBy(nullsLast()) { it.joinedAt?.toDate() })
    }

    /**
     * Counts the total number of entries under the lottery's waitlist subcollection.
     *
     * @param lotteryId the identifier of the lottery.
     * @return the count of entries on the waitlist.
     */
    override suspend fun getEntryCount(lotteryId: String): Int {
        if (lotteryId.isBlank()) return 0
        val snapshot = entriesCollection(lotteryId).get().await()
        return snapshot.size()
    }

    /**
     * Saves or overwrites a waitlist entry in Firestore after validation.
     *
     * @param entry the [WaitlistEntry] to persist.
     * @throws IllegalArgumentException if entry validation fails.
     */
    override suspend fun save(entry: WaitlistEntry) {
        val errors = entry.validate()
        require(errors.isEmpty()) { "Invalid waitlist entry: $errors" }
        entriesCollection(entry.lotteryId).document(entry.userId).set(entry).await()
    }

    /**
     * Deletes a waitlist entry document from the lottery's entries subcollection.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user.
     */
    override suspend fun deleteEntry(lotteryId: String, userId: String) {
        if (lotteryId.isBlank() || userId.isBlank()) return
        entriesCollection(lotteryId).document(userId).delete().await()
    }

    /**
     * Enrolls an entrant onto a waitlist, checking capacity and campsite rules acceptance.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user.
     * @param userName display name of the user.
     * @param partySize number of guests requested.
     * @param rulesAccepted whether the entrant accepted campsite rules.
     * @param maxCapacity optional capacity cap on the lottery waitlist.
     * @return the saved [WaitlistEntry].
     * @throws IllegalArgumentException if lotteryId, userId are blank or partySize is less than 1.
     * @throws RulesNotAcceptedException if rules have not been accepted.
     * @throws WaitlistFullException if the waitlist has reached maximum capacity.
     */
    override suspend fun joinWaitlist(
        lotteryId: String,
        userId: String,
        userName: String,
        partySize: Int,
        rulesAccepted: Boolean,
        maxCapacity: Int?
    ): WaitlistEntry {
        if (lotteryId.isBlank()) throw IllegalArgumentException("Lottery ID cannot be blank.")
        if (userId.isBlank()) throw IllegalArgumentException("User ID cannot be blank.")
        if (partySize < 1) throw IllegalArgumentException("Party size must be at least 1.")
        if (!rulesAccepted) throw RulesNotAcceptedException("You must accept the campsite rules before joining the waitlist.")

        val existing = getEntry(lotteryId, userId)
        if (existing == null && maxCapacity != null) {
            val currentCount = getEntryCount(lotteryId)
            if (currentCount >= maxCapacity) {
                throw WaitlistFullException("The waitlist for this lottery has reached maximum capacity ($maxCapacity).")
            }
        }

        val now = Timestamp.now()
        val entry = WaitlistEntry(
            userId = userId,
            userName = userName,
            lotteryId = lotteryId,
            partySize = partySize,
            joinedAt = existing?.joinedAt ?: now,
            status = existing?.status ?: WaitlistStatus.WAITING,
            rulesAcceptedAt = now,
            rulesAccepted = true
        )
        save(entry)
        return entry
    }

    /**
     * Removes an entrant from the lottery waitlist.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user leaving.
     */
    override suspend fun leaveWaitlist(lotteryId: String, userId: String) {
        deleteEntry(lotteryId, userId)
    }

    /**
     * Updates the status of an entrant's waitlist entry in Firestore.
     *
     * @param lotteryId the identifier of the lottery.
     * @param userId the identifier of the user.
     * @param status the new [WaitlistStatus] to set.
     * @return the updated [WaitlistEntry], or null if the document does not exist.
     */
    override suspend fun updateStatus(
        lotteryId: String,
        userId: String,
        status: WaitlistStatus
    ): WaitlistEntry? {
        if (lotteryId.isBlank() || userId.isBlank()) return null
        val docRef = entriesCollection(lotteryId).document(userId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) return null
        docRef.update("status", status.name).await()
        return docRef.get().await().toObject(WaitlistEntry::class.java)
    }

    /**
     * Conducts a fair random lottery draw from all remaining entrants with status [WaitlistStatus.WAITING],
     * promoting the drawn entrant to [WaitlistStatus.WON].
     *
     * @param lotteryId the identifier of the lottery.
     * @return the newly drawn [WaitlistEntry], or null if no waiting entrants remain.
     */
    override suspend fun drawNextReplacement(lotteryId: String): WaitlistEntry? {
        return drawLotteryRound(lotteryId, 1).firstOrNull()
    }

    /**
     * Conducts a recurring lottery draw round, randomly drawing up to [count] replacement winners
     * from all remaining entrants with status [WaitlistStatus.WAITING].
     *
     * @param lotteryId the identifier of the lottery.
     * @param count the number of vacant spots to fill in this lottery round.
     * @return the list of newly drawn [WaitlistEntry] records promoted to WON.
     */
    override suspend fun drawLotteryRound(lotteryId: String, count: Int): List<WaitlistEntry> {
        if (lotteryId.isBlank() || count <= 0) return emptyList()
        val querySnapshot = entriesCollection(lotteryId)
            .whereEqualTo("status", WaitlistStatus.WAITING.name)
            .get()
            .await()

        val waitingEntries = querySnapshot.documents
            .mapNotNull { it.toObject(WaitlistEntry::class.java) }

        if (waitingEntries.isEmpty()) return emptyList()

        // Fair random lottery draw across all waiting entrants
        val drawn = waitingEntries.shuffled().take(count)
        val promoted = mutableListOf<WaitlistEntry>()

        for (entry in drawn) {
            val docRef = entriesCollection(lotteryId).document(entry.userId)
            docRef.update("status", WaitlistStatus.WON.name).await()
            docRef.get().await().toObject(WaitlistEntry::class.java)?.let {
                promoted.add(it)
            }
        }

        return promoted
    }

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
    override suspend fun autoRollOnDecline(lotteryId: String, userId: String): WaitlistEntry? {
        if (lotteryId.isBlank() || userId.isBlank()) return null
        updateStatus(lotteryId, userId, WaitlistStatus.DECLINED)
        return drawNextReplacement(lotteryId)
    }

    /**
     * Reconciles expired invitations for a lottery by transitioning stale WON entries to DECLINED
     * and running a recurring lottery draw to fill all freed slots randomly from the waiting pool.
     *
     * @param lotteryId the identifier of the lottery.
     * @param expiredUserIds the list of user identifiers whose invitation acceptance deadline has lapsed.
     * @return the list of newly drawn replacement [WaitlistEntry] records.
     */
    override suspend fun reconcileExpiredInvites(
        lotteryId: String,
        expiredUserIds: List<String>
    ): List<WaitlistEntry> {
        if (lotteryId.isBlank()) return emptyList()
        for (userId in expiredUserIds) {
            updateStatus(lotteryId, userId, WaitlistStatus.DECLINED)
        }
        return drawLotteryRound(lotteryId, expiredUserIds.size)
    }
}
