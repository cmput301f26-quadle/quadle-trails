// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01, 03.12.01
// PURPOSE: Read and write access to owner–camper chat messages.
//  Implemented by FirestoreMessageRepository (used by the app) and FakeMessageRepository (tests).
// Outstanding issues: none.
package com.example.trails.data.repository

import com.example.trails.data.model.Message
import kotlinx.coroutines.flow.Flow

/** Storage for [Message]s. Every flow lists messages oldest first. */
interface MessageRepository {

    /**
     * Observes one conversation.
     *
     * @param conversationId [Message.conversationIdFor] of the campsite and camper.
     * @return a flow that emits the conversation's messages whenever one is added.
     */
    fun observeConversation(conversationId: String): Flow<List<Message>>

    /**
     * Observes every message a user is part of, as the owner or as the camper.
     *
     * @param userId the user's device id.
     * @return a flow that emits the user's messages whenever one is added.
     */
    fun observeForUser(userId: String): Flow<List<Message>>

    /**
     * Observes every message in the app, for the admin's read-only log (US 03.12.01).
     *
     * @return a flow that emits all messages whenever one is added.
     */
    fun observeAll(): Flow<List<Message>>

    /**
     * Sends a message. A blank [Message.sentAt] is set to now.
     *
     * @param message the message; [Message.validate] must pass.
     * @return the new message's id.
     * @throws IllegalArgumentException if the message is invalid.
     * @throws Exception if the write fails.
     */
    suspend fun send(message: Message): String
}
