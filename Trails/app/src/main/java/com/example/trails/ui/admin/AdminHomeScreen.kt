// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.04.01, 03.05.01, 03.11.01
// PURPOSE: Admin's home screen (Home tab in Admin mode, Figma Scenario 8 · Admin home): greeting
//   with the mode pill at the top right, and live Campsites / Profiles counts that open the admin lists.
// Outstanding issues: the Images, Logs and Needs review parts come in Part 4.
package com.example.trails.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Role
import com.example.trails.ui.profile.ModePill
import com.example.trails.ui.profile.ModePillChip
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects [AdminHomeViewModel] to navigation and renders [AdminHomeContent].
 *
 * @param navController used to open the admin campsites and profiles screens, and by the
 *   mode pill to go to the new mode's home after a switch (US 03.11.01).
 * @param modifier modifier for the screen.
 * @param viewModel supplies the greeting and the counts.
 */
@Composable
fun AdminHomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AdminHomeViewModel = viewModel { AdminHomeViewModel() }
) {
    val state by viewModel.state.collectAsState()
    AdminHomeContent(
        state = state,
        onCampsitesClick = { navController.navigate(AdminRoutes.ADMIN_CAMPSITES) },
        onProfilesClick = { navController.navigate(AdminRoutes.ADMIN_PROFILES) },
        modifier = modifier,
        modePill = { ModePill(navController) }
    )
}

/**
 * Stateless admin home UI, split out so UI tests can drive it directly.
 *
 * @param state what to show, or null while loading.
 * @param onCampsitesClick called when the Campsites tile is tapped (US 03.04.01).
 * @param onProfilesClick called when the Profiles tile is tapped (US 03.05.01).
 * @param modifier modifier for the screen.
 * @param modePill the mode pill shown at the top right of the header (US 03.11.01); empty by
 *   default so tests and previews don't need a ViewModel.
 */
@Composable
fun AdminHomeContent(
    state: AdminHomeUiState?,
    onCampsitesClick: () -> Unit,
    onProfilesClick: () -> Unit,
    modifier: Modifier = Modifier,
    modePill: @Composable () -> Unit = {}
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(AdminHomeTags.SCREEN),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag(AdminHomeTags.LOADING))
            }
            return@Column
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    state.greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(AdminHomeTags.GREETING)
                )
                Text(
                    stringResource(R.string.admin_home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(Modifier.testTag(AdminHomeTags.MODE_PILL)) {
                modePill()
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CountTile(
                count = state.campsiteCount,
                label = stringResource(R.string.admin_home_campsites),
                onClick = onCampsitesClick,
                tag = AdminHomeTags.CAMPSITES,
                modifier = Modifier.weight(1f)
            )
            CountTile(
                count = state.profileCount,
                label = stringResource(R.string.admin_home_profiles),
                onClick = onProfilesClick,
                tag = AdminHomeTags.PROFILES,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Tappable tile: a big count with a label under it, in the same card style as the admin lists. */
@Composable
private fun CountTile(count: Int, label: String, onClick: () -> Unit, tag: String, modifier: Modifier) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.testTag(tag)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object AdminHomeTags {
    const val SCREEN = "admin_home_screen"
    const val GREETING = "admin_home_greeting"
    const val CAMPSITES = "admin_home_campsites"
    const val PROFILES = "admin_home_profiles"
    const val LOADING = "admin_home_loading"
    const val MODE_PILL = "admin_home_mode_pill"
}

/** Design-time preview of the admin home with the sample campsites and users, and the Admin pill. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminHomeContentPreview() {
    TrailsTheme {
        AdminHomeContent(
            state = buildAdminHomeState(
                user = TestUsers.all.firstOrNull { it.deviceId == TestUsers.ADMIN_ID },
                campsites = TestCampsites.all,
                users = TestUsers.all,
                hour = 9
            ),
            onCampsitesClick = {},
            onProfilesClick = {},
            modePill = { ModePillChip(Role.ADMIN, onClick = {}) }
        )
    }
}
