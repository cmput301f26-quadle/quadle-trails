// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: UI tests for the chat screen: both sides' bubbles, sending, and blocked states.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeMessageRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestMessages
import com.example.trails.data.fake.TestUsers
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ready(myId: String, sendState: SendState = SendState.IDLE) = ChatUiState.Ready(
        campsiteName = "Tunnel Mountain Site 14",
        ownerId = TestUsers.OWNER_ID,
        myId = myId,
        iAmOwner = myId == TestUsers.OWNER_ID,
        otherName = if (myId == TestUsers.OWNER_ID) "Priya Sharma" else "Daniel Whitehorse",
        messages = TestMessages.all,
        sendState = sendState
    )

    private fun show(state: ChatUiState, draft: String = "", onSend: () -> Unit = {}, onDraft: (String) -> Unit = {}) {
        composeRule.setContent {
            TrailsTheme { ChatContent(state, draft, onDraft, onSend, onBack = {}) }
        }
    }

    // US 02.12.01: the owner sees their own message on the right and the camper's reply on the left.
    @Test
    fun owner_seesBothSidesOfTheConversation() {
        show(ready(TestUsers.OWNER_ID))
        composeRule.onNodeWithText("Priya Sharma").assertIsDisplayed()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ChatTags.MINE).assertCountEquals(1)
        composeRule.onAllNodesWithTag(ChatTags.THEIRS).assertCountEquals(1)
        composeRule.onNodeWithText("Thanks Daniel! Is there firewood on site?").assertIsDisplayed()
    }

    // US 01.14.01: the same conversation from the camper's side flips which bubbles are theirs.
    @Test
    fun camper_seesTheSameConversationFlipped() {
        show(ready(TestUsers.ENTRANT_ID))
        composeRule.onNodeWithText("Daniel Whitehorse").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ChatTags.MINE).assertCountEquals(1)
        composeRule.onNodeWithText("Hi Priya, thanks for booking! Check-in is after 2pm.").assertIsDisplayed()
    }

    @Test
    fun send_isDisabledUntilTextIsTyped_thenSends() {
        var draft by mutableStateOf("")
        var sent = 0
        composeRule.setContent {
            TrailsTheme {
                ChatContent(ready(TestUsers.ENTRANT_ID), draft, { draft = it }, { sent++ }, onBack = {})
            }
        }
        composeRule.onNodeWithTag(ChatTags.SEND).assertIsNotEnabled()
        composeRule.onNodeWithTag(ChatTags.INPUT).performTextInput("See you Friday")
        composeRule.onNodeWithTag(ChatTags.SEND).assertIsEnabled().performClick()
        assertEquals(1, sent)
        assertEquals("See you Friday", draft)
    }

    @Test
    fun failedSend_showsError() {
        show(ready(TestUsers.ENTRANT_ID, SendState.FAILED), draft = "hello")
        composeRule.onNodeWithTag(ChatTags.SEND_ERROR).assertIsDisplayed()
    }

    @Test
    fun whileSending_buttonIsDisabled() {
        show(ready(TestUsers.ENTRANT_ID, SendState.SENDING), draft = "hello")
        composeRule.onNodeWithTag(ChatTags.SEND).assertIsNotEnabled()
    }

    @Test
    fun emptyConversation_showsAPrompt() {
        show(ready(TestUsers.OWNER_ID).copy(messages = emptyList()))
        composeRule.onNodeWithTag(ChatTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No messages yet. Say hello to your guest.").assertIsDisplayed()
    }

    @Test
    fun outsider_seesNoMessagesOrInput() {
        show(ChatUiState.NotAllowed)
        composeRule.onNodeWithTag(ChatTags.NOT_ALLOWED).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTags.INPUT).assertDoesNotExist()
    }

    @Test
    fun deletedCampsite_showsNotFound() {
        show(ChatUiState.NotFound)
        composeRule.onNodeWithTag(ChatTags.NOT_FOUND).assertIsDisplayed()
    }

    // End to end on fakes: typing and tapping Send stores the message and shows it as mine.
    @Test
    fun realViewModel_sendingAddsMyBubble() {
        val messages = FakeMessageRepository(initial = emptyList())
        composeRule.setContent {
            val vm = remember {
                ChatViewModel(
                    TestCampsites.HINTON_ID, TestUsers.ENTRANT_ID, TestUsers.OWNER_ID,
                    FakeCampsiteRepository(), messages, FakeUserRepository()
                )
            }
            TrailsTheme {
                val state by vm.uiState.collectAsState()
                val draft by vm.draft.collectAsState()
                ChatContent(state, draft, vm::onDraftChange, vm::send, onBack = {})
            }
        }
        composeRule.onNodeWithText("Priya Sharma").assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTags.INPUT).performTextInput("Welcome to Wildhorse!")
        composeRule.onNodeWithTag(ChatTags.SEND).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(ChatTags.MINE).fetchSemanticsNodes().size == 1 }
        composeRule.onNodeWithText("Welcome to Wildhorse!").assertIsDisplayed()
    }
}
