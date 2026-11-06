// Unit tests for the fixed App policy cards (US 01.11.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import org.junit.Assert.assertEquals
import org.junit.Test

class PolicyContentTest {

    // This checks that the App policy screen has exactly the four cards from the design.
    @Test
    fun appPolicy_hasFourCards() {
        assertEquals(4, appPolicyCards.size)
    }

    // This checks that no two App policy cards share the same title.
    @Test
    fun appPolicy_titlesAreDistinct() {
        val titles = appPolicyCards.map { it.title }
        assertEquals(titles.size, titles.toSet().size)
    }
}
