// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: Unit tests for the messaging rules: owner guests, camper contacts, summaries, times.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Message
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.model.Role
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.ui.navigation.BottomNavTab
import com.example.trails.ui.navigation.selectedTab
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

class ConversationsTest {

    private val owner = "owner-1"
    private val lake = Campsite(id = "lake", ownerId = owner, name = "Lake Site", activeLotteryId = "lot-1")
    private val ridge = Campsite(id = "ridge", ownerId = owner, name = "Ridge Site")
    private val names = mapOf(owner to "Daniel", "priya" to "Priya", "ava" to "Ava")

    private fun entry(user: String, status: WaitlistStatus, name: String = "") =
        WaitlistEntry(userId = user, userName = name, lotteryId = "lot-1", status = status, rulesAccepted = true)

    private fun reservation(user: String, site: String, status: ReservationStatus = ReservationStatus.ACTIVE) =
        Reservation(id = "$user-$site", userId = user, campsiteId = site, status = status, rulesAccepted = true)

    private fun msg(id: String, site: String, camper: String, fromOwner: Boolean, text: String, at: Long) = Message(
        id = id,
        conversationId = Message.conversationIdFor(site, camper),
        campsiteId = site,
        campsiteName = if (site == "lake") "Lake Site" else "Ridge Site",
        ownerId = owner,
        camperId = camper,
        senderId = if (fromOwner) owner else camper,
        senderRole = if (fromOwner) Role.OWNER else Role.ENTRANT,
        text = text,
        sentAt = Timestamp(Date(at))
    )

    // US 02.12.01: guests are campers with an active reservation or a live waitlist entry.
    @Test
    fun ownerGuests_listsReservedAndWaitlistedCampers() {
        val guests = ownerGuests(
            ownerId = owner,
            campsites = listOf(lake, ridge),
            reservationsByCampsite = mapOf(
                "ridge" to listOf(reservation("priya", "ridge"), reservation("gone", "ridge", ReservationStatus.CANCELLED))
            ),
            entriesByCampsite = mapOf(
                "lake" to listOf(
                    entry("ava", WaitlistStatus.WAITING),
                    entry("priya", WaitlistStatus.WON),
                    entry("lost", WaitlistStatus.LOST),
                    entry("declined", WaitlistStatus.DECLINED)
                )
            ),
            names = names
        )
        assertEquals(
            listOf(
                MessageContact("lake", "Lake Site", "ava", "Ava", GuestStatus.WAITLISTED),
                MessageContact("lake", "Lake Site", "priya", "Priya", GuestStatus.WON),
                MessageContact("ridge", "Ridge Site", "priya", "Priya", GuestStatus.RESERVED)
            ),
            guests
        )
    }

    @Test
    fun ownerGuests_keepsStrongestStatus_skipsOwner_andFallsBackToWaitlistName() {
        val guests = ownerGuests(
            ownerId = owner,
            campsites = listOf(lake),
            reservationsByCampsite = mapOf("lake" to listOf(reservation("sam", "lake"))),
            entriesByCampsite = mapOf(
                "lake" to listOf(entry("sam", WaitlistStatus.WAITING, "Sam W"), entry(owner, WaitlistStatus.WAITING))
            ),
            names = emptyMap()
        )
        assertEquals(listOf(MessageContact("lake", "Lake Site", "sam", "Sam W", GuestStatus.RESERVED)), guests)
    }

    // US 01.14.01: a camper can message owners of campsites they're reserving, not others.
    @Test
    fun camperContacts_listsOwnersOfCampsitesBeingReserved() {
        val other = Campsite(id = "other", ownerId = "owner-2", name = "Other Site")
        val mine = Campsite(id = "mine", ownerId = "priya", name = "My Own Site", activeLotteryId = "lot-1")
        val contacts = camperContacts(
            camperId = "priya",
            campsites = listOf(lake, ridge, other, mine),
            reservations = listOf(reservation("priya", "ridge"), reservation("priya", "other", ReservationStatus.REJECTED)),
            entriesByLottery = mapOf("lot-1" to entry("priya", WaitlistStatus.WAITING)),
            names = names
        )
        assertEquals(
            listOf(
                MessageContact("lake", "Lake Site", "priya", "Daniel", GuestStatus.WAITLISTED),
                MessageContact("ridge", "Ridge Site", "priya", "Daniel", GuestStatus.RESERVED)
            ),
            contacts
        )
    }

    @Test
    fun camperContacts_lostEntry_isNotAContact() {
        val contacts = camperContacts(
            "priya", listOf(lake), emptyList(), mapOf("lot-1" to entry("priya", WaitlistStatus.LOST)), names
        )
        assertTrue(contacts.isEmpty())
    }

    @Test
    fun summaries_oneRowPerConversation_newestFirst_fromEachSide() {
        val messages = listOf(
            msg("1", "lake", "priya", fromOwner = true, "Welcome", 1_000),
            msg("2", "lake", "priya", fromOwner = false, "Thanks", 3_000),
            msg("3", "ridge", "ava", fromOwner = true, "Hello Ava", 2_000)
        )
        val ownerView = conversationSummaries(owner, Role.OWNER, messages, names)
        assertEquals(listOf("Priya", "Ava"), ownerView.map { it.otherName })
        assertEquals("Thanks", ownerView.first().lastText)
        assertFalse(ownerView.first().lastFromMe)
        assertTrue(ownerView.first().iAmOwner)

        val camperView = conversationSummaries("priya", Role.ENTRANT, messages.filter { it.camperId == "priya" }, names)
        assertEquals(1, camperView.size)
        assertEquals("Daniel", camperView.single().otherName)
        assertTrue(camperView.single().lastFromMe)
        assertFalse(camperView.single().iAmOwner)
    }

    // Owner mode shows only conversations about the user's campsites, camper mode only their stays.
    @Test
    fun summaries_followTheCurrentMode() {
        val asCamper = msg("1", "lake", owner, fromOwner = false, "I'm camping elsewhere", 1_000)
            .copy(ownerId = "owner-2", camperId = owner, senderId = owner)
        assertTrue(conversationSummaries(owner, Role.OWNER, listOf(asCamper), names).isEmpty())
        assertEquals(1, conversationSummaries(owner, Role.ENTRANT, listOf(asCamper), names).size)
        assertEquals(1, conversationSummaries(owner, Role.ADMIN, listOf(asCamper), names).size)
    }

    @Test
    fun inbox_hidesContactsThatAlreadyHaveAConversation() {
        val contacts = listOf(
            MessageContact("lake", "Lake Site", "priya", "Priya", GuestStatus.WON),
            MessageContact("lake", "Lake Site", "ava", "Ava", GuestStatus.WAITLISTED)
        )
        val inbox = buildInboxState(owner, Role.OWNER, listOf(msg("1", "lake", "priya", true, "Hi", 1_000)), contacts, names)
        assertEquals(1, inbox.conversations.size)
        assertEquals(listOf("ava"), inbox.contacts.map { it.camperId })
        assertFalse(inbox.isEmpty)
        assertTrue(buildInboxState(owner, Role.OWNER, emptyList(), emptyList(), names).isEmpty)
        assertTrue(buildInboxState(owner, Role.ADMIN, emptyList(), contacts, names).contacts.isEmpty())
    }

    @Test
    fun guestStatusOf_mapsEveryWaitlistStatus() {
        assertEquals(GuestStatus.WAITLISTED, guestStatusOf(WaitlistStatus.WAITING))
        assertEquals(GuestStatus.WON, guestStatusOf(WaitlistStatus.WON))
        assertEquals(GuestStatus.RESERVED, guestStatusOf(WaitlistStatus.RESERVED))
        assertNull(guestStatusOf(WaitlistStatus.LOST))
        assertNull(guestStatusOf(WaitlistStatus.DECLINED))
    }

    @Test
    fun formatMessageTime_todayThisYearAndOlder() {
        val utc = TimeZone.getTimeZone("UTC")
        fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
            GregorianCalendar(utc).apply { clear(); set(y, m - 1, d, h, min) }.time
        val now = at(2026, 11, 25, 18, 0)
        assertEquals("4:52 PM", formatMessageTime(at(2026, 11, 25, 16, 52), now, Locale.US, utc))
        assertEquals("Nov 3", formatMessageTime(at(2026, 11, 3, 9, 0), now, Locale.US, utc))
        assertEquals("Dec 30, 2025", formatMessageTime(at(2025, 12, 30, 9, 0), now, Locale.US, utc))
    }

    @Test
    fun chatRoute_isUnderTheMessagesTab() {
        assertEquals("chat/lake/priya", MessagingRoutes.chat("lake", "priya"))
        assertEquals(BottomNavTab.MESSAGES, selectedTab(MessagingRoutes.CHAT))
    }
}
