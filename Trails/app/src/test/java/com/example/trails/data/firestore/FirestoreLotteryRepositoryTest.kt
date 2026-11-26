// Unit tests for FirestoreLotteryRepository, with Firestore mocked by Mockito.
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.data.firestore

import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Collections
import com.example.trails.data.model.Lottery
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class FirestoreLotteryRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var lotteries: CollectionReference
    private lateinit var doc: DocumentReference
    private lateinit var repo: FirestoreLotteryRepository

    private val lotteryId = "lottery-may-long-weekend"
    private val mayLongWeekend = Lottery(
        id = lotteryId,
        ownerId = TestUsers.OWNER_ID,
        name = "May long weekend",
        campsiteIds = listOf(TestCampsites.HINTON_ID),
        numWinners = 5,
        waitlistCapacity = 100
    )

    @Before
    fun setUp() {
        db = mock(FirebaseFirestore::class.java)
        lotteries = mock(CollectionReference::class.java)
        doc = mock(DocumentReference::class.java)
        `when`(db.collection(Collections.LOTTERIES)).thenReturn(lotteries)
        `when`(lotteries.document(lotteryId)).thenReturn(doc)
        repo = FirestoreLotteryRepository(db)
    }

    // A new lottery (blank id) gets a fresh document, and that document's id comes back.
    @Test
    fun save_newLottery_createsDocumentAndReturnsItsId(): Unit = runBlocking {
        val newDoc = mock(DocumentReference::class.java)
        val draft = mayLongWeekend.copy(id = "")
        `when`(lotteries.document()).thenReturn(newDoc)
        `when`(newDoc.id).thenReturn("generated-id")
        `when`(newDoc.set(draft)).thenReturn(Tasks.forResult(null))

        assertEquals("generated-id", repo.save(draft))
        verify(newDoc).set(draft)
    }

    /** Runs [repo]'s transaction against [stored], the lottery as it is in Firestore. */
    private fun mockTransaction(stored: Lottery): Transaction {
        val tx = mock(Transaction::class.java)
        val snap = mock(DocumentSnapshot::class.java)
        `when`(doc.id).thenReturn(lotteryId)
        `when`(tx.get(doc)).thenReturn(snap)
        `when`(snap.toObject(Lottery::class.java)).thenReturn(stored)
        `when`(db.runTransaction(any<Transaction.Function<Any>>())).thenAnswer { call ->
            Tasks.forResult(call.getArgument<Transaction.Function<Any>>(0).apply(tx))
        }
        return tx
    }

    private val openLottery = mayLongWeekend.copy(registrationEnd = Timestamp(Date(System.currentTimeMillis() + 86_400_000L)))

    // Editing (US 02.04.06) only updates the form's fields, so it can't wipe a draw or accepted collaborators.
    @Test
    fun save_existingLottery_updatesOnlyTheFormFields(): Unit = runBlocking {
        val tx = mockTransaction(openLottery)
        val edited = openLottery.copy(name = "Renamed", numWinners = 3)

        assertEquals(lotteryId, repo.save(edited))
        verify(tx).update(doc, mapOf(
            "name" to "Renamed", "campsiteIds" to edited.campsiteIds,
            "registrationStart" to edited.registrationStart, "registrationEnd" to edited.registrationEnd,
            "bookingStart" to edited.bookingStart, "bookingEnd" to edited.bookingEnd,
            "numWinners" to 3, "waitlistCapacity" to edited.waitlistCapacity
        ))
    }

    // Once invitations go out (US 02.04.08) the agreed settings can't change.
    @Test
    fun save_lockedLottery_isRejected(): Unit = runBlocking {
        mockTransaction(openLottery.copy(collaborationLocked = true))
        try {
            repo.save(openLottery.copy(name = "Renamed"))
            fail("A locked lottery must not be saved")
        } catch (e: IllegalArgumentException) {
            assertEquals("This lottery's settings are locked.", e.message)
        }
    }

    @Test
    fun getById_mapsTheDocument(): Unit = runBlocking {
        val snap = mock(DocumentSnapshot::class.java)
        `when`(snap.toObject(Lottery::class.java)).thenReturn(mayLongWeekend)
        `when`(doc.get()).thenReturn(Tasks.forResult(snap))

        assertEquals("May long weekend", repo.getById(lotteryId)?.name)
    }

    @Test
    fun getById_missingDocument_isNull(): Unit = runBlocking {
        val snap = mock(DocumentSnapshot::class.java)
        `when`(snap.toObject(Lottery::class.java)).thenReturn(null)
        `when`(doc.get()).thenReturn(Tasks.forResult(snap))

        assertNull(repo.getById(lotteryId))
    }

    @Test
    fun delete_deletesTheDocument(): Unit = runBlocking {
        `when`(doc.delete()).thenReturn(Tasks.forResult(null))
        repo.delete(lotteryId)
        verify(doc).delete()
    }

    // The Lotteries tab only lists the owner's lotteries, and stops listening when it stops collecting.
    @Test
    @Suppress("UNCHECKED_CAST")
    fun observeByOwner_includesOwnAndSharedLotteries(): Unit = runBlocking {
        val reg = mock(ListenerRegistration::class.java)
        val captor = ArgumentCaptor.forClass(EventListener::class.java) as ArgumentCaptor<EventListener<QuerySnapshot>>
        val shared = mayLongWeekend.copy(id = "shared", ownerId = "someone-else", participantOwnerIds = listOf("someone-else", TestUsers.OWNER_ID))
        val unrelated = mayLongWeekend.copy(id = "unrelated", ownerId = "someone-else")
        `when`(lotteries.addSnapshotListener(captor.capture())).thenAnswer {
            val snap = mock(QuerySnapshot::class.java)
            `when`(snap.toObjects(Lottery::class.java)).thenReturn(listOf(mayLongWeekend, shared, unrelated))
            captor.value.onEvent(snap, null)
            reg
        }

        assertEquals(listOf(lotteryId, "shared"), repo.observeByOwner(TestUsers.OWNER_ID).first().map { it.id })
        verify(reg).remove()
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun observeById_emitsTheLottery(): Unit = runBlocking {
        val reg = mock(ListenerRegistration::class.java)
        val captor = ArgumentCaptor.forClass(EventListener::class.java) as ArgumentCaptor<EventListener<DocumentSnapshot>>
        `when`(doc.addSnapshotListener(captor.capture())).thenAnswer {
            val snap = mock(DocumentSnapshot::class.java)
            `when`(snap.toObject(Lottery::class.java)).thenReturn(mayLongWeekend)
            captor.value.onEvent(snap, null)
            reg
        }

        assertEquals(5, repo.observeById(lotteryId).first()?.numWinners)
        verify(reg).remove()
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun observeById_listenerError_closesFlowWithError(): Unit = runBlocking {
        val reg = mock(ListenerRegistration::class.java)
        val captor = ArgumentCaptor.forClass(EventListener::class.java) as ArgumentCaptor<EventListener<DocumentSnapshot>>
        val denied = FirebaseFirestoreException("Permission denied", FirebaseFirestoreException.Code.PERMISSION_DENIED)
        `when`(doc.addSnapshotListener(captor.capture())).thenAnswer {
            captor.value.onEvent(null, denied)
            reg
        }

        try {
            repo.observeById(lotteryId).first()
            fail("expected the listener error to close the flow")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
        verify(reg).remove()
    }
}
