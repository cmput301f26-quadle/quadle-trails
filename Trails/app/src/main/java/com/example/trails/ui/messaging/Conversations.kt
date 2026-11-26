// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: Pure rules behind the Messages tab and the chat: who an owner's guests are, which
//  owners a camper can message, conversation summaries and message times.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Message
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.model.Role
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** How far a camper is in reserving a campsite; later values win when both apply. */
enum class GuestStatus {
    /** On the campsite's lottery waitlist. */
    WAITLISTED,
    /** Drawn in the lottery and invited to book. */
    WON,
    /** Holds an active reservation. */
    RESERVED
}

/**
 * Someone the user can start a conversation with: a guest for an owner, or the owner of a
 * campsite the camper is reserving.
 *
 * @property campsiteId the campsite the conversation is about.
 * @property campsiteName its name.
 * @property camperId the camper in the conversation.
 * @property otherName the other person's name, or null if unknown.
 * @property status how far the camper is in reserving.
 */
data class MessageContact(
    val campsiteId: String,
    val campsiteName: String,
    val camperId: String,
    val otherName: String?,
    val status: GuestStatus
) {
    /** The conversation this contact opens. */
    val conversationId: String get() = Message.conversationIdFor(campsiteId, camperId)
}

/**
 * The latest message of one conversation, for the inbox list.
 *
 * @property campsiteId the campsite the conversation is about.
 * @property campsiteName its name.
 * @property ownerId the campsite's owner.
 * @property camperId the camper.
 * @property otherName the other person's name, or null if unknown.
 * @property lastText the latest message's text.
 * @property lastSentAt when it was sent.
 * @property lastFromMe true when the user wrote the latest message.
 * @property iAmOwner true when the user is the campsite's owner, false when they are the camper.
 */
data class ConversationSummary(
    val campsiteId: String,
    val campsiteName: String,
    val ownerId: String,
    val camperId: String,
    val otherName: String?,
    val lastText: String,
    val lastSentAt: Timestamp?,
    val lastFromMe: Boolean,
    val iAmOwner: Boolean
) {
    /** The conversation this summary belongs to. */
    val conversationId: String get() = Message.conversationIdFor(campsiteId, camperId)
}

/**
 * What the Messages tab lists.
 *
 * @property conversations conversations the user is in for their current mode, newest first.
 * @property contacts people the user can message who have no conversation yet.
 */
data class MessagesInboxState(
    val conversations: List<ConversationSummary> = emptyList(),
    val contacts: List<MessageContact> = emptyList()
) {
    /** True when there is nothing to list. */
    val isEmpty: Boolean get() = conversations.isEmpty() && contacts.isEmpty()
}

/**
 * Maps a waitlist status to a guest status.
 *
 * @param status the entry's status.
 * @return the guest status, or null when the camper is no longer reserving (lost or declined).
 */
fun guestStatusOf(status: WaitlistStatus): GuestStatus? = when (status) {
    WaitlistStatus.WAITING -> GuestStatus.WAITLISTED
    WaitlistStatus.WON -> GuestStatus.WON
    WaitlistStatus.RESERVED -> GuestStatus.RESERVED
    WaitlistStatus.LOST, WaitlistStatus.DECLINED -> null
}

/**
 * The guests an owner can message (US 02.12.01): campers with an active reservation, or a live
 * waitlist entry on the campsite's active lottery. One contact per campsite and camper.
 *
 * @param ownerId the owner; never listed as their own guest.
 * @param campsites the owner's campsites.
 * @param reservationsByCampsite every reservation for each campsite id.
 * @param entriesByCampsite the active lottery's waitlist entries for each campsite id.
 * @param names display names by device id.
 * @return the guests, sorted by campsite name then guest name.
 */
fun ownerGuests(
    ownerId: String,
    campsites: List<Campsite>,
    reservationsByCampsite: Map<String, List<Reservation>>,
    entriesByCampsite: Map<String, List<WaitlistEntry>>,
    names: Map<String, String>
): List<MessageContact> = campsites.flatMap { site ->
    val statuses = mutableMapOf<String, GuestStatus>()
    val entryNames = mutableMapOf<String, String>()
    fun add(camperId: String, status: GuestStatus) {
        if (camperId.isBlank() || camperId == ownerId) return
        statuses[camperId] = maxOf(statuses[camperId] ?: status, status)
    }
    reservationsByCampsite[site.id].orEmpty()
        .filter { it.status == ReservationStatus.ACTIVE }
        .forEach { add(it.userId, GuestStatus.RESERVED) }
    entriesByCampsite[site.id].orEmpty().forEach { entry ->
        guestStatusOf(entry.status)?.let { add(entry.userId, it) }
        if (entry.userName.isNotBlank()) entryNames[entry.userId] = entry.userName
    }
    statuses.map { (camperId, status) ->
        MessageContact(site.id, site.name, camperId, names[camperId] ?: entryNames[camperId], status)
    }
}.sortedWith(compareBy<MessageContact> { it.campsiteName.lowercase() }.thenBy { it.otherName.orEmpty().lowercase() })

/**
 * The campsite owners a camper can message (US 01.14.01): owners of campsites the camper has an
 * active reservation for, or a live waitlist entry on the campsite's active lottery.
 *
 * @param camperId the camper.
 * @param campsites every campsite.
 * @param reservations the camper's reservations.
 * @param entriesByLottery the camper's waitlist entry in each active lottery, or null if none.
 * @param names display names by device id.
 * @return one contact per campsite, sorted by campsite name.
 */
fun camperContacts(
    camperId: String,
    campsites: List<Campsite>,
    reservations: List<Reservation>,
    entriesByLottery: Map<String, WaitlistEntry?>,
    names: Map<String, String>
): List<MessageContact> {
    val reserved = reservations
        .filter { it.userId == camperId && it.status == ReservationStatus.ACTIVE }
        .map { it.campsiteId }
        .toSet()
    return campsites.mapNotNull { site ->
        if (site.ownerId.isBlank() || site.ownerId == camperId) return@mapNotNull null
        val fromLottery = site.activeLotteryId
            ?.let { entriesByLottery[it] }
            ?.takeIf { it.userId == camperId }
            ?.let { guestStatusOf(it.status) }
        val status = if (site.id in reserved) GuestStatus.RESERVED else fromLottery ?: return@mapNotNull null
        MessageContact(site.id, site.name, camperId, names[site.ownerId], status)
    }.sortedBy { it.campsiteName.lowercase() }
}

/**
 * Turns a user's messages into one summary per conversation.
 *
 * @param myId the user.
 * @param mode the user's current mode: owners see conversations about their campsites, campers
 *  see conversations about their stays, and admins see both.
 * @param messages the user's messages, in any order.
 * @param names display names by device id.
 * @return summaries, newest first.
 */
fun conversationSummaries(
    myId: String,
    mode: Role,
    messages: List<Message>,
    names: Map<String, String>
): List<ConversationSummary> = messages
    .filter {
        when (mode) {
            Role.OWNER -> it.ownerId == myId
            Role.ENTRANT -> it.camperId == myId
            Role.ADMIN -> it.ownerId == myId || it.camperId == myId
        }
    }
    .groupBy { it.conversationId }
    .values
    .map { thread ->
        val last = thread.maxWith(compareBy<Message, Date?>(nullsFirst()) { it.sentAt?.toDate() }.thenBy { it.id })
        val otherId = if (last.ownerId == myId) last.camperId else last.ownerId
        ConversationSummary(
            campsiteId = last.campsiteId,
            campsiteName = thread.lastOrNull { it.campsiteName.isNotBlank() }?.campsiteName ?: last.campsiteName,
            ownerId = last.ownerId,
            camperId = last.camperId,
            otherName = names[otherId],
            lastText = last.text,
            lastSentAt = last.sentAt,
            lastFromMe = last.senderId == myId,
            iAmOwner = last.ownerId == myId
        )
    }
    .sortedWith(compareByDescending<ConversationSummary, Date?>(nullsLast()) { it.lastSentAt?.toDate() })

/**
 * Builds the Messages tab: conversations for the current mode, then contacts with no
 * conversation yet. Admin mode has no contacts; admins reach owners and campers in their other modes.
 *
 * @param myId the user.
 * @param mode the user's current mode.
 * @param messages the user's messages.
 * @param contacts guests (owner mode) or owners (camper mode) the user can message.
 * @param names display names by device id.
 * @return the inbox.
 */
fun buildInboxState(
    myId: String,
    mode: Role,
    messages: List<Message>,
    contacts: List<MessageContact>,
    names: Map<String, String>
): MessagesInboxState {
    val conversations = conversationSummaries(myId, mode, messages, names)
    val started = conversations.map { it.conversationId }.toSet()
    return MessagesInboxState(
        conversations = conversations,
        contacts = if (mode == Role.ADMIN) emptyList() else contacts.filterNot { it.conversationId in started }
    )
}

/**
 * A short time for a message: the time of day today, the date this year, otherwise the full date.
 *
 * @param sentAt when the message was sent.
 * @param now the current time.
 * @param locale formatting locale.
 * @param timeZone time zone the user is in.
 * @return e.g. "4:52 PM", "Nov 3" or "Nov 3, 2025".
 */
fun formatMessageTime(
    sentAt: Date,
    now: Date,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val sent = Calendar.getInstance(timeZone, locale).apply { time = sentAt }
    val today = Calendar.getInstance(timeZone, locale).apply { time = now }
    val pattern = when {
        sent.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> "MMM d, yyyy"
        sent.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR) -> "MMM d"
        else -> "h:mm a"
    }
    return SimpleDateFormat(pattern, locale).apply { this.timeZone = timeZone }.format(sentAt)
}
