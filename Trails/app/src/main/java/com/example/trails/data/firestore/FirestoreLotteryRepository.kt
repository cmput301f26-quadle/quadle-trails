// Firestore-backed LotteryRepository for the lotteries collection.
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Outstanding issues:
//  - A listener error closes the flow; screens should show an error state.
//  - Switch AppContainer to this together with the Firestore campsite repository, so a
//    campsite's activeLotteryId never points at a lottery that only existed in memory.
package com.example.trails.data.firestore

import com.example.trails.data.model.Collections
import com.example.trails.data.model.Lottery
import com.example.trails.data.repository.LotteryRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Reads and writes [Lottery] documents in the [Collections.LOTTERIES] Firestore collection.
 * Waiting list entries live in each lottery's [Collections.ENTRIES] subcollection and are
 * handled by the waitlist repository.
 *
 * @param db the Firestore instance; defaults to the app's default instance.
 */
class FirestoreLotteryRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LotteryRepository {

    private val lotteries = db.collection(Collections.LOTTERIES)

    override fun observeAll(): Flow<List<Lottery>> = observe(lotteries)

    override fun observeByOwner(ownerId: String): Flow<List<Lottery>> =
        observe(lotteries.whereEqualTo("ownerId", ownerId))

    override fun observeById(id: String): Flow<Lottery?> = callbackFlow {
        val registration = lotteries.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Lottery::class.java))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getById(id: String): Lottery? =
        lotteries.document(id).get().await().toObject(Lottery::class.java)

    override suspend fun save(lottery: Lottery): String {
        val doc = if (lottery.id.isBlank()) lotteries.document() else lotteries.document(lottery.id)
        doc.set(lottery).await() // the @DocumentId field is not written into the document
        return doc.id
    }

    /** Firestore's delete already succeeds when the document doesn't exist. */
    override suspend fun delete(id: String) {
        lotteries.document(id).delete().await()
    }

    /** Turns a Firestore query into a Flow that re-emits on every change. */
    private fun observe(query: Query): Flow<List<Lottery>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(Lottery::class.java).orEmpty())
        }
        awaitClose { registration.remove() }
    }
}
