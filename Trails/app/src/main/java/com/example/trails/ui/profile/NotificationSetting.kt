// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Notifications
// USER STORIES: 01.10.01 (opt out of receiving notifications)
// PURPOSE: The "Notifications" on/off row shown in Settings (Figma Scenario 1 · Notifications off).
// Outstanding issues: None currently known.
// AI assistance: Used AI to help build the notification switch row.
package com.example.trails.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trails.R
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects the notifications switch to the current user's profile. Shows nothing until
 * the profile is loaded.
 *
 * @param modifier modifier for the row.
 * @param viewModel the profile controller.
 */
@Composable
fun NotificationSettingRow(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    val failed by viewModel.notificationUpdateFailed.collectAsState()
    val user = (state as? ProfileUiState.Loaded)?.user ?: return
    NotificationSettingContent(
        enabled = !user.notificationsOptOut,
        failed = failed,
        onEnabledChange = viewModel::onNotificationsEnabledChange,
        modifier = modifier
    )
}

/**
 * Stateless notifications row: label, hint and a switch.
 *
 * @param enabled true when the user receives notifications.
 * @param failed true if the last change could not be saved.
 * @param onEnabledChange called with the new value when the switch is tapped.
 * @param modifier modifier for the row.
 */
@Composable
fun NotificationSettingContent(
    enabled: Boolean,
    failed: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.profile_notifications_title)) },
            supportingContent = {
                Text(
                    stringResource(
                        if (enabled) R.string.profile_notifications_on else R.string.profile_notifications_off
                    )
                )
            },
            trailingContent = {
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.testTag(NotificationSettingTags.SWITCH)
                )
            },
            modifier = Modifier.padding(horizontal = 8.dp).testTag(NotificationSettingTags.ROW)
        )
        if (failed) {
            Text(
                stringResource(R.string.profile_notifications_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp).testTag(NotificationSettingTags.ERROR)
            )
        }
    }
}

/** Test tags used by UI tests to find elements in this row. */
object NotificationSettingTags {
    const val ROW = "notification_setting_row"
    const val SWITCH = "notification_setting_switch"
    const val ERROR = "notification_setting_error"
}

/** Design-time preview with notifications on. */
@Preview(showBackground = true)
@Composable
private fun NotificationSettingPreview() {
    TrailsTheme { NotificationSettingContent(enabled = true, failed = false, onEnabledChange = {}) }
}
