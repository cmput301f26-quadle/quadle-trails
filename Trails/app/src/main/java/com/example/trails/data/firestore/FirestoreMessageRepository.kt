// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01, 03.12.01
// PURPOSE: Firestore implementation of MessageRepository, backed by the top-level messages collection.
// Outstanding issues: none.
package com.example.trails.data.firestore

import android.util.Log
import com.example.trails.data.model.Collections
import com.example.trails.data.model.Message
import com.example.trails.data.repository.MessageRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Stores messages in Firestore. Queries only use equality filters and are sorted in memory,
 * so no composite index is needed.
 *
 * Firebase is only touched on first use, so creating this class in a JVM unit test does not
 * start Firebase.
 *
 * @param db the Firestore instance; defaults to the app's instance.
 */
class FirestoreMessageRepository(
    db: Lazy<FirebaseFirestore> = lazy { FirebaseFirestore.getInstance() }
) : MessageRepository {

    private val messages by lazy { db.value.collection(Collections.MESSAGES) }

    override fun observeConversation(conversationId: String): Flow<List<Message>> =
        if (conversationId.isBlank()) flowOf(emptyList())
        else observe(messages.whereEqualTo("conversationId", conversationId))

    override fun observeForUser(userId: String): Flow<List<Message>> {
        if (userId.isBlank()) return flowOf(emptyList())
        // Two equality queries instead of one OR query, which would need its own index.
        return combine(
            observe(messages.whereEqualTo("ownerId", userId)),
            observe(messages.whereEqualTo("camperId", userId))
        ) { asOwner, asCamper -> sortedOldestFirst((asOwner + asCamper).distinctBy { it.id }) }
    }

    override fun observeAll(): Flow<List<Message>> = observe(messages)

    override suspend fun send(message: Message): String {
        val errors = message.validate()
        require(errors.isEmpty()) { "Invalid message: $errors" }
        val doc = messages.document()
        doc.set(message.copy(id = doc.id, text = message.text.trim(), sentAt = message.sentAt ?: Timestamp.now())).await()
        return doc.id
    }

    private fun observe(query: Query): Flow<List<Message>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Couldn't observe messages", error)
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            }
            trySend(sortedOldestFirst(items))
        }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val TAG = "FirestoreMessageRepo"
    }
}

/**
 * Sorts messages oldest first; unsent timestamps go last, ties keep a stable order by id.
 *
 * @param messages the messages to sort.
 * @return the sorted list.
 */
internal fun sortedOldestFirst(messages: List<Message>): List<Message> =
    messages.sortedWith(compareBy<Message, java.util.Date?>(nullsLast()) { it.sentAt?.toDate() }.thenBy { it.id })
