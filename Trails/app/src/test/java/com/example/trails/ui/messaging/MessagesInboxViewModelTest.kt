// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: Unit tests for MessagesInboxViewModel: owners see their guests, campers see the owners
//  they can message, and a started conversation moves into Conversations.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeMessageRepository
import com.example.trails.data.fake.FakeReservationRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestLotteries
import com.example.trails.data.fake.TestReservations
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Message
import com.example.trails.data.model.Role
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesInboxViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var messages: FakeMessageRepository
    private lateinit var users: FakeUserRepository
    private val campsites = FakeCampsiteRepository()

    // Priya holds the Banff and Jasper sample reservations; Morgan is on the Hinton lottery's waitlist.
    private val reservations = FakeReservationRepository(TestReservations.all)
    private val waitlist = FakeWaitlistRepository(
        listOf(
            WaitlistEntry(
                userId = TestUsers.ADMIN_ID,
                userName = "Morgan Lee",
                lotteryId = TestLotteries.SCHEDULED_LOTTERY_ID,
                status = WaitlistStatus.WAITING,
                rulesAccepted = true
            )
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        messages = FakeMessageRepository(initial = emptyList())
        users = FakeUserRepository()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.inboxFor(userId: String): MessagesInboxViewModel {
        val vm = MessagesInboxViewModel(userId, messages, users, campsites, reservations, waitlist)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        return vm
    }

    private fun MessagesInboxViewModel.loaded() = uiState.value as MessagesInboxUiState.Loaded

    // US 02.12.01: Daniel (owner mode) sees every guest reserving one of his campsites.
    @Test
    fun owner_seesGuestsFromReservationsAndWaitlists() = runTest(dispatcher) {
        val inbox = inboxFor(TestUsers.OWNER_ID).loaded()
        assertEquals(Role.OWNER, inbox.mode)
        assertTrue(inbox.inbox.conversations.isEmpty())
        assertEquals(
            setOf(
                Triple(TestCampsites.BANFF_ID, TestUsers.ENTRANT_ID, GuestStatus.RESERVED),
                Triple(TestCampsites.JASPER_ID, TestUsers.ENTRANT_ID, GuestStatus.RESERVED),
                Triple(TestCampsites.HINTON_ID, TestUsers.ADMIN_ID, GuestStatus.WAITLISTED)
            ),
            inbox.inbox.contacts.map { Triple(it.campsiteId, it.camperId, it.status) }.toSet()
        )
        assertTrue(inbox.inbox.contacts.any { it.otherName == "Priya Sharma" })
    }

    // US 01.14.01: Priya (camper mode) sees the owner of each campsite she's reserving.
    @Test
    fun camper_seesOwnersOfCampsitesTheyReserve() = runTest(dispatcher) {
        val inbox = inboxFor(TestUsers.ENTRANT_ID).loaded()
        assertEquals(Role.ENTRANT, inbox.mode)
        assertEquals(
            listOf(TestCampsites.BANFF_ID, TestCampsites.JASPER_ID).sorted(),
            inbox.inbox.contacts.map { it.campsiteId }.sorted()
        )
        assertTrue(inbox.inbox.contacts.all { it.otherName == "Daniel Whitehorse" && it.camperId == TestUsers.ENTRANT_ID })
    }

    @Test
    fun sentMessage_movesTheContactIntoConversations_onBothSides() = runTest(dispatcher) {
        val ownerInbox = inboxFor(TestUsers.OWNER_ID)
        val camperInbox = inboxFor(TestUsers.ENTRANT_ID)

        messages.send(
            Message(
                conversationId = Message.conversationIdFor(TestCampsites.BANFF_ID, TestUsers.ENTRANT_ID),
                campsiteId = TestCampsites.BANFF_ID,
                campsiteName = "Tunnel Mountain Site 14",
                ownerId = TestUsers.OWNER_ID,
                camperId = TestUsers.ENTRANT_ID,
                senderId = TestUsers.OWNER_ID,
                senderRole = Role.OWNER,
                text = "See you soon!"
            )
        )
        advanceUntilIdle()

        val ownerView = ownerInbox.loaded().inbox
        assertEquals(listOf("Priya Sharma"), ownerView.conversations.map { it.otherName })
        assertTrue(ownerView.conversations.single().lastFromMe)
        assertTrue(ownerView.contacts.none { it.campsiteId == TestCampsites.BANFF_ID && it.camperId == TestUsers.ENTRANT_ID })

        val camperView = camperInbox.loaded().inbox
        assertEquals("See you soon!", camperView.conversations.single().lastText)
        assertEquals(listOf(TestCampsites.JASPER_ID), camperView.contacts.map { it.campsiteId })
    }

    @Test
    fun admin_withNoConversations_hasAnEmptyInbox() = runTest(dispatcher) {
        val inbox = inboxFor(TestUsers.ADMIN_ID).loaded()
        assertEquals(Role.ADMIN, inbox.mode)
        assertTrue(inbox.inbox.isEmpty)
    }

    @Test
    fun switchingToOwnerMode_showsGuestsInstead() = runTest(dispatcher) {
        users.updateActiveMode(TestUsers.OWNER_ID, Role.ENTRANT)
        val vm = inboxFor(TestUsers.OWNER_ID)
        assertTrue(vm.loaded().inbox.contacts.isEmpty())

        users.updateActiveMode(TestUsers.OWNER_ID, Role.OWNER)
        advanceUntilIdle()
        assertEquals(Role.OWNER, vm.loaded().mode)
        assertEquals(3, vm.loaded().inbox.contacts.size)
    }
}
