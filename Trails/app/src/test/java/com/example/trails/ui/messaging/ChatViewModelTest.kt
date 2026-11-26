// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: Unit tests for ChatViewModel: owner and camper share one conversation, only they can
//  open it, and failed or blank sends don't lose or post anything.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeMessageRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Message
import com.example.trails.data.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val site = TestCampsites.HINTON_ID
    private val camper = TestUsers.ENTRANT_ID
    private val owner = TestUsers.OWNER_ID

    private lateinit var messages: FakeMessageRepository
    private val campsites = FakeCampsiteRepository()
    private val users = FakeUserRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        messages = FakeMessageRepository(initial = emptyList())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun chatFor(userId: String, camperId: String = camper) =
        ChatViewModel(site, camperId, userId, campsites, messages, users)

    private fun TestScope.watch(vm: ChatViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun ChatViewModel.ready() = uiState.value as ChatUiState.Ready

    // US 02.12.01 then US 01.14.01: the owner writes, the camper sees it and replies in the same thread.
    @Test
    fun ownerMessage_reachesCamper_andCamperReplyReachesOwner() = runTest(dispatcher) {
        val ownerChat = chatFor(owner)
        val camperChat = chatFor(camper)
        watch(ownerChat)
        watch(camperChat)

        assertTrue(ownerChat.ready().iAmOwner)
        assertEquals("Priya Sharma", ownerChat.ready().otherName)
        assertEquals("Daniel Whitehorse", camperChat.ready().otherName)
        assertEquals("Wildhorse Lake Site 3", camperChat.ready().campsiteName)

        ownerChat.onDraftChange("Hi Priya, the gate code is 1234.")
        ownerChat.send()
        advanceUntilIdle()
        assertEquals("", ownerChat.draft.value)
        assertEquals(listOf("Hi Priya, the gate code is 1234."), camperChat.ready().messages.map { it.text })

        camperChat.onDraftChange("Thanks Daniel!")
        camperChat.send()
        advanceUntilIdle()

        val thread = messages.observeConversation(Message.conversationIdFor(site, camper)).first()
        assertEquals(listOf(Role.OWNER, Role.ENTRANT), thread.map { it.senderRole })
        assertEquals(listOf(owner, camper), thread.map { it.senderId })
        assertTrue(thread.all { it.ownerId == owner && it.camperId == camper && it.campsiteName == "Wildhorse Lake Site 3" })
        assertEquals(thread, ownerChat.ready().messages)
    }

    @Test
    fun someoneElse_cannotOpenTheConversation() = runTest(dispatcher) {
        val outsider = chatFor(TestUsers.ADMIN_ID)
        watch(outsider)
        assertEquals(ChatUiState.NotAllowed, outsider.uiState.value)

        outsider.onDraftChange("let me in")
        outsider.send()
        advanceUntilIdle()
        assertTrue(messages.observeForUser(camper).first().isEmpty())
    }

    @Test
    fun deletedCampsite_isNotFound() = runTest(dispatcher) {
        val vm = ChatViewModel("missing-site", camper, camper, campsites, messages, users)
        watch(vm)
        assertEquals(ChatUiState.NotFound, vm.uiState.value)
    }

    @Test
    fun blankDraft_sendsNothing() = runTest(dispatcher) {
        val vm = chatFor(camper)
        watch(vm)
        vm.onDraftChange("   ")
        vm.send()
        advanceUntilIdle()
        assertTrue(vm.ready().messages.isEmpty())
    }

    @Test
    fun failedSend_keepsDraft_andShowsError_untilTheUserTypes() = runTest(dispatcher) {
        messages.sendFailure = IOException("offline")
        val vm = chatFor(camper)
        watch(vm)
        vm.onDraftChange("Is firewood included?")
        vm.send()
        advanceUntilIdle()
        assertEquals(SendState.FAILED, vm.ready().sendState)
        assertEquals("Is firewood included?", vm.draft.value)

        vm.onDraftChange("Is firewood included??")
        advanceUntilIdle()
        assertEquals(SendState.IDLE, vm.ready().sendState)
    }

    @Test
    fun draft_isCappedAtTheMaxLength() {
        val vm = chatFor(camper)
        vm.onDraftChange("a".repeat(Message.MAX_LENGTH + 50))
        assertEquals(Message.MAX_LENGTH, vm.draft.value.length)
    }

    @Test
    fun buildChatState_rules() {
        val campsite = TestCampsites.all.first { it.id == site }
        assertEquals(ChatUiState.NotFound, buildChatState(null, camper, camper, emptyList(), emptyMap(), SendState.IDLE))
        assertEquals(ChatUiState.NotAllowed, buildChatState(campsite, camper, "", emptyList(), emptyMap(), SendState.IDLE))
        val asCamper = buildChatState(campsite, camper, camper, emptyList(), emptyMap(), SendState.IDLE) as ChatUiState.Ready
        assertEquals(false, asCamper.iAmOwner)
        assertEquals(null, asCamper.otherName)
    }
}
