// Unit tests for browse search and filters (US 01.01.01, 01.01.02).
// OWNER: Neaz (NeazMZ06) — Search & Discovery
package com.example.trails.ui.discovery

import com.example.trails.data.fake.TestCampsites
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.GregorianCalendar
import java.util.TimeZone

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
    fun location_jasper_keepsWhistlers() {
        val shown = buildBrowseList(all, BrowseFilters(location = "jasper"))
        assertEquals(listOf(TestCampsites.JASPER_ID), shown.map { it.id })
    }

    @Test
    fun guests7_keepsOnlyHinton() {
        val shown = buildBrowseList(all, BrowseFilters(guestsText = "7"))
        assertEquals(listOf(TestCampsites.HINTON_ID), shown.map { it.id })
    }

    @Test
    fun may82027_keepsOnlyHinton() {
        val day = utcMidnight(2027, 5, 8)
        val shown = buildBrowseList(all, BrowseFilters(arriveMillis = day, leaveMillis = day))
        assertEquals(listOf(TestCampsites.HINTON_ID), shown.map { it.id })
    }

    private fun utcMidnight(year: Int, month: Int, day: Int): Long =
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis
}
