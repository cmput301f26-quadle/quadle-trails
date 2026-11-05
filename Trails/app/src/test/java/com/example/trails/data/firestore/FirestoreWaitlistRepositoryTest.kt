// unit tests for firestore waitlist repository
// owner: chitkarsh (chitkarshrathi) - waitlist
// used antigravity to generate these mockito test cases
package com.example.trails.data.firestore

import com.example.trails.data.model.Collections
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.data.repository.RulesNotAcceptedException
import com.example.trails.data.repository.WaitlistFullException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Date

class FirestoreWaitlistRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var lotteriesCol: CollectionReference
    private lateinit var lotteryDoc: DocumentReference
    private lateinit var entriesCol: CollectionReference
    private lateinit var entryDoc: DocumentReference
    private lateinit var repo: FirestoreWaitlistRepository

    private val lotteryId = "test-lottery-id"
    private val userId = "test-user-id"

    // used agent for the repetitive setup here
    @Before
    fun setUp() {
        db = mock(FirebaseFirestore::class.java)
        lotteriesCol = mock(CollectionReference::class.java)
        lotteryDoc = mock(DocumentReference::class.java)
        entriesCol = mock(CollectionReference::class.java)
        entryDoc = mock(DocumentReference::class.java)

        `when`(db.collection(Collections.LOTTERIES)).thenReturn(lotteriesCol)
        `when`(lotteriesCol.document(lotteryId)).thenReturn(lotteryDoc)
        `when`(lotteryDoc.collection(Collections.ENTRIES)).thenReturn(entriesCol)
        `when`(entriesCol.document(userId)).thenReturn(entryDoc)

        repo = FirestoreWaitlistRepository(db)
    }

    // successful database writes

    @Test
    fun save_validEntry_writesToFirestore(): Unit = runBlocking {
        val entry = WaitlistEntry(
            userId = userId,
            userName = "Chitkarsh",
            lotteryId = lotteryId,
            partySize = 2,
            rulesAccepted = true
        )
        `when`(entryDoc.set(entry)).thenReturn(Tasks.forResult(null))

        repo.save(entry)
        verify(entryDoc).set(entry)
        return@runBlocking
    }

    @Test
    fun deleteEntry_validId_deletesDocument(): Unit = runBlocking {
        `when`(entryDoc.delete()).thenReturn(Tasks.forResult(null))

        repo.deleteEntry(lotteryId, userId)
        verify(entryDoc).delete()
        return@runBlocking
    }

    @Test
    fun leaveWaitlist_delegatesToDeleteEntry(): Unit = runBlocking {
        `when`(entryDoc.delete()).thenReturn(Tasks.forResult(null))

        repo.leaveWaitlist(lotteryId, userId)
        verify(entryDoc).delete()
        return@runBlocking
    }

    @Test
    fun joinWaitlist_validParams_savesEntryAndReturnsWithTimestamps(): Unit = runBlocking {
        // mock get entry returning null when user not on waitlist yet
        val missingSnap = mock(DocumentSnapshot::class.java)
        `when`(missingSnap.exists()).thenReturn(false)
        `when`(entryDoc.get()).thenReturn(Tasks.forResult(missingSnap))

        // mock count query returning 1
        val countSnap = mock(QuerySnapshot::class.java)
        `when`(countSnap.size()).thenReturn(1)
        `when`(entriesCol.get()).thenReturn(Tasks.forResult(countSnap))

        val captor = ArgumentCaptor.forClass(WaitlistEntry::class.java)
        `when`(entryDoc.set(captor.capture())).thenReturn(Tasks.forResult(null))

        val result = repo.joinWaitlist(
            lotteryId = lotteryId,
            userId = userId,
            userName = "Chitkarsh",
            partySize = 3,
            rulesAccepted = true,
            maxCapacity = 5
        )

        assertEquals(userId, result.userId)
        assertEquals(lotteryId, result.lotteryId)
        assertEquals("Chitkarsh", result.userName)
        assertEquals(3, result.partySize)
        assertEquals(WaitlistStatus.WAITING, result.status)
        assertTrue(result.rulesAccepted)
        assertNotNull(result.joinedAt)
        assertNotNull(result.rulesAcceptedAt)

        val saved = captor.value
        assertEquals(userId, saved.userId)
        assertEquals(lotteryId, saved.lotteryId)
        assertEquals(3, saved.partySize)
    }

    // full waitlists and capacity checks

    @Test
    fun joinWaitlist_waitlistAtCapacity_throwsWaitlistFullException(): Unit = runBlocking {
        // user not currently on waitlist
        val missingSnap = mock(DocumentSnapshot::class.java)
        `when`(missingSnap.exists()).thenReturn(false)
        `when`(entryDoc.get()).thenReturn(Tasks.forResult(missingSnap))

        // existing count is 5 max capacity is 5
        val countSnap = mock(QuerySnapshot::class.java)
        `when`(countSnap.size()).thenReturn(5)
        `when`(entriesCol.get()).thenReturn(Tasks.forResult(countSnap))

        try {
            repo.joinWaitlist(
                lotteryId = lotteryId,
                userId = userId,
                userName = "Chitkarsh",
                partySize = 2,
                rulesAccepted = true,
                maxCapacity = 5
            )
            fail("expected WaitlistFullException was not thrown")
        } catch (e: WaitlistFullException) {
            assertTrue(e.message?.contains("5") == true)
        }
    }

    @Test
    fun joinWaitlist_waitlistAtCapacity_existingUser_updatesSuccessfully(): Unit = runBlocking {
        // user is already on waitlist
        val existingEntry = WaitlistEntry(
            userId = userId,
            userName = "Chitkarsh",
            lotteryId = lotteryId,
            partySize = 1,
            rulesAccepted = true
        )
        val existingSnap = mock(DocumentSnapshot::class.java)
        `when`(existingSnap.exists()).thenReturn(true)
        `when`(existingSnap.toObject(WaitlistEntry::class.java)).thenReturn(existingEntry)
        `when`(entryDoc.get()).thenReturn(Tasks.forResult(existingSnap))

        val captor = ArgumentCaptor.forClass(WaitlistEntry::class.java)
        `when`(entryDoc.set(captor.capture())).thenReturn(Tasks.forResult(null))

        // should succeed even if capacity is 1
        val result = repo.joinWaitlist(
            lotteryId = lotteryId,
            userId = userId,
            userName = "Chitkarsh",
            partySize = 2,
            rulesAccepted = true,
            maxCapacity = 1
        )

        assertEquals(2, result.partySize)
    }

    // missing data and validation

    @Test
    fun getEntry_documentDoesNotExist_returnsNull(): Unit = runBlocking {
        val snap = mock(DocumentSnapshot::class.java)
        `when`(snap.exists()).thenReturn(false)
        `when`(entryDoc.get()).thenReturn(Tasks.forResult(snap))

        val result = repo.getEntry(lotteryId, userId)
        assertNull(result)
    }

    @Test
    fun getEntry_documentExists_returnsWaitlistEntry(): Unit = runBlocking {
        val entry = WaitlistEntry(userId = userId, lotteryId = lotteryId, partySize = 2, rulesAccepted = true)
        val snap = mock(DocumentSnapshot::class.java)
        `when`(snap.exists()).thenReturn(true)
        `when`(snap.toObject(WaitlistEntry::class.java)).thenReturn(entry)
        `when`(entryDoc.get()).thenReturn(Tasks.forResult(snap))

        val result = repo.getEntry(lotteryId, userId)
        assertNotNull(result)
        assertEquals(userId, result?.userId)
    }

    @Test
    fun getEntry_blankIds_returnsNullWithoutQuerying(): Unit = runBlocking {
        assertNull(repo.getEntry("", userId))
        assertNull(repo.getEntry(lotteryId, ""))
    }

    @Test
    fun getEntriesForLottery_blankLotteryId_returnsEmptyList(): Unit = runBlocking {
        assertTrue(repo.getEntriesForLottery("").isEmpty())
    }

    @Test
    fun getEntriesForLottery_validLottery_returnsEntriesSortedByJoinedAt(): Unit = runBlocking {
        val querySnap = mock(QuerySnapshot::class.java)
        val t1 = Timestamp(Date(100000))
        val t2 = Timestamp(Date(200000))
        val t3 = Timestamp(Date(300000))
        val e1 = WaitlistEntry(userId = "first", lotteryId = lotteryId, joinedAt = t1, rulesAccepted = true)
        val e2 = WaitlistEntry(userId = "second", lotteryId = lotteryId, joinedAt = t2, rulesAccepted = true)
        val e3 = WaitlistEntry(userId = "third", lotteryId = lotteryId, joinedAt = t3, rulesAccepted = true)

        // return unsorted from query snapshot
        `when`(querySnap.toObjects(WaitlistEntry::class.java)).thenReturn(listOf(e3, e1, e2))
        `when`(entriesCol.get()).thenReturn(Tasks.forResult(querySnap))

        val result = repo.getEntriesForLottery(lotteryId)
        assertEquals(listOf("first", "second", "third"), result.map { it.userId })
    }

    @Test
    fun getEntryCount_blankLotteryId_returnsZero(): Unit = runBlocking {
        assertEquals(0, repo.getEntryCount(""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun save_blankUserId_throwsIllegalArgumentException(): Unit = runBlocking {
        repo.save(WaitlistEntry(userId = "", lotteryId = lotteryId, rulesAccepted = true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun save_blankLotteryId_throwsIllegalArgumentException(): Unit = runBlocking {
        repo.save(WaitlistEntry(userId = userId, lotteryId = "", rulesAccepted = true))
    }

    @Test(expected = RulesNotAcceptedException::class)
    fun joinWaitlist_rulesNotAccepted_throwsRulesNotAcceptedException(): Unit = runBlocking {
        repo.joinWaitlist(lotteryId, userId, "Chitkarsh", 2, rulesAccepted = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun joinWaitlist_partySizeZero_throwsIllegalArgumentException(): Unit = runBlocking {
        repo.joinWaitlist(lotteryId, userId, "Chitkarsh", partySize = 0, rulesAccepted = true)
    }

    // network exceptions and failure cases

    @Test(expected = FirebaseFirestoreException::class)
    fun save_networkFailure_throwsException(): Unit = runBlocking {
        val entry = WaitlistEntry(userId = userId, lotteryId = lotteryId, rulesAccepted = true)
        val firestoreError = FirebaseFirestoreException(
            "Service unavailable",
            FirebaseFirestoreException.Code.UNAVAILABLE
        )
        `when`(entryDoc.set(entry)).thenReturn(Tasks.forException(firestoreError))

        repo.save(entry)
    }

    @Test(expected = FirebaseFirestoreException::class)
    fun getEntry_networkFailure_throwsException(): Unit = runBlocking {
        val firestoreError = FirebaseFirestoreException(
            "Network timeout",
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
        )
        `when`(entryDoc.get()).thenReturn(Tasks.forException(firestoreError))

        repo.getEntry(lotteryId, userId)
    }

    @Test(expected = FirebaseFirestoreException::class)
    fun deleteEntry_networkFailure_throwsException(): Unit = runBlocking {
        val firestoreError = FirebaseFirestoreException(
            "Permission denied",
            FirebaseFirestoreException.Code.PERMISSION_DENIED
        )
        `when`(entryDoc.delete()).thenReturn(Tasks.forException(firestoreError))

        repo.deleteEntry(lotteryId, userId)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun observeEntry_snapshotListener_emitsEntry(): Unit = runBlocking {
        val reg = mock(ListenerRegistration::class.java)
        val captor = ArgumentCaptor.forClass(EventListener::class.java) as ArgumentCaptor<EventListener<DocumentSnapshot>>
        `when`(entryDoc.addSnapshotListener(captor.capture())).thenAnswer {
            val listener = captor.value
            val snap = mock(DocumentSnapshot::class.java)
            val entry = WaitlistEntry(userId = userId, lotteryId = lotteryId, rulesAccepted = true)
            `when`(snap.exists()).thenReturn(true)
            `when`(snap.toObject(WaitlistEntry::class.java)).thenReturn(entry)
            listener.onEvent(snap, null)
            reg
        }

        val emitted = repo.observeEntry(lotteryId, userId).first()
        assertNotNull(emitted)
        assertEquals(userId, emitted?.userId)
        verify(reg).remove()
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun observeForLottery_snapshotListener_emitsEntriesSorted(): Unit = runBlocking {
        val reg = mock(ListenerRegistration::class.java)
        val captor = ArgumentCaptor.forClass(EventListener::class.java) as ArgumentCaptor<EventListener<QuerySnapshot>>
        `when`(entriesCol.addSnapshotListener(captor.capture())).thenAnswer {
            val listener = captor.value
            val querySnap = mock(QuerySnapshot::class.java)
            val t1 = Timestamp(Date(100000))
            val t2 = Timestamp(Date(200000))
            val e1 = WaitlistEntry(userId = "u1", lotteryId = lotteryId, joinedAt = t1, rulesAccepted = true)
            val e2 = WaitlistEntry(userId = "u2", lotteryId = lotteryId, joinedAt = t2, rulesAccepted = true)
            `when`(querySnap.toObjects(WaitlistEntry::class.java)).thenReturn(listOf(e2, e1))
            listener.onEvent(querySnap, null)
            reg
        }

        val emitted = repo.observeForLottery(lotteryId).first()
        assertEquals(listOf("u1", "u2"), emitted.map { it.userId })
        verify(reg).remove()
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun observeEntry_snapshotListenerError_closesFlowWithError(): Unit = runBlocking {
        val reg = mock(ListenerRegistration::class.java)
        val captor = ArgumentCaptor.forClass(EventListener::class.java) as ArgumentCaptor<EventListener<DocumentSnapshot>>
        val firestoreError = FirebaseFirestoreException("Permission denied", FirebaseFirestoreException.Code.PERMISSION_DENIED)

        `when`(entryDoc.addSnapshotListener(captor.capture())).thenAnswer {
            val listener = captor.value
            listener.onEvent(null, firestoreError)
            reg
        }

        try {
            repo.observeEntry(lotteryId, userId).first()
            fail("expected exception was not thrown")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
        verify(reg).remove()
    }
}
