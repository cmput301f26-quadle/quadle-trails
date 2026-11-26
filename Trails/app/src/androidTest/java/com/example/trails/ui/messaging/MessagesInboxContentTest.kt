// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: UI tests for the Messages tab: conversations, guests to message, owners to message.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestMessages
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Role
import com.example.trails.ui.shell.MessagesTags
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MessagesInboxContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val names = TestUsers.all.associate { it.deviceId to it.name }
    private val morganAtHinton =
        MessageContact(TestCampsites.HINTON_ID, "Wildhorse Lake Site 3", TestUsers.ADMIN_ID, "Morgan Lee", GuestStatus.WAITLISTED)

    private fun show(state: MessagesInboxUiState, onOpen: (String, String) -> Unit = { _, _ -> }) {
        composeRule.setContent { TrailsTheme { MessagesInboxContent(state, onOpen) } }
    }

    // US 02.12.01: the owner sees the existing conversation and a guest they can message.
    @Test
    fun owner_seesConversationsAndGuests() {
        val inbox = buildInboxState(TestUsers.OWNER_ID, Role.OWNER, TestMessages.all, listOf(morganAtHinton), names)
        show(MessagesInboxUiState.Loaded(Role.OWNER, inbox))

        composeRule.onNodeWithTag(MessagesTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Conversations").assertIsDisplayed()
        composeRule.onNodeWithText("Priya Sharma").assertIsDisplayed()
        composeRule.onNodeWithText("Thanks Daniel! Is there firewood on site?").assertIsDisplayed()
        composeRule.onNodeWithText("Your guests").assertIsDisplayed()
        composeRule.onNodeWithText("Morgan Lee").assertIsDisplayed()
        composeRule.onNodeWithText("Wildhorse Lake Site 3 · On the waitlist").assertIsDisplayed()
    }

    @Test
    fun tappingAGuest_opensTheirChat() {
        var opened: Pair<String, String>? = null
        val inbox = MessagesInboxState(contacts = listOf(morganAtHinton))
        show(MessagesInboxUiState.Loaded(Role.OWNER, inbox)) { site, camper -> opened = site to camper }

        composeRule.onNodeWithText("Message").performClick()
        assertEquals(TestCampsites.HINTON_ID to TestUsers.ADMIN_ID, opened)
    }

    // US 01.14.01: the camper sees the owner of a campsite they're reserving and opens that chat.
    @Test
    fun camper_seesOwnersToMessage_andOpensTheSameConversation() {
        var opened: Pair<String, String>? = null
        val owner = MessageContact(TestCampsites.JASPER_ID, "Whistlers Riverside Site 7", TestUsers.ENTRANT_ID, "Daniel Whitehorse", GuestStatus.RESERVED)
        val inbox = buildInboxState(TestUsers.ENTRANT_ID, Role.ENTRANT, TestMessages.all, listOf(owner), names)
        show(MessagesInboxUiState.Loaded(Role.ENTRANT, inbox)) { site, camper -> opened = site to camper }

        composeRule.onNodeWithText("Message a campsite owner").assertIsDisplayed()
        composeRule.onNodeWithText("Whistlers Riverside Site 7 · Reserved").assertIsDisplayed()
        composeRule.onNodeWithText("You: Thanks Daniel! Is there firewood on site?").assertIsDisplayed()

        composeRule.onNodeWithTag(MessagesInboxTags.conversation(TestMessages.BANFF_CONVERSATION_ID)).performClick()
        assertEquals(TestCampsites.BANFF_ID to TestUsers.ENTRANT_ID, opened)
    }

    @Test
    fun nothingToList_showsTheEmptyState() {
        show(MessagesInboxUiState.Loaded(Role.ENTRANT, MessagesInboxState()))
        composeRule.onNodeWithTag(MessagesTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No messages yet").assertIsDisplayed()
    }

    @Test
    fun loading_showsASpinner() {
        show(MessagesInboxUiState.Loading)
        composeRule.onNodeWithTag(MessagesInboxTags.LOADING).assertIsDisplayed()
    }
}
