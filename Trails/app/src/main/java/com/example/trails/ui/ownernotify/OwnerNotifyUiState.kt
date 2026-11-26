// UI state and rules for notifying a waitlist and the Sent notifications history (US 02.10.01, 02.10.02).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Plain Kotlin (no Compose), so it can be unit-tested.
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write the form checks, history filters and date text.
package com.example.trails.ui.ownernotify

import com.example.trails.data.model.Lottery
import com.example.trails.data.model.SentNotification
import com.example.trails.data.model.SentRecipient
import com.example.trails.data.model.WaitlistEntry
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Longest subject allowed. */
const val MAX_SUBJECT = 80

/** Longest message allowed, as in Figma ("Up to 500 characters"). */
const val MAX_MESSAGE = 500

/** A problem with what the owner typed. */
enum class NotifyFieldError {
    /** The subject is empty. */
    SUBJECT_REQUIRED,

    /** The subject is longer than [MAX_SUBJECT]. */
    SUBJECT_TOO_LONG,

    /** The message is empty. */
    MESSAGE_REQUIRED,

    /** The message is longer than [MAX_MESSAGE]. */
    MESSAGE_TOO_LONG
}

/**
 * What the owner typed on New notification.
 *
 * @property subject the title.
 * @property message the text.
 */
data class NotifyForm(val subject: String = "", val message: String = "")

/**
 * Problems found in a [NotifyForm].
 *
 * @property subject the subject's problem, or null.
 * @property message the message's problem, or null.
 */
data class NotifyFormErrors(val subject: NotifyFieldError? = null, val message: NotifyFieldError? = null) {
    /** True if anything must be fixed before sending. */
    val hasErrors: Boolean get() = subject != null || message != null
}

/**
 * Checks the form (US 02.10.01).
 *
 * @param form what the owner typed.
 * @return the problems; empty when it can be sent.
 */
fun validateNotifyForm(form: NotifyForm): NotifyFormErrors {
    val subject = form.subject.trim()
    val message = form.message.trim()
    return NotifyFormErrors(
        subject = when {
            subject.isEmpty() -> NotifyFieldError.SUBJECT_REQUIRED
            subject.length > MAX_SUBJECT -> NotifyFieldError.SUBJECT_TOO_LONG
            else -> null
        },
        message = when {
            message.isEmpty() -> NotifyFieldError.MESSAGE_REQUIRED
            message.length > MAX_MESSAGE -> NotifyFieldError.MESSAGE_TOO_LONG
            else -> null
        }
    )
}

/**
 * The people on a waitlist, in the order they joined.
 *
 * @param entries the lottery's waitlist entries.
 * @return one recipient per entrant; a blank name is kept blank for the screen to replace.
 */
fun recipientsFrom(entries: List<WaitlistEntry>): List<SentRecipient> =
    entries
        .sortedBy { it.joinedAt?.toDate()?.time ?: Long.MAX_VALUE }
        .map { SentRecipient(userId = it.userId, name = it.userName, partySize = it.partySize) }

/**
 * "Grande Cache Ridge · June 2027 waitlist": campsite and lottery, either may be missing.
 *
 * @param campsiteName the campsite's name, or blank.
 * @param lotteryName the lottery's name, or blank.
 */
fun waitlistLabel(campsiteName: String, lotteryName: String): String =
    listOf(campsiteName, lotteryName.takeIf { it.isNotBlank() }?.let { "$it waitlist" } ?: "")
        .filter { it.isNotBlank() }
        .joinToString(" · ")

/**
 * The record kept for the owner after sending.
 *
 * @param ownerId who sent it.
 * @param lottery the lottery whose waitlist was notified.
 * @param campsiteName the lottery's campsite name.
 * @param form what was sent; trimmed here.
 * @param recipients who it went to.
 * @param sentAt when it was sent.
 */
fun buildSentNotification(
    ownerId: String,
    lottery: Lottery,
    campsiteName: String,
    form: NotifyForm,
    recipients: List<SentRecipient>,
    sentAt: Timestamp
): SentNotification = SentNotification(
    ownerId = ownerId,
    lotteryId = lottery.id,
    lotteryName = lottery.name,
    campsiteName = campsiteName,
    subject = form.subject.trim(),
    message = form.message.trim(),
    recipients = recipients,
    sentAt = sentAt
)

/** The chips on Sent notifications (US 02.10.02). */
enum class SentFilter {
    /** Everything ever sent. */
    ALL,

    /** Sent in the last 7 days. */
    WEEK,

    /** Sent in the last 30 days. */
    MONTH
}

private const val DAY_MS = 86_400_000L

/**
 * The sent notifications under one chip, newest first.
 *
 * @param sent everything the owner sent.
 * @param filter the selected chip.
 * @param now the current time in milliseconds.
 */
fun filterSent(sent: List<SentNotification>, filter: SentFilter, now: Long): List<SentNotification> {
    val since = when (filter) {
        SentFilter.ALL -> Long.MIN_VALUE
        SentFilter.WEEK -> now - 7 * DAY_MS
        SentFilter.MONTH -> now - 30 * DAY_MS
    }
    return sent
        .filter { (it.sentAt?.toDate()?.time ?: Long.MIN_VALUE) >= since }
        .sortedByDescending { it.sentAt?.toDate()?.time ?: 0L }
}

/**
 * When it was sent, e.g. "May 1, 2027, 09:24", or null if unknown.
 *
 * @param sentAt the time sent.
 * @param locale the language for month names.
 */
fun formatSentAt(sentAt: Timestamp?, locale: Locale = Locale.getDefault()): String? =
    sentAt?.let { SimpleDateFormat("MMM d, yyyy, HH:mm", locale).format(Date(it.toDate().time)) }

/** Every situation New notification can be in. */
sealed interface ComposeUiState {

    /** The lottery and its waitlist are loading. */
    data object Loading : ComposeUiState

    /**
     * Ready to write.
     *
     * @property lottery the lottery being notified.
     * @property campsiteName its campsite's name, or blank.
     * @property recipients everyone on its waitlist.
     */
    data class Ready(
        val lottery: Lottery,
        val campsiteName: String,
        val recipients: List<SentRecipient>
    ) : ComposeUiState

    /** The lottery does not exist. */
    data object NotFound : ComposeUiState

    /**
     * Loading failed.
     *
     * @property reason the error message, if there is one.
     */
    data class Error(val reason: String?) : ComposeUiState
}

/** Progress of sending. */
sealed interface SendState {

    /** Nothing in progress. */
    data object Idle : SendState

    /** Sending; the button is disabled. */
    data object Sending : SendState

    /**
     * Sent; the "Notification sent" dialog shows.
     *
     * @property subject the subject that was sent.
     * @property count how many entrants received it.
     * @property waitlist the waitlist label, e.g. "Grande Cache Ridge · June 2027 waitlist".
     */
    data class Sent(val subject: String, val count: Int, val waitlist: String) : SendState

    /**
     * Sending failed.
     *
     * @property reason the error message, if there is one.
     */
    data class Failed(val reason: String?) : SendState
}
