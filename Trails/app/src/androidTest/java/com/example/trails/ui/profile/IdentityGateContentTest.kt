// Intent tests for the identity gate screens (US 01.09.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
package com.example.trails.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdentityGateContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun starting_showsTheLoadingScreen() {
        composeRule.setContent { TrailsTheme { IdentityStartingContent() } }
        composeRule.onNodeWithTag(IdentityGateTags.STARTING).assertIsDisplayed()
    }

    @Test
    fun failed_showsTryAgain() {
        composeRule.setContent { TrailsTheme { IdentityFailedContent(onRetry = {}) } }
        composeRule.onNodeWithTag(IdentityGateTags.FAILED).assertIsDisplayed()
        composeRule.onNodeWithTag(IdentityGateTags.RETRY).assertIsDisplayed()
    }

    @Test
    fun failed_tryAgainCallsRetry() {
        var retries = 0
        composeRule.setContent { TrailsTheme { IdentityFailedContent(onRetry = { retries++ }) } }
        composeRule.onNodeWithTag(IdentityGateTags.RETRY).performClick()
        assertEquals(1, retries)
    }
}
