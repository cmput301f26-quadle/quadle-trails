// Recognizes the device before the app opens, then shows Welcome or Welcome back (US 01.09.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: From the greeting, "Manage profile" and "App policy" open the app's home instead of those screens.
package com.example.trails.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trails.R
import com.example.trails.ui.theme.TrailsTheme

/**
 * Shows [content] only once the user's identity is established, after greeting the
 * user as in Scenario 1 "Open the app": Welcome on a new device, Welcome back on a
 * recognised one. When the identity changes, for example after a profile is deleted,
 * everything is rebuilt from scratch so no screen keeps the old id.
 *
 * @param viewModel establishes the identity.
 * @param content the app.
 */
@Composable
fun IdentityGate(
    viewModel: IdentityViewModel = viewModel { IdentityViewModel() },
    content: @Composable () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val current = state) {
            IdentityState.Starting -> IdentityStartingContent()
            is IdentityState.Failed -> IdentityFailedContent(onRetry = viewModel::retry)
            is IdentityState.Ready -> key(current.id) { GreetingThenApp(current.id, content) }
        }
    }
}

/**
 * Welcome / Welcome back (and Device profile from there), then the app once the user
 * continues. Mode switches made here are saved, and the app opens in that mode.
 */
@Composable
private fun GreetingThenApp(id: String, content: @Composable () -> Unit) {
    var entered by rememberSaveable { mutableStateOf(false) }
    if (entered) {
        content()
        return
    }
    val profile: ProfileViewModel = viewModel(key = "greeting-$id") { ProfileViewModel(deviceId = id) }
    var showingDeviceProfile by rememberSaveable { mutableStateOf(false) }
    val state by profile.uiState.collectAsState()

    when (val current = state) {
        ProfileUiState.Loading -> ProfileLoadingContent()
        is ProfileUiState.Error -> ProfileErrorContent(onRetry = profile::retry)
        ProfileUiState.NoProfile, ProfileUiState.Deleted -> WelcomeContent(
            deviceIdLabel = profile.deviceIdLabel,
            onContinue = {
                profile.ensureProfileExists()
                entered = true
            }
        )
        is ProfileUiState.Loaded -> if (showingDeviceProfile) {
            DeviceProfileContent(
                user = current.user,
                deviceIdLabel = profile.deviceIdLabel,
                onBack = { showingDeviceProfile = false },
                onManageProfile = { entered = true },
                onPolicy = { entered = true }
            )
        } else {
            WelcomeBackContent(
                user = current.user,
                deviceIdLabel = profile.deviceIdLabel,
                onContinue = { entered = true },
                onOpenDeviceProfile = { showingDeviceProfile = true },
                pill = { ModePill(navController = null, viewModel = profile) }
            )
        }
    }
}

/**
 * Shown while the identity is being established.
 *
 * @param modifier modifier for the screen.
 */
@Composable
fun IdentityStartingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(IdentityGateTags.STARTING),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.identity_starting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Shown when the identity could not be established.
 *
 * @param onRetry called when the user taps "Try again".
 * @param modifier modifier for the screen.
 */
@Composable
fun IdentityFailedContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(IdentityGateTags.FAILED),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.identity_failed_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.identity_failed_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry, modifier = Modifier.testTag(IdentityGateTags.RETRY)) {
            Text(stringResource(R.string.identity_retry))
        }
    }
}

/** Test tags for the identity gate. */
object IdentityGateTags {
    const val STARTING = "identity_starting"
    const val FAILED = "identity_failed"
    const val RETRY = "identity_retry"
}

/** Preview of the loading state. */
@Preview(showBackground = true)
@Composable
private fun IdentityStartingPreview() {
    TrailsTheme { IdentityStartingContent() }
}

/** Preview of the failed state. */
@Preview(showBackground = true)
@Composable
private fun IdentityFailedPreview() {
    TrailsTheme { IdentityFailedContent(onRetry = {}) }
}
