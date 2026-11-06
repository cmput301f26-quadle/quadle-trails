// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.01, 01.01.02
// PURPOSE: Unit tests for browse keyword search and filters.
// Outstanding issues: none.
package com.example.trails.ui.discovery

import com.example.trails.data.fake.TestCampsites
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Checks that [buildBrowseList] keeps, drops, and sorts campsites for each filter,
 * including blank fields, a guest count of zero, and a campsite with no dates.
 */
class BrowseUiStateTest {

    private val all = TestCampsites.all

    @Test
    fun blankQuery_keepsEveryCampsiteSortedByName() {
        val shown = buildBrowseList(all, BrowseFilters())
        assertEquals(
            listOf("Tunnel Mountain Site 14", "Whistlers Riverside Site 7", "Wildhorse Lake Site 3"),
            shown.map { it.name }
        )
    }

    @Test
    fun query_matchesName() {
        val shown = buildBrowseList(all, BrowseFilters(query = "tunnel"))
        assertEquals(listOf(TestCampsites.BANFF_ID), shown.map { it.id })
    }

    @Test
    fun query_matchesTag() {
        val shown = buildBrowseList(all, BrowseFilters(query = "tennis"))
        assertEquals(listOf(TestCampsites.HINTON_ID), shown.map { it.id })
    }

    @Test
    fun query_withNoMatch_isEmpty() {
        assertEquals(emptyList<String>(), buildBrowseList(all, BrowseFilters(query = "zzzz")).map { it.id })
    }

    @Test
    fun query_matchesDescriptionIgnoringCase() {
        val shown = buildBrowseList(all, BrowseFilters(query = "PADDLING"))
        assertEquals(listOf(TestCampsites.HINTON_ID), shown.map { it.id })
    }

    @Test
    fun query_withOnlySpaces_keepsEveryCampsite() {
        assertEquals(3, buildBrowseList(all, BrowseFilters(query = "   ")).size)
    }

    @Test
    fun location_jasper_keepsWhistlers() {
        val shown = buildBrowseList(all, BrowseFilters(location = "jasper"))
        assertEquals(listOf(TestCampsites.JASPER_ID), shown.map { it.id })
    }

    @Test
    fun location_withNoMatch_isEmpty() {
        assertEquals(emptyList<String>(), buildBrowseList(all, BrowseFilters(location = "canmore")).map { it.id })
    }

    @Test
    fun guests7_keepsOnlyHinton() {
        val shown = buildBrowseList(all, BrowseFilters(guestsText = "7"))
        assertEquals(listOf(TestCampsites.HINTON_ID), shown.map { it.id })
    }

    @Test
    fun guestsAtBanffCapacity_keepsBanffAndHinton() {
        val shown = buildBrowseList(all, BrowseFilters(guestsText = "6"))
        assertEquals(listOf(TestCampsites.BANFF_ID, TestCampsites.HINTON_ID), shown.map { it.id })
    }

    @Test
    fun guestsAboveEveryCapacity_isEmpty() {
        assertEquals(emptyList<String>(), buildBrowseList(all, BrowseFilters(guestsText = "9")).map { it.id })
    }

    @Test
    fun guestsZeroOrNotANumber_keepsEveryCampsite() {
        assertEquals(3, buildBrowseList(all, BrowseFilters(guestsText = "0")).size)
        assertEquals(3, buildBrowseList(all, BrowseFilters(guestsText = "abc")).size)
    }

    @Test
    fun may82027_keepsOnlyHinton() {
        val day = utcMidnight(2027, 5, 8)
        val shown = buildBrowseList(all, BrowseFilters(arriveMillis = day, leaveMillis = day))
        assertEquals(listOf(TestCampsites.HINTON_ID), shown.map { it.id })
    }

    @Test
    fun oneDateAlone_isASingleDay() {
        val day = utcMidnight(2027, 5, 8)
        assertEquals(
            listOf(TestCampsites.HINTON_ID),
            buildBrowseList(all, BrowseFilters(arriveMillis = day)).map { it.id }
        )
        assertEquals(
            listOf(TestCampsites.HINTON_ID),
            buildBrowseList(all, BrowseFilters(leaveMillis = day)).map { it.id }
        )
    }

    @Test
    fun datesInReverseOrder_stillIncludeBothEnds() {
        val shown = buildBrowseList(
            all,
            BrowseFilters(arriveMillis = utcMidnight(2027, 5, 9), leaveMillis = utcMidnight(2027, 5, 8))
        )
        assertEquals(listOf(TestCampsites.HINTON_ID), shown.map { it.id })
    }

    @Test
    fun dateOutsideEveryRange_isEmpty() {
        val day = utcMidnight(2027, 5, 1)
        assertEquals(
            emptyList<String>(),
            buildBrowseList(all, BrowseFilters(arriveMillis = day, leaveMillis = day)).map { it.id }
        )
    }

    @Test
    fun campsiteWithNoAvailability_doesNotMatchADate() {
        val closed = all.first().copy(availability = emptyList())
        val day = utcMidnight(2027, 5, 8)
        assertEquals(
            emptyList<String>(),
            buildBrowseList(listOf(closed), BrowseFilters(arriveMillis = day)).map { it.id }
        )
    }

    @Test
    fun noCampsites_staysEmpty() {
        assertEquals(emptyList<String>(), buildBrowseList(emptyList(), BrowseFilters()).map { it.id })
    }

    @Test
    fun locationAndGuests_bothHaveToMatch() {
        val shown = buildBrowseList(all, BrowseFilters(location = "banff", guestsText = "7"))
        assertEquals(emptyList<String>(), shown.map { it.id })
    }

    private fun utcMidnight(year: Int, month: Int, day: Int): Long =
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis
}
