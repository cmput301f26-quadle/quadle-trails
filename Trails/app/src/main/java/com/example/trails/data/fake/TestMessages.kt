// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: Sample conversation between the sample owner and the sample camper about the
//  Banff reservation, for previews and the fake repository.
// Outstanding issues: none.
package com.example.trails.data.fake

import com.example.trails.data.model.Message
import com.example.trails.data.model.Role

/** Sample messages for previews and unit tests. */
object TestMessages {
    /** Daniel and Priya's conversation about Tunnel Mountain Site 14. */
    val BANFF_CONVERSATION_ID = Message.conversationIdFor(TestCampsites.BANFF_ID, TestUsers.ENTRANT_ID)

    private fun banff(id: String, fromOwner: Boolean, text: String, daysAgo: Long) = Message(
        id = id,
        conversationId = BANFF_CONVERSATION_ID,
        campsiteId = TestCampsites.BANFF_ID,
        campsiteName = "Tunnel Mountain Site 14",
        ownerId = TestUsers.OWNER_ID,
        camperId = TestUsers.ENTRANT_ID,
        senderId = if (fromOwner) TestUsers.OWNER_ID else TestUsers.ENTRANT_ID,
        senderRole = if (fromOwner) Role.OWNER else Role.ENTRANT,
        text = text,
        sentAt = daysFromNow(-daysAgo)
    )

    val all = listOf(
        banff("msg-banff-1", fromOwner = true, "Hi Priya, thanks for booking! Check-in is after 2pm.", daysAgo = 2),
        banff("msg-banff-2", fromOwner = false, "Thanks Daniel! Is there firewood on site?", daysAgo = 1)
    )
}
