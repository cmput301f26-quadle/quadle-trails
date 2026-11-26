package com.example.trails.data.firestore

import com.example.trails.data.model.*
import com.example.trails.data.repository.LotteryCollaborationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** US 02.04.08: consent and all campsite links are committed together. */
class FirestoreLotteryCollaborationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val clock: () -> Long = System::currentTimeMillis
) : LotteryCollaborationRepository {
    private val invitations get() = db.collection("lotteryInvitations")
    private val lotteries get() = db.collection(Collections.LOTTERIES)
    private val campsites get() = db.collection(Collections.CAMPSITES)

    override fun observeIncoming(ownerId: String) = observe(invitations.whereEqualTo("inviteeId", ownerId))
    override fun observeForLottery(lotteryId: String) = observe(invitations.whereEqualTo("lotteryId", lotteryId))

    override suspend fun invite(lotteryId: String, actorId: String, campsiteIds: List<String>) {
        require(campsiteIds.isNotEmpty() && campsiteIds.size <= 20)
        val parent = lotteries.document(lotteryId)
        db.runTransaction { tx ->
            val lottery = tx.get(parent).toObject(Lottery::class.java) ?: error("Lottery no longer exists.")
            val sites = readSites(tx, campsiteIds)
            validateInvitation(lottery, actorId, sites, clock())
            checkAvailable(tx, sites, lotteryId)
            val owner = sites.first().ownerId
            val id = UUID.nameUUIDFromBytes("$lotteryId:$owner".toByteArray()).toString()
            val ref = invitations.document(id)
            val previous = tx.get(ref).toObject(LotteryInvitation::class.java)
            require(previous?.status != "PENDING") { "This owner already has a pending invitation." }
            tx.set(ref, LotteryInvitation(
                lotteryId = lotteryId, lotteryName = lottery.name, inviterId = actorId,
                inviteeId = owner, campsiteIds = campsiteIds, campsiteNames = sites.map { it.name },
                registrationStart = lottery.registrationStart, registrationEnd = lottery.registrationEnd,
                bookingStart = lottery.bookingStart, bookingEnd = lottery.bookingEnd
            ))
            tx.update(parent, "collaborationLocked", true)
        }.await()
    }

    override suspend fun respond(invitationId: String, actorId: String, accept: Boolean) {
        val ref = invitations.document(invitationId)
        db.runTransaction { tx ->
            val invite = tx.get(ref).toObject(LotteryInvitation::class.java) ?: error("Invitation no longer exists.")
            require(invite.inviteeId == actorId) { "Only the invited owner can respond." }
            require(invite.status == "PENDING") { "This invitation has already been answered." }
            val parent = lotteries.document(invite.lotteryId)
            val lottery = tx.get(parent).toObject(Lottery::class.java) ?: error("Lottery no longer exists.")
            if (accept) {
                val sites = readSites(tx, invite.campsiteIds)
                validateAcceptance(invite, lottery, actorId, sites, clock())
                checkAvailable(tx, sites, lottery.id)
                tx.update(parent, mapOf(
                    "campsiteIds" to (lottery.campsiteIds + invite.campsiteIds).distinct(),
                    "participantOwnerIds" to (lottery.participantOwnerIds + lottery.ownerId + actorId).distinct()
                ))
                sites.forEach { tx.update(campsites.document(it.id), "activeLotteryId", lottery.id) }
            }
            tx.update(ref, "status", if (accept) "ACCEPTED" else "DECLINED")
        }.await()
    }

    private fun readSites(tx: Transaction, ids: List<String>): List<Campsite> = ids.map { id ->
        val snapshot = tx.get(campsites.document(id))
        require(snapshot.exists()) { "A selected campsite no longer exists." }
        campsiteFromDocument(id, snapshot.data.orEmpty())
    }

    private fun checkAvailable(tx: Transaction, sites: List<Campsite>, lotteryId: String) {
        sites.mapNotNull { it.activeLotteryId }.distinct().filter { it != lotteryId }.forEach { id ->
            val other = tx.get(lotteries.document(id)).toObject(Lottery::class.java)
            require(other == null || (other.registrationEnd?.toDate()?.time ?: Long.MAX_VALUE) <= clock()) {
                "A campsite is already listed in another active lottery."
            }
        }
    }

    private fun observe(query: Query): Flow<List<LotteryInvitation>> = callbackFlow {
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else trySend(snapshot?.toObjects(LotteryInvitation::class.java).orEmpty())
        }
        awaitClose { listener.remove() }
    }
}
