// Unit tests for the owner notification rules: form checks, recipients, history filters and text (US 02.10.01, 02.10.02).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write tests for the notification form and history filters.
package com.example.trails.ui.ownernotify

import com.example.trails.data.model.Lottery
import com.example.trails.data.model.SentNotification
import com.example.trails.data.model.SentRecipient
import com.example.trails.data.model.WaitlistEntry
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/** Tests the plain-Kotlin rules behind New notification and Sent notifications. */
class OwnerNotifyUiStateTest {

    private val day = 86_400_000L
    private val now = 1_800_000_000_000L
    private fun at(ms: Long) = Timestamp(Date(ms))

    @Test
    fun validate_filledForm_noErrors() {
        assertFalse(validateNotifyForm(NotifyForm("Lottery draw date update", "The draw moved to May 24.")).hasErrors)
    }

    @Test
    fun validate_blankFields_required() {
        val errors = validateNotifyForm(NotifyForm("  ", ""))
        assertEquals(NotifyFieldError.SUBJECT_REQUIRED, errors.subject)
        assertEquals(NotifyFieldError.MESSAGE_REQUIRED, errors.message)
    }

    @Test
    fun validate_tooLong_rejected() {
        val errors = validateNotifyForm(NotifyForm("a".repeat(MAX_SUBJECT + 1), "b".repeat(MAX_MESSAGE + 1)))
        assertEquals(NotifyFieldError.SUBJECT_TOO_LONG, errors.subject)
        assertEquals(NotifyFieldError.MESSAGE_TOO_LONG, errors.message)
    }

    @Test
    fun validate_exactlyAtLimits_ok() {
        assertFalse(validateNotifyForm(NotifyForm("a".repeat(MAX_SUBJECT), "b".repeat(MAX_MESSAGE))).hasErrors)
    }

    @Test
    fun recipients_inJoinOrderWithNameAndParty() {
        val entries = listOf(
            WaitlistEntry(userId = "lena", userName = "Lena Fischer", partySize = 3, joinedAt = at(now)),
            WaitlistEntry(userId = "maya", userName = "Maya Singh", partySize = 2, joinedAt = at(now - 2 * day))
        )
        assertEquals(
            listOf(SentRecipient("maya", "Maya Singh", 2), SentRecipient("lena", "Lena Fischer", 3)),
            recipientsFrom(entries)
        )
    }

    @Test
    fun waitlistLabel_campsiteAndLottery() {
        assertEquals("Grande Cache Ridge · June 2027 waitlist", waitlistLabel("Grande Cache Ridge", "June 2027"))
        assertEquals("June 2027 waitlist", waitlistLabel("", "June 2027"))
        assertEquals("Grande Cache Ridge", waitlistLabel("Grande Cache Ridge", ""))
    }

    @Test
    fun buildSentNotification_trimsAndKeepsRecipients() {
        val recipients = listOf(SentRecipient("maya", "Maya Singh", 2))
        val record = buildSentNotification(
            "device-owner-001", Lottery(id = "june", name = "June 2027"), "Grande Cache Ridge",
            NotifyForm("  Lottery draw date update ", " Draw moved. "), recipients, at(now)
        )
        assertEquals("device-owner-001", record.ownerId)
        assertEquals("june", record.lotteryId)
        assertEquals("Lottery draw date update", record.subject)
        assertEquals("Draw moved.", record.message)
        assertEquals(recipients, record.recipients)
    }

    @Test
    fun filterSent_byChip_newestFirst() {
        val today = SentNotification(id = "today", sentAt = at(now))
        val tenDays = SentNotification(id = "ten", sentAt = at(now - 10 * day))
        val twoMonths = SentNotification(id = "old", sentAt = at(now - 60 * day))
        val all = listOf(twoMonths, today, tenDays)
        assertEquals(listOf("today", "ten", "old"), filterSent(all, SentFilter.ALL, now).map { it.id })
        assertEquals(listOf("today"), filterSent(all, SentFilter.WEEK, now).map { it.id })
        assertEquals(listOf("today", "ten"), filterSent(all, SentFilter.MONTH, now).map { it.id })
    }

    @Test
    fun formatSentAt_likeFigma_orNull() {
        val may1 = Timestamp(GregorianCalendar(2027, 4, 1, 9, 24).time)
        assertEquals("May 1, 2027, 09:24", formatSentAt(may1, Locale.US))
        assertNull(formatSentAt(null))
    }

    @Test
    fun formErrors_noneByDefault() {
        assertTrue(!NotifyFormErrors().hasErrors)
    }
}
