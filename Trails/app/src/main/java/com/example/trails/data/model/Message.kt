// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: One chat message between a campsite's owner and a camper reserving it, stored in
//  the messages collection.
// Outstanding issues: none.
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * A message in the conversation between a campsite's owner and one camper.
 *
 * Every message about the same campsite and camper shares [conversationId], so the owner
 * (US 02.12.01) and the camper (US 01.14.01) read and write one thread.
 *
 * @property id Firestore document id.
 * @property conversationId [conversationIdFor] of [campsiteId] and [camperId].
 * @property campsiteId the campsite the conversation is about.
 * @property campsiteName campsite name when sent, so lists don't need another read.
 * @property ownerId device id of the campsite's owner.
 * @property camperId device id of the camper reserving the campsite.
 * @property senderId who wrote it; always [ownerId] or [camperId].
 * @property senderRole [Role.OWNER] when the owner wrote it, otherwise [Role.ENTRANT].
 * @property text the message body.
 * @property sentAt when it was sent.
 */
data class Message(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val campsiteId: String = "",
    val campsiteName: String = "",
    val ownerId: String = "",
    val camperId: String = "",
    val senderId: String = "",
    val senderRole: Role = Role.ENTRANT,
    val text: String = "",
    val sentAt: Timestamp? = null
) {
    /**
     * Checks a message before it is sent.
     *
     * @return error messages keyed by field name; empty if the message can be sent.
     */
    fun validate(): Map<String, String> = buildMap {
        if (campsiteId.isBlank()) put("campsiteId", "Campsite is required")
        if (ownerId.isBlank()) put("ownerId", "Owner is required")
        if (camperId.isBlank()) put("camperId", "Camper is required")
        if (campsiteId.isNotBlank() && camperId.isNotBlank() &&
            conversationId != conversationIdFor(campsiteId, camperId)
        ) put("conversationId", "Conversation doesn't match the campsite and camper")
        when (senderId) {
            "" -> put("senderId", "Sender is required")
            ownerId -> if (senderRole != Role.OWNER) put("senderRole", "The owner sends as Owner")
            camperId -> if (senderRole != Role.ENTRANT) put("senderRole", "The camper sends as Camper")
            else -> put("senderId", "Only the owner or the camper can send")
        }
        val body = text.trim()
        if (body.isEmpty()) put("text", "Message can't be empty")
        else if (body.length > MAX_LENGTH) put("text", "Keep it under $MAX_LENGTH characters")
    }

    companion object {
        /** Longest message body allowed, in characters. */
        const val MAX_LENGTH = 1000

        /**
         * The conversation for one campsite and one camper.
         *
         * @param campsiteId the campsite.
         * @param camperId the camper's device id.
         * @return an id shared by every message between that camper and the owner.
         */
        fun conversationIdFor(campsiteId: String, camperId: String): String = "${campsiteId}__$camperId"
    }
}
