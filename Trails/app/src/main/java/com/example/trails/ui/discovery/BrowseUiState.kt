// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.01, 01.01.02
// PURPOSE: Filter rules for the entrant browse list. Plain Kotlin so it can be unit-tested.
// Outstanding issues: none.
package com.example.trails.ui.discovery

import com.example.trails.data.model.Campsite
import com.example.trails.ui.campsite.availabilityToDays
import com.example.trails.ui.campsite.dayKey
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * The keyword and filters currently applied on the browse screen.
 *
 * A blank field means that filter is not applied.
 *
 * @property query keyword matched against the name, location, description, and tags.
 * @property location text matched against the campsite's location name.
 * @property guestsText minimum number of guests, as typed. Non-numeric text is ignored.
 * @property arriveMillis start of the date filter, or null when unset. UTC midnight from the date picker.
 * @property leaveMillis end of the date filter, or null when unset. UTC midnight from the date picker.
 */
data class BrowseFilters(
    val query: String = "",
    val location: String = "",
    val guestsText: String = "",
    val arriveMillis: Long? = null,
    val leaveMillis: Long? = null
)

/**
 * Everything the browse screen needs to draw.
 *
 * @property shown campsites that match [filters], sorted by name.
 * @property filters the keyword and filters currently applied to the list.
 * @property totalCount how many campsites exist before any keyword or filter is applied.
 * @property places location names of [shown], with duplicates removed, sorted.
 */
data class BrowseUiState(
    val shown: List<Campsite> = emptyList(),
    val filters: BrowseFilters = BrowseFilters(),
    val totalCount: Int = 0,
    val places: List<String> = emptyList()
)

/**
 * Builds the campsite list shown on Browse.
 *
 * Keeps campsites that pass the keyword and every active filter, then sorts them by name.
 * A blank filter does not remove any campsite.
 *
 * @param campsites every campsite loaded from the repository.
 * @param filters the search text and filters currently entered.
 * @return the campsites to display, sorted by name.
 */
fun buildBrowseList(campsites: List<Campsite>, filters: BrowseFilters): List<Campsite> {
    val query = filters.query.trim()
    val location = filters.location.trim()
    val guests = filters.guestsText.trim().toIntOrNull()
    return campsites
        .filter { campsite ->
            matchesQuery(campsite, query) &&
                matchesLocation(campsite, location) &&
                matchesGuests(campsite, guests) &&
                matchesDates(campsite, filters.arriveMillis, filters.leaveMillis)
        }
        .sortedBy { it.name.lowercase() }
}

/**
 * Checks whether a campsite matches a keyword.
 *
 * The keyword is compared, ignoring case, with the name, location, description, and tags.
 *
 * @param campsite the campsite to test.
 * @param query the keyword, already trimmed. A blank keyword matches every campsite.
 * @return true when the campsite should stay in the list.
 */
fun matchesQuery(campsite: Campsite, query: String): Boolean {
    if (query.isEmpty()) return true
    return campsite.name.contains(query, ignoreCase = true) ||
        campsite.locationName.contains(query, ignoreCase = true) ||
        campsite.description.contains(query, ignoreCase = true) ||
        campsite.tags.any { it.contains(query, ignoreCase = true) }
}

/**
 * Checks whether a campsite is in the location the entrant typed.
 *
 * The comparison ignores case and matches any part of the location name.
 *
 * @param campsite the campsite to test.
 * @param location the location text, already trimmed. Blank text matches every campsite.
 * @return true when the campsite should stay in the list.
 */
fun matchesLocation(campsite: Campsite, location: String): Boolean {
    if (location.isEmpty()) return true
    return campsite.locationName.contains(location, ignoreCase = true)
}

/**
 * Checks whether a campsite can sleep the requested number of guests.
 *
 * The campsite stays when its maximum occupancy is at least [guests].
 *
 * @param campsite the campsite to test.
 * @param guests the minimum number of guests, or null when the field is blank or not a number.
 * @return true when the campsite should stay in the list. Null and numbers below 1 match every campsite.
 */
fun matchesGuests(campsite: Campsite, guests: Int?): Boolean {
    if (guests == null || guests < 1) return true
    return campsite.maxOccupancy >= guests
}

/**
 * Checks whether a campsite is available on any day in the picked range.
 *
 * Both ends are included. If only one end is set, that single day is the whole range.
 * A campsite with no availability dates does not match a date filter.
 *
 * @param campsite the campsite to test.
 * @param arriveMillis start of the range, or null when unset. UTC midnight from the date picker.
 * @param leaveMillis end of the range, or null when unset. UTC midnight from the date picker.
 * @return true when the campsite should stay in the list. Both null values match every campsite.
 */
fun matchesDates(campsite: Campsite, arriveMillis: Long?, leaveMillis: Long?): Boolean {
    if (arriveMillis == null && leaveMillis == null) return true
    val open = availabilityToDays(campsite.availability)
    if (open.isEmpty()) return false
    val arrive = arriveMillis?.let(::datePickerDayKey)
    val leave = leaveMillis?.let(::datePickerDayKey)
    val from = minOf(arrive ?: leave!!, leave ?: arrive!!)
    val to = maxOf(arrive ?: leave!!, leave ?: arrive!!)
    return (from..to).any { it in open }
}

/**
 * Converts a date-picker value into the day key used for campsite availability.
 *
 * The picker stores UTC midnight. The year, month, and day are read in UTC so the
 * chosen calendar day does not shift with the phone's time zone.
 *
 * @param millis UTC midnight of the picked day, in milliseconds.
 * @return the day key for that calendar day.
 */
fun datePickerDayKey(millis: Long): Long {
    val utc = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return dayKey(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH) + 1, utc.get(Calendar.DAY_OF_MONTH))
}
