// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.01, 01.01.02
// Plain Kotlin so it can be unit-tested.
package com.example.trails.ui.discovery

import com.example.trails.data.model.Campsite
import com.example.trails.ui.campsite.availabilityToDays
import com.example.trails.ui.campsite.dayKey
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/** Search text plus the location, guest, and date filters. */
data class BrowseFilters(
    val query: String = "",
    val location: String = "",
    val guestsText: String = "",
    val arriveMillis: Long? = null,
    val leaveMillis: Long? = null
)

/** The list on screen, the active filters, and how many campsites exist before filtering. */
data class BrowseUiState(
    val shown: List<Campsite> = emptyList(),
    val filters: BrowseFilters = BrowseFilters(),
    val totalCount: Int = 0
)

/**
 * Campsites that match the keyword and every active filter, sorted by name.
 * A blank filter is ignored.
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

/** Keyword search (US 01.01.01). Blank text matches everything. */
fun matchesQuery(campsite: Campsite, query: String): Boolean {
    if (query.isEmpty()) return true
    return campsite.name.contains(query, ignoreCase = true) ||
        campsite.locationName.contains(query, ignoreCase = true) ||
        campsite.description.contains(query, ignoreCase = true) ||
        campsite.tags.any { it.contains(query, ignoreCase = true) }
}

/** Location filter (US 01.01.02). Blank text matches everything. */
fun matchesLocation(campsite: Campsite, location: String): Boolean {
    if (location.isEmpty()) return true
    return campsite.locationName.contains(location, ignoreCase = true)
}

/**
 * Guest filter (US 01.01.02). Keeps campsites that can sleep at least [guests] people.
 * Blank or non-numeric text matches everything.
 */
fun matchesGuests(campsite: Campsite, guests: Int?): Boolean {
    if (guests == null || guests < 1) return true
    return campsite.maxOccupancy >= guests
}

/**
 * Date filter (US 01.01.02). Keeps a campsite when one of its available days falls in the
 * picked range. Both ends are included. Either end alone is a one-day range.
 * [arriveMillis] and [leaveMillis] are Material date-picker values (UTC midnight).
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

/** The calendar day the user picked, as the same day key used for campsite availability. */
fun datePickerDayKey(millis: Long): Long {
    val utc = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return dayKey(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH) + 1, utc.get(Calendar.DAY_OF_MONTH))
}
