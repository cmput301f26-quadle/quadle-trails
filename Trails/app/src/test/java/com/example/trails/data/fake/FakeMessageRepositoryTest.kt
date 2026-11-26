// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: Unit tests for FakeMessageRepository: one shared thread, per-user filtering, ordering.
// Outstanding issues: none.
package com.example.trails.data.fake

import com.example.trails.data.model.Message
import com.example.trails.data.model.Role
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class FakeMessageRepositoryTest {

    private val conversation = Message.conversationIdFor("site-1", "camper-1")

    private fun message(fromOwner: Boolean, text: String, at: Long? = null) = Message(
        conversationId = conversation,
        campsiteId = "site-1",
        campsiteName = "Site 1",
        ownerId = "owner-1",
        camperId = "camper-1",
        senderId = if (fromOwner) "owner-1" else "camper-1",
        senderRole = if (fromOwner) Role.OWNER else Role.ENTRANT,
        text = text,
        sentAt = at?.let { Timestamp(Date(it)) }
    )

    // US 02.12.01 + 01.14.01: the owner's message and the camper's reply land in one thread.
    @Test
    fun ownerAndCamper_shareOneConversation() = runTest {
        val repo = FakeMessageRepository(initial = emptyList())
        repo.send(message(fromOwner = true, "Welcome!", at = 1_000))
        repo.send(message(fromOwner = false, "Thanks!", at = 2_000))

        val thread = repo.observeConversation(conversation).first()
        assertEquals(listOf("Welcome!", "Thanks!"), thread.map { it.text })
        assertEquals(repo.observeForUser("owner-1").first(), repo.observeForUser("camper-1").first())
    }

    @Test
    fun observeForUser_onlyListsTheirMessages() = runTest {
        val repo = FakeMessageRepository(initial = emptyList())
        repo.send(message(fromOwner = true, "Hi"))
        assertEquals(1, repo.observeForUser("owner-1").first().size)
        assertTrue(repo.observeForUser("someone-else").first().isEmpty())
        assertTrue(repo.observeForUser("").first().isEmpty())
    }

    @Test
    fun messages_areSortedOldestFirst_andGetATime() = runTest {
        val repo = FakeMessageRepository(initial = emptyList())
        repo.send(message(fromOwner = true, "second", at = 2_000))
        repo.send(message(fromOwner = false, "first", at = 1_000))
        repo.send(message(fromOwner = true, "  latest  "))

        val thread = repo.observeConversation(conversation).first()
        assertEquals(listOf("first", "second", "latest"), thread.map { it.text })
        assertNotNull(thread.last().sentAt)
    }

    @Test
    fun invalidMessage_isRejected() = runTest {
        val repo = FakeMessageRepository(initial = emptyList())
        val error = runCatching { repo.send(message(fromOwner = true, " ")) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(repo.observeConversation(conversation).first().isEmpty())
    }

    @Test
    fun seededSample_isDanielAndPriyasBanffChat() = runTest {
        val thread = FakeMessageRepository().observeConversation(TestMessages.BANFF_CONVERSATION_ID).first()
        assertEquals(2, thread.size)
        assertTrue(thread.all { it.ownerId == TestUsers.OWNER_ID && it.camperId == TestUsers.ENTRANT_ID })
    }
}
