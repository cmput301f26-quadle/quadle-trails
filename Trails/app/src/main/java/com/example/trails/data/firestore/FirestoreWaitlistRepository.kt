// firestore backed waitlist repository for entries under lotteries/{id}/entries
// owner: chitkarsh (chitkarshrathi) - waitlist
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

class FirestoreWaitlistRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : WaitlistRepository {

    // helper pointing to subcollection lotteries/{id}/entries
    private fun entriesCollection(lotteryId: String): CollectionReference =
        db.collection(Collections.LOTTERIES).document(lotteryId).collection(Collections.ENTRIES)

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
            val entries = snapshot?.toObjects(WaitlistEntry::class.java).orEmpty()
                .sortedWith(compareBy(nullsLast()) { it.joinedAt?.toDate() })
            trySend(entries)
        }
        awaitClose { registration.remove() }
    }

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
            val entry = if (snapshot != null && snapshot.exists()) {
                snapshot.toObject(WaitlistEntry::class.java)
            } else {
                null
            }
            trySend(entry)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getEntry(lotteryId: String, userId: String): WaitlistEntry? {
        if (lotteryId.isBlank() || userId.isBlank()) return null
        val snapshot = entriesCollection(lotteryId).document(userId).get().await()
        return if (snapshot.exists()) snapshot.toObject(WaitlistEntry::class.java) else null
    }

    override suspend fun getEntriesForLottery(lotteryId: String): List<WaitlistEntry> {
        if (lotteryId.isBlank()) return emptyList()
        val snapshot = entriesCollection(lotteryId).get().await()
        return snapshot.toObjects(WaitlistEntry::class.java)
            .sortedWith(compareBy(nullsLast()) { it.joinedAt?.toDate() })
    }

    override suspend fun getEntryCount(lotteryId: String): Int {
        if (lotteryId.isBlank()) return 0
        val snapshot = entriesCollection(lotteryId).get().await()
        return snapshot.size()
    }

    override suspend fun save(entry: WaitlistEntry) {
        val errors = entry.validate()
        require(errors.isEmpty()) { "invalid waitlist entry: $errors" }
        entriesCollection(entry.lotteryId).document(entry.userId).set(entry).await()
    }

    override suspend fun deleteEntry(lotteryId: String, userId: String) {
        if (lotteryId.isBlank() || userId.isBlank()) return
        entriesCollection(lotteryId).document(userId).delete().await()
    }

    override suspend fun joinWaitlist(
        lotteryId: String,
        userId: String,
        userName: String,
        partySize: Int,
        rulesAccepted: Boolean,
        maxCapacity: Int?
    ): WaitlistEntry {
        if (lotteryId.isBlank()) throw IllegalArgumentException("lottery id cannot be blank")
        if (userId.isBlank()) throw IllegalArgumentException("user id cannot be blank")
        if (partySize < 1) throw IllegalArgumentException("party size must be at least 1")
        if (!rulesAccepted) throw RulesNotAcceptedException("you must accept the campsite rules before joining the waitlist")

        val existing = getEntry(lotteryId, userId)
        if (existing == null && maxCapacity != null) {
            val currentCount = getEntryCount(lotteryId)
            if (currentCount >= maxCapacity) {
                throw WaitlistFullException("the waitlist for this lottery has reached maximum capacity ($maxCapacity)")
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

    override suspend fun leaveWaitlist(lotteryId: String, userId: String) {
        deleteEntry(lotteryId, userId)
    }
}
