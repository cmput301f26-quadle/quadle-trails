// Intent tests for the notifications on/off row in Settings (US 01.10.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write Compose UI tests for the notification switch.
package com.example.trails.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trails.ui.shell.SettingsContent
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI tests for [NotificationSettingContent] and its place in Settings. */
@RunWith(AndroidJUnit4::class)
class NotificationSettingContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var changedTo: Boolean? = null

    private fun show(enabled: Boolean, failed: Boolean = false) {
        composeRule.setContent {
            TrailsTheme { NotificationSettingContent(enabled, failed, onEnabledChange = { changedTo = it }) }
        }
    }

    @Test
    fun enabled_switchIsOn() {
        show(enabled = true)
        composeRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeRule.onNodeWithTag(NotificationSettingTags.SWITCH).assertIsOn()
    }

    @Test
    fun optedOut_switchIsOffAndSaysSo() {
        show(enabled = false)
        composeRule.onNodeWithTag(NotificationSettingTags.SWITCH).assertIsOff()
        composeRule.onNodeWithText("Off · you won't receive notifications").assertIsDisplayed()
    }

    @Test
    fun tappingSwitch_optsOut() {
        show(enabled = true)
        composeRule.onNodeWithTag(NotificationSettingTags.SWITCH).performClick()
        assertEquals(false, changedTo)
    }

    @Test
    fun tappingSwitchWhenOff_optsBackIn() {
        show(enabled = false)
        composeRule.onNodeWithTag(NotificationSettingTags.SWITCH).performClick()
        assertEquals(true, changedTo)
    }

    @Test
    fun saveFailure_isShown() {
        show(enabled = true, failed = true)
        composeRule.onNodeWithTag(NotificationSettingTags.ERROR).assertIsDisplayed()
    }

    @Test
    fun settings_showsTheNotificationRowWhenGiven() {
        composeRule.setContent {
            TrailsTheme {
                SettingsContent(
                    onProfileClick = {}, onPolicyClick = {},
                    notificationSetting = { NotificationSettingContent(true, false, onEnabledChange = {}) }
                )
            }
        }
        composeRule.onNodeWithTag(NotificationSettingTags.ROW).assertIsDisplayed()
    }
}
