// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01, 03.12.01
// PURPOSE: In-memory MessageRepository seeded from TestMessages, for unit tests and previews.
// Outstanding issues: none.
package com.example.trails.data.fake

import com.example.trails.data.firestore.sortedOldestFirst
import com.example.trails.data.model.Message
import com.example.trails.data.repository.MessageRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [MessageRepository] for unit tests and previews.
 *
 * @param initial messages to start with; defaults to [TestMessages.all].
 */
class FakeMessageRepository(initial: List<Message> = TestMessages.all) : MessageRepository {
    private val messages = MutableStateFlow(initial)
    private var nextId = 1

    /** When set, [send] throws it, so tests can check how screens handle a failed send. */
    var sendFailure: Exception? = null

    override fun observeConversation(conversationId: String): Flow<List<Message>> =
        messages.map { list -> sortedOldestFirst(list.filter { it.conversationId == conversationId }) }

    override fun observeForUser(userId: String): Flow<List<Message>> =
        messages.map { list ->
            if (userId.isBlank()) emptyList()
            else sortedOldestFirst(list.filter { it.ownerId == userId || it.camperId == userId })
        }

    override fun observeAll(): Flow<List<Message>> = messages.map(::sortedOldestFirst)

    override suspend fun send(message: Message): String {
        sendFailure?.let { throw it }
        val errors = message.validate()
        require(errors.isEmpty()) { "Invalid message: $errors" }
        val id = "fake-message-${nextId++}"
        messages.update { it + message.copy(id = id, text = message.text.trim(), sentAt = message.sentAt ?: Timestamp.now()) }
        return id
    }
}
