// Intent tests for Welcome, Welcome back, Device profile and Profile deleted (US 01.09.01, 01.08.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
package com.example.trails.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val alex = User(
        deviceId = "d1", name = "Alex Morgan", email = "alex.morgan@email.com",
        roles = listOf(Role.ENTRANT), activeMode = Role.ENTRANT
    )

    private var taps = 0

    // Welcome

    @Test
    fun welcome_showsHeroIdentifierAndNoPasswordMessage() {
        composeRule.setContent { TrailsTheme { WelcomeContent("QD-••••-7A2F", onContinue = {}) } }
        composeRule.onNodeWithTag(ProfileTags.WELCOME).assertIsDisplayed()
        composeRule.onNodeWithTag(ProfileTags.HERO).assertIsDisplayed()
        composeRule.onNodeWithText("No username or password").assertIsDisplayed()
        composeRule.onNodeWithTag(ProfileTags.DEVICE_ID).performScrollTo().assertTextContains("QD-••••-7A2F")
    }

    @Test
    fun welcome_continueToExplore() {
        composeRule.setContent { TrailsTheme { WelcomeContent("QD-••••-7A2F", onContinue = { taps++ }) } }
        composeRule.onNodeWithTag(ProfileTags.CONTINUE).performScrollTo().performClick()
        assertEquals(1, taps)
    }

    // Welcome back

    @Test
    fun welcomeBack_greetsTheStoredName() {
        composeRule.setContent {
            TrailsTheme { WelcomeBackContent(alex, "QD-••••-7A2F", onContinue = {}, onOpenDeviceProfile = {}) }
        }
        composeRule.onNodeWithTag(ProfileTags.GREETING).assertTextContains("Alex Morgan", substring = true)
        composeRule.onNodeWithText("Your camper profile is retained").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun welcomeBack_showsTheModePill() {
        composeRule.setContent {
            TrailsTheme { WelcomeBackContent(alex, "QD-••••-7A2F", onContinue = {}, onOpenDeviceProfile = {}) }
        }
        composeRule.onNodeWithTag(ModePillTags.PILL).assertIsDisplayed()
    }

    @Test
    fun welcomeBack_profileRowOpensDeviceProfile() {
        composeRule.setContent {
            TrailsTheme { WelcomeBackContent(alex, "QD-••••-7A2F", onContinue = {}, onOpenDeviceProfile = { taps++ }) }
        }
        composeRule.onNodeWithTag(ProfileTags.PROFILE_ROW).performClick()
        assertEquals(1, taps)
    }

    @Test
    fun welcomeBack_continueExploring() {
        composeRule.setContent {
            TrailsTheme { WelcomeBackContent(alex, "QD-••••-7A2F", onContinue = { taps++ }, onOpenDeviceProfile = {}) }
        }
        composeRule.onNodeWithTag(ProfileTags.CONTINUE_EXPLORING).performScrollTo().performClick()
        assertEquals(1, taps)
    }

    // Device profile

    @Test
    fun deviceProfile_showsTheUserAndDifferentDeviceNote() {
        composeRule.setContent {
            TrailsTheme { DeviceProfileContent(alex, "QD-••••-7A2F", onBack = {}, onManageProfile = {}, onPolicy = {}) }
        }
        composeRule.onNodeWithTag(ProfileTags.PROFILE_ROW).assertTextContains("Alex Morgan", substring = true)
        composeRule.onNodeWithText("Using a different device?").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deviceProfile_manageProfileOpensProfile() {
        composeRule.setContent {
            TrailsTheme { DeviceProfileContent(alex, "QD-••••-7A2F", onBack = {}, onManageProfile = { taps++ }, onPolicy = {}) }
        }
        composeRule.onNodeWithTag(ProfileTags.MANAGE).performScrollTo().performClick()
        assertEquals(1, taps)
    }

    @Test
    fun deviceProfile_backGoesBack() {
        composeRule.setContent {
            TrailsTheme { DeviceProfileContent(alex, "QD-••••-7A2F", onBack = { taps++ }, onManageProfile = {}, onPolicy = {}) }
        }
        composeRule.onNodeWithTag(ProfileTags.BACK).performClick()
        assertEquals(1, taps)
    }

    // Profile deleted and error

    @Test
    fun deleted_showsBrowseAndSetUpNew() {
        composeRule.setContent { TrailsTheme { ProfileDeletedContent(onBrowse = { taps++ }, onSetUpNew = {}) } }
        composeRule.onNodeWithTag(ProfileTags.DELETED).assertIsDisplayed()
        composeRule.onNodeWithTag(ProfileTags.SET_UP_NEW).assertIsDisplayed()
        composeRule.onNodeWithTag(ProfileTags.BROWSE).performClick()
        assertEquals(1, taps)
    }

    @Test
    fun error_showsRetry() {
        composeRule.setContent { TrailsTheme { ProfileErrorContent(onRetry = { taps++ }) } }
        composeRule.onNodeWithTag(ProfileTags.RETRY).performClick()
        assertEquals(1, taps)
    }
}
