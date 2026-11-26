// Intent tests for "Become a host" in the "Use Quadle as" list (US 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write Compose UI tests for the Become a host option.
package com.example.trails.ui.profile

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI tests for the "Become a host" card in [ChooseModeContent]. */
@RunWith(AndroidJUnit4::class)
class BecomeHostContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val priya = User(
        deviceId = "device-entrant-001", name = "Priya Sharma", email = "priya.sharma@example.com",
        roles = listOf(Role.ENTRANT), activeMode = Role.ENTRANT
    )
    private val daniel = User(
        deviceId = "device-owner-001", name = "Daniel Whitehorse", email = "daniel.whitehorse@example.com",
        roles = listOf(Role.ENTRANT, Role.OWNER), activeMode = Role.OWNER
    )

    private var becameHost = 0

    /** Shows the mode list for [user] and counts "Become a host" taps. */
    private fun show(user: User, modeSwitch: ModeSwitchState = ModeSwitchState.Idle) {
        composeRule.setContent {
            TrailsTheme {
                ChooseModeContent(
                    user, modeSwitch,
                    onSwitch = {},
                    onProfile = null,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    onBecomeHost = { becameHost++ }
                )
            }
        }
    }

    // A camper sees "Become a host", and tapping it starts hosting.
    @Test
    fun camper_seesBecomeHost_andTapStartsHosting() {
        show(priya)
        composeRule.onNodeWithTag(ChooseModeTags.BECOME_HOST).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Become a host").assertIsDisplayed()
        composeRule.onNodeWithTag(ChooseModeTags.BECOME_HOST).performClick()
        assertEquals(1, becameHost)
    }

    // Someone who is already an owner is not offered it again.
    @Test
    fun owner_doesNotSeeBecomeHost() {
        show(daniel)
        composeRule.onNodeWithTag(ChooseModeTags.BECOME_HOST).assertDoesNotExist()
    }

    // The card cannot be tapped again while the switch is saving.
    @Test
    fun whileSwitching_becomeHostIsDisabled() {
        show(priya, ModeSwitchState.Switching)
        composeRule.onNodeWithTag(ChooseModeTags.BECOME_HOST).performScrollTo().assertIsNotEnabled()
    }

    // A camper is never offered Admin mode.
    @Test
    fun camper_isNeverOfferedAdmin() {
        show(priya)
        composeRule.onNodeWithTag(ChooseModeTags.option(Role.ADMIN)).assertDoesNotExist()
        composeRule.onNodeWithTag(ChooseModeTags.option(Role.ENTRANT)).assertIsDisplayed()
    }
}
