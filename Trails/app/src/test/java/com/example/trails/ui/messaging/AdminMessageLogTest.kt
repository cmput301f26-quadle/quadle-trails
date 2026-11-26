// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 03.12.01
// PURPOSE: Unit tests for the admin message log: owner-sent only, newest first, search, admins only.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import com.example.trails.data.fake.FakeMessageRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Message
import com.example.trails.data.model.Role
import com.google.firebase.Timestamp
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
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AdminMessageLogTest {

    private val dispatcher = StandardTestDispatcher()
    private val names = mapOf("owner-1" to "Daniel", "priya" to "Priya", "ava" to "Ava")

    private fun msg(id: String, camper: String, fromOwner: Boolean, text: String, at: Long, site: String = "Lake Site") = Message(
        id = id,
        conversationId = Message.conversationIdFor(site, camper),
        campsiteId = site,
        campsiteName = site,
        ownerId = "owner-1",
        camperId = camper,
        senderId = if (fromOwner) "owner-1" else camper,
        senderRole = if (fromOwner) Role.OWNER else Role.ENTRANT,
        text = text,
        sentAt = Timestamp(Date(at))
    )

    private val all = listOf(
        msg("1", "priya", fromOwner = true, "Welcome Priya", 1_000),
        msg("2", "priya", fromOwner = false, "Thanks!", 2_000),
        msg("3", "ava", fromOwner = true, "Gate code is 1234", 3_000, site = "Ridge Site")
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // US 03.12.01: the log only holds owner-to-camper messages, newest first.
    @Test
    fun log_keepsOnlyOwnerMessages_newestFirst() {
        val rows = adminMessageLog(all, names)
        assertEquals(listOf("3", "1"), rows.map { it.id })
        assertEquals("Daniel", rows.first().ownerName)
        assertEquals("Ava", rows.first().camperName)
        assertEquals("Ridge Site", rows.first().campsiteName)
    }

    @Test
    fun log_ignoresMessagesThatClaimOwnerRoleFromSomeoneElse() {
        val spoofed = msg("4", "priya", fromOwner = false, "fake", 4_000).copy(senderRole = Role.OWNER)
        assertEquals(listOf("3", "1"), adminMessageLog(all + spoofed, names).map { it.id })
    }

    @Test
    fun search_matchesTextCampsiteOrNames_ignoringCase() {
        assertEquals(listOf("3"), adminMessageLog(all, names, "gate CODE").map { it.id })
        assertEquals(listOf("3"), adminMessageLog(all, names, "ridge").map { it.id })
        assertEquals(listOf("1"), adminMessageLog(all, names, "priya").map { it.id })
        assertEquals(listOf("3", "1"), adminMessageLog(all, names, "daniel").map { it.id })
        assertTrue(adminMessageLog(all, names, "thanks").isEmpty())
        assertEquals(2, adminMessageLog(all, names, "   ").size)
    }

    private fun TestScope.logFor(userId: String, messages: FakeMessageRepository): AdminMessageLogViewModel {
        val vm = AdminMessageLogViewModel(userId, messages, FakeUserRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        return vm
    }

    @Test
    fun admin_seesTheLog_andCanSearchIt() = runTest(dispatcher) {
        val vm = logFor(TestUsers.ADMIN_ID, FakeMessageRepository())
        val loaded = vm.uiState.value as AdminMessageLogUiState.Loaded
        assertEquals(1, loaded.totalCount)
        assertEquals("Daniel Whitehorse", loaded.rows.single().ownerName)
        assertEquals("Priya Sharma", loaded.rows.single().camperName)

        vm.onQueryChange("nothing like this")
        advanceUntilIdle()
        val searched = vm.uiState.value as AdminMessageLogUiState.Loaded
        assertTrue(searched.rows.isEmpty())
        assertEquals(1, searched.totalCount)
    }

    @Test
    fun newOwnerMessage_appearsLive() = runTest(dispatcher) {
        val messages = FakeMessageRepository(initial = emptyList())
        val vm = logFor(TestUsers.ADMIN_ID, messages)
        assertEquals(0, (vm.uiState.value as AdminMessageLogUiState.Loaded).totalCount)

        messages.send(msg("", "priya", fromOwner = true, "Hello", 5_000).copy(ownerId = "owner-1"))
        advanceUntilIdle()
        assertEquals(listOf("Hello"), (vm.uiState.value as AdminMessageLogUiState.Loaded).rows.map { it.text })
    }

    // Owners and campers can't open the log, even by route.
    @Test
    fun nonAdmins_areNotAllowed() = runTest(dispatcher) {
        assertEquals(AdminMessageLogUiState.NotAllowed, logFor(TestUsers.OWNER_ID, FakeMessageRepository()).uiState.value)
        assertEquals(AdminMessageLogUiState.NotAllowed, logFor(TestUsers.ENTRANT_ID, FakeMessageRepository()).uiState.value)
        assertEquals(AdminMessageLogUiState.NotAllowed, logFor("unknown", FakeMessageRepository()).uiState.value)
    }
}
