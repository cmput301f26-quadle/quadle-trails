// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: Unit tests for Message.validate and the shared conversation id.
// Outstanding issues: none.
package com.example.trails.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTest {

    private val fromOwner = Message(
        conversationId = Message.conversationIdFor("site-1", "camper-1"),
        campsiteId = "site-1",
        ownerId = "owner-1",
        camperId = "camper-1",
        senderId = "owner-1",
        senderRole = Role.OWNER,
        text = "Hi there"
    )

    // The owner and the camper get the same conversation id, and each camper gets their own.
    @Test
    fun conversationId_isSharedPerCampsiteAndCamper() {
        assertEquals(Message.conversationIdFor("site-1", "camper-1"), Message.conversationIdFor("site-1", "camper-1"))
        assertNotEquals(Message.conversationIdFor("site-1", "camper-1"), Message.conversationIdFor("site-1", "camper-2"))
        assertNotEquals(Message.conversationIdFor("site-1", "camper-1"), Message.conversationIdFor("site-2", "camper-1"))
    }

    @Test
    fun validMessages_fromOwnerAndCamper_pass() {
        assertTrue(fromOwner.validate().isEmpty())
        val fromCamper = fromOwner.copy(senderId = "camper-1", senderRole = Role.ENTRANT)
        assertTrue(fromCamper.validate().isEmpty())
    }

    @Test
    fun blankOrTooLongText_fails() {
        assertTrue("text" in fromOwner.copy(text = "   ").validate())
        assertTrue("text" in fromOwner.copy(text = "a".repeat(Message.MAX_LENGTH + 1)).validate())
        assertTrue(fromOwner.copy(text = "a".repeat(Message.MAX_LENGTH)).validate().isEmpty())
    }

    // Only the two people in the conversation can send, each with their own role.
    @Test
    fun outsiderOrWrongRole_fails() {
        assertTrue("senderId" in fromOwner.copy(senderId = "stranger").validate())
        assertTrue("senderRole" in fromOwner.copy(senderRole = Role.ENTRANT).validate())
        assertTrue("senderRole" in fromOwner.copy(senderId = "camper-1", senderRole = Role.OWNER).validate())
    }

    @Test
    fun missingIdsOrWrongConversation_fails() {
        val errors = Message(text = "hi").validate()
        assertTrue(errors.keys.containsAll(listOf("campsiteId", "ownerId", "camperId", "senderId")))
        assertTrue("conversationId" in fromOwner.copy(conversationId = "other").validate())
    }
}
