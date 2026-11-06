// Intent tests for the mode pill and the "Use Quadle as" list (US 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write Compose UI tests for role selection.
package com.example.trails.ui.profile

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChooseModeContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val zainab = User(
        deviceId = "d1", name = "Zainab Saghir", email = "zainab_admin",
        roles = listOf(Role.ENTRANT, Role.OWNER, Role.ADMIN), activeMode = Role.ADMIN
    )
    private val adminCamper = User(
        deviceId = "d2", name = "Morgan Lee",
        roles = listOf(Role.ENTRANT, Role.ADMIN), activeMode = Role.ADMIN
    )

    private var switchedTo: Role? = null
    private var profileOpened = 0

    private fun show(user: User, modeSwitch: ModeSwitchState = ModeSwitchState.Idle) {
        composeRule.setContent {
            TrailsTheme {
                ChooseModeContent(
                    user, modeSwitch,
                    onSwitch = { switchedTo = it },
                    onProfile = { profileOpened++ },
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
    }

    @Test
    fun listsEveryHeldRole() {
        show(zainab)
        Role.entries.forEach { composeRule.onNodeWithTag(ChooseModeTags.option(it)).assertIsDisplayed() }
    }

    @Test
    fun hidesRolesTheUserDoesNotHold() {
        show(adminCamper)
        composeRule.onNodeWithTag(ChooseModeTags.option(Role.OWNER)).assertDoesNotExist()
    }

    @Test
    fun marksTheCurrentMode() {
        show(zainab)
        composeRule.onNodeWithTag(ChooseModeTags.CURRENT_BADGE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ChooseModeTags.option(Role.ADMIN)).assertTextContains("Current", substring = true)
    }

    @Test
    fun showsUseQuadleAsAndTheUsersName() {
        show(zainab)
        composeRule.onNodeWithText("USE QUADLE AS").assertIsDisplayed()
        composeRule.onNodeWithText("Zainab Saghir").assertIsDisplayed()
    }

    @Test
    fun switchIsDisabledUntilAnotherModeIsPicked() {
        show(adminCamper)
        composeRule.onNodeWithTag(ChooseModeTags.SWITCH).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun pickingOwner_switchesToOwner() {
        show(zainab)
        composeRule.onNodeWithTag(ChooseModeTags.option(Role.OWNER)).performClick()
        composeRule.onNodeWithTag(ChooseModeTags.SWITCH).performScrollTo().assertTextContains("Switch to Owner").performClick()
        assertEquals(Role.OWNER, switchedTo)
    }

    @Test
    fun pickingCamper_switchesToEntrant() {
        show(adminCamper)
        composeRule.onNodeWithTag(ChooseModeTags.option(Role.ENTRANT)).performClick()
        composeRule.onNodeWithTag(ChooseModeTags.SWITCH).performScrollTo().performClick()
        assertEquals(Role.ENTRANT, switchedTo)
    }

    @Test
    fun profileLink_opensTheDeviceProfile() {
        show(zainab)
        composeRule.onNodeWithTag(ChooseModeTags.PROFILE_LINK).performClick()
        assertEquals(1, profileOpened)
    }

    @Test
    fun failure_isShownAndNothingIsSwitched() {
        show(adminCamper, ModeSwitchState.Failed("offline"))
        composeRule.onNodeWithTag(ChooseModeTags.ERROR).performScrollTo().assertIsDisplayed()
        assertNull(switchedTo)
    }

    @Test
    fun pill_showsTheModeAndIsTappable() {
        var taps = 0
        composeRule.setContent { TrailsTheme { ModePillChip(Role.ADMIN, onClick = { taps++ }) } }
        composeRule.onNodeWithTag(ModePillTags.PILL).assertTextContains("Admin").performClick()
        assertEquals(1, taps)
    }
}
