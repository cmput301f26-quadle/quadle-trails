// Form logic for creating and editing a lottery (US 02.04.01–02.04.07).
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Plain Kotlin with no Android or coroutine types, so it is unit-tested directly.
package com.example.trails.ui.lottery

import com.example.trails.data.model.Lottery

/** Most winners a lottery can draw. */
const val MAX_WINNERS = 999

/** Largest waiting list the stepper allows. */
const val MAX_WAITLIST = 9990

/** Waiting list size suggested when the owner first turns the limit on (Figma F16). */
const val DEFAULT_WAITLIST = 100

/** How much the waiting list stepper changes per tap. */
const val WAITLIST_STEP = 10

/**
 * Everything the owner has chosen so far. Times are milliseconds so the logic stays plain Kotlin.
 *
 * @property name lottery name, e.g. "May long weekend".
 * @property campsiteIds campsites in the lottery, in the order picked (US 02.04.01, 02.04.02).
 * @property registrationStart when entrants can start joining (US 02.04.03).
 * @property registrationEnd when joining closes and the draw runs automatically (US 02.04.03).
 * @property bookingStart when winners can start booking (US 02.04.04).
 * @property bookingEnd when the booking window closes (US 02.04.04).
 * @property numWinners how many entrants are drawn (US 02.04.05).
 * @property limitWaitlist true if the waiting list has a maximum size (US 02.04.07).
 * @property waitlistCapacity the maximum, only used while [limitWaitlist] is on.
 */
data class LotteryDraft(
    val name: String = "",
    val campsiteIds: List<String> = emptyList(),
    val registrationStart: Long? = null,
    val registrationEnd: Long? = null,
    val bookingStart: Long? = null,
    val bookingEnd: Long? = null,
    val numWinners: Int = 1,
    val limitWaitlist: Boolean = false,
    val waitlistCapacity: Int = DEFAULT_WAITLIST
) {
    /** The draw runs automatically when registration closes. */
    val drawTime: Long? get() = registrationEnd

    /** Adds [campsiteId] if it isn't picked yet, otherwise removes it. */
    fun toggleCampsite(campsiteId: String): LotteryDraft =
        copy(campsiteIds = if (campsiteId in campsiteIds) campsiteIds - campsiteId else campsiteIds + campsiteId)

    /** Sets the number of winners (1 to [MAX_WINNERS]) and grows the waiting list to fit them. */
    fun withWinners(count: Int): LotteryDraft {
        val winners = count.coerceIn(1, MAX_WINNERS)
        return copy(numWinners = winners, waitlistCapacity = maxOf(waitlistCapacity, winners))
    }

    /** Sets the waiting list size, never below the number of winners. */
    fun withWaitlistCapacity(capacity: Int): LotteryDraft =
        copy(waitlistCapacity = capacity.coerceIn(numWinners, MAX_WAITLIST))

    /** Turns the waiting list limit on or off. */
    fun withWaitlistLimit(on: Boolean): LotteryDraft =
        copy(limitWaitlist = on, waitlistCapacity = maxOf(waitlistCapacity, numWinners))

    /**
     * Builds the lottery to save. Call only when [problems] is empty.
     *
     * @param base the lottery being edited, or null for a new one; its id and owner are kept.
     * @param ownerId owner assigned to a new lottery.
     */
    fun toLottery(base: Lottery?, ownerId: String): Lottery = (base ?: Lottery(ownerId = ownerId)).copy(
        name = name.trim(),
        campsiteIds = campsiteIds,
        registrationStart = registrationStart?.let(::timestampOf),
        registrationEnd = registrationEnd?.let(::timestampOf),
        bookingStart = bookingStart?.let(::timestampOf),
        bookingEnd = bookingEnd?.let(::timestampOf),
        numWinners = numWinners,
        waitlistCapacity = if (limitWaitlist) waitlistCapacity else null
    )

    companion object {
        /** Pre-fills the form from an existing lottery, for editing (US 02.04.06). */
        fun from(lottery: Lottery): LotteryDraft = LotteryDraft(
            name = lottery.name,
            campsiteIds = lottery.campsiteIds,
            registrationStart = lottery.registrationStart.millis(),
            registrationEnd = lottery.registrationEnd.millis(),
            bookingStart = lottery.bookingStart.millis(),
            bookingEnd = lottery.bookingEnd.millis(),
            numWinners = lottery.numWinners.coerceAtLeast(1),
            limitWaitlist = lottery.waitlistCapacity != null,
            waitlistCapacity = lottery.waitlistCapacity ?: maxOf(DEFAULT_WAITLIST, lottery.numWinners)
        )
    }
}

/** The parts of the form a problem can be shown on. */
enum class LotteryField { NAME, CAMPSITES, REGISTRATION, BOOKING, WAITLIST }

/** Something that stops the lottery from being saved. The screen turns each into a message. */
sealed interface LotteryProblem {
    data object NameMissing : LotteryProblem
    data object NoCampsites : LotteryProblem
    data object RegistrationNotSet : LotteryProblem
    data object RegistrationEndsBeforeStart : LotteryProblem
    data object RegistrationClosesInPast : LotteryProblem

    /** Registration already opened, so its start can't move. */
    data object RegistrationStartLocked : LotteryProblem
    data object BookingNotSet : LotteryProblem

    /** Booking must open once registration closes, at [registrationEnd], or later (Figma F13). */
    data class BookingBeforeRegistrationCloses(val registrationEnd: Long) : LotteryProblem
    data object BookingEndsBeforeStart : LotteryProblem

    /** A limited waiting list has to fit at least one person per winner. */
    data class WaitlistBelowWinners(val winners: Int) : LotteryProblem

    /** [entrants] people already joined, so the limit can't go below that. */
    data class WaitlistBelowEntrants(val entrants: Int) : LotteryProblem
}

/**
 * Checks the draft, at most one problem per field.
 *
 * @param now current time; registration has to close in the future.
 * @param entrantCount people already on the waiting list (0 for a new lottery).
 * @param openedAt when registration opened if it already has, which then can't change.
 * @return problems keyed by field; empty if the lottery can be saved.
 */
fun LotteryDraft.problems(now: Long, entrantCount: Int = 0, openedAt: Long? = null): Map<LotteryField, LotteryProblem> = buildMap {
    if (name.isBlank()) put(LotteryField.NAME, LotteryProblem.NameMissing)
    if (campsiteIds.isEmpty()) put(LotteryField.CAMPSITES, LotteryProblem.NoCampsites)

    val regStart = registrationStart
    val regEnd = registrationEnd
    when {
        regStart == null || regEnd == null -> put(LotteryField.REGISTRATION, LotteryProblem.RegistrationNotSet)
        regEnd <= regStart -> put(LotteryField.REGISTRATION, LotteryProblem.RegistrationEndsBeforeStart)
        openedAt != null && regStart != openedAt -> put(LotteryField.REGISTRATION, LotteryProblem.RegistrationStartLocked)
        regEnd <= now -> put(LotteryField.REGISTRATION, LotteryProblem.RegistrationClosesInPast)
    }

    val bookStart = bookingStart
    val bookEnd = bookingEnd
    when {
        bookStart == null || bookEnd == null -> put(LotteryField.BOOKING, LotteryProblem.BookingNotSet)
        regEnd != null && bookStart < regEnd -> put(LotteryField.BOOKING, LotteryProblem.BookingBeforeRegistrationCloses(regEnd))
        bookEnd <= bookStart -> put(LotteryField.BOOKING, LotteryProblem.BookingEndsBeforeStart)
    }

    if (limitWaitlist) {
        when {
            waitlistCapacity < numWinners -> put(LotteryField.WAITLIST, LotteryProblem.WaitlistBelowWinners(numWinners))
            waitlistCapacity < entrantCount -> put(LotteryField.WAITLIST, LotteryProblem.WaitlistBelowEntrants(entrantCount))
        }
    }
}

/** What an edit changed, for the "Changes saved" banner (Figma F22). */
sealed interface LotteryChange {
    data class Winners(val count: Int) : LotteryChange
    data class Waitlist(val capacity: Int?) : LotteryChange
    data object Name : LotteryChange
    data object Campsites : LotteryChange
    data object Registration : LotteryChange
    data object Booking : LotteryChange

    /** More than one setting changed, or none did. */
    data object General : LotteryChange
}

/** Describes the difference between [before] and [after] when exactly one setting changed. */
fun describeChange(before: Lottery, after: Lottery): LotteryChange {
    val changes = buildList {
        if (before.numWinners != after.numWinners) add(LotteryChange.Winners(after.numWinners))
        if (before.waitlistCapacity != after.waitlistCapacity) add(LotteryChange.Waitlist(after.waitlistCapacity))
        if (before.name != after.name) add(LotteryChange.Name)
        if (before.campsiteIds.toSet() != after.campsiteIds.toSet()) add(LotteryChange.Campsites)
        if (before.registrationStart != after.registrationStart || before.registrationEnd != after.registrationEnd) {
            add(LotteryChange.Registration)
        }
        if (before.bookingStart != after.bookingStart || before.bookingEnd != after.bookingEnd) add(LotteryChange.Booking)
    }
    return changes.singleOrNull() ?: LotteryChange.General
}
