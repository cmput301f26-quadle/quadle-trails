// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: Settings tab: links to the user's profile and the app policy (Scenario 1 storyboard).
// Outstanding issues: none.
package com.example.trails.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.ui.admin.AdminRoutes
import com.example.trails.ui.profile.ProfileRoutes
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects the settings list to navigation and renders [SettingsContent].
 *
 * @param navController used to open the profile and policy screens.
 * @param modifier modifier for the screen.
 */
@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    SettingsContent(
        onProfileClick = { navController.navigate(ProfileRoutes.PROFILE) },
        onPolicyClick = { navController.navigate(AdminRoutes.POLICY) },
        modifier = modifier
    )
}

/**
 * Stateless settings UI, split out so UI tests can drive it directly.
 *
 * @param onProfileClick called when the "Profile" row is tapped.
 * @param onPolicyClick called when the "App policy & guidelines" row is tapped.
 * @param modifier modifier for the screen.
 */
@Composable
fun SettingsContent(
    onProfileClick: () -> Unit,
    onPolicyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .testTag(SettingsTags.SCREEN)
    ) {
        Text(
            stringResource(R.string.shell_settings_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        SettingsRow(stringResource(R.string.shell_settings_profile), onProfileClick, SettingsTags.PROFILE_ROW)
        HorizontalDivider()
        SettingsRow(stringResource(R.string.shell_settings_policy), onPolicyClick, SettingsTags.POLICY_ROW)
    }
}

/** One tappable row: label on the left, chevron on the right. */
@Composable
private fun SettingsRow(text: String, onClick: () -> Unit, tag: String) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = {
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag(tag)
    )
}

/** Test tags used by UI tests to find elements on this screen. */
object SettingsTags {
    const val SCREEN = "settings_screen"
    const val PROFILE_ROW = "settings_profile_row"
    const val POLICY_ROW = "settings_policy_row"
}

/** Design-time preview of the settings list. */
@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    TrailsTheme { SettingsContent(onProfileClick = {}, onPolicyClick = {}) }
}
