// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.04.01, 03.05.01, 03.07.01
// PURPOSE: Admin's home screen (Home tab in Admin mode, Figma Scenario 8 · Admin home): greeting,
//   a Needs review card for open policy reports, and live Campsites / Profiles counts that open
//   the admin lists.
// Outstanding issues: no mode pill in the header yet (US 03.11.01 is Aarav's); the Images and Logs
//   parts come in Part 4.
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
import com.example.trails.data.fake.TestReports
import com.example.trails.data.fake.TestUsers
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects [AdminHomeViewModel] to navigation and renders [AdminHomeContent].
 *
 * @param navController used to open the admin campsites, profiles and policy reports screens.
 * @param modifier modifier for the screen.
 * @param viewModel supplies the greeting, the counts and the open reports.
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
        onNeedsReviewClick = { navController.navigate(AdminRoutes.ADMIN_REPORTS) },
        modifier = modifier
    )
}

/**
 * Stateless admin home UI, split out so UI tests can drive it directly.
 *
 * @param state what to show, or null while loading.
 * @param onCampsitesClick called when the Campsites tile is tapped (US 03.04.01).
 * @param onProfilesClick called when the Profiles tile is tapped (US 03.05.01).
 * @param onNeedsReviewClick called when the Needs review card is tapped (US 03.07.01).
 * @param modifier modifier for the screen.
 */
@Composable
fun AdminHomeContent(
    state: AdminHomeUiState?,
    onCampsitesClick: () -> Unit,
    onProfilesClick: () -> Unit,
    onNeedsReviewClick: () -> Unit = {},
    modifier: Modifier = Modifier
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        if (state.openReportCount > 0) {
            NeedsReviewCard(state, onNeedsReviewClick)
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

/**
 * Amber outlined card with the "Needs review" chip, the open report count, how long the oldest
 * report has waited and a chevron; the whole card is tappable.
 */
@Composable
private fun NeedsReviewCard(state: AdminHomeUiState, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ReportAmber.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, ReportAmber),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AdminHomeTags.NEEDS_REVIEW)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminChip(stringResource(R.string.admin_home_needs_review), ReportAmber)
                Text(
                    state.reportCountLine,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(AdminHomeTags.NEEDS_REVIEW_COUNT)
                )
                state.oldestReportLine?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(AdminHomeTags.NEEDS_REVIEW_OLDEST)
                    )
                }
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    const val NEEDS_REVIEW = "admin_home_needs_review"
    const val NEEDS_REVIEW_COUNT = "admin_home_needs_review_count"
    const val NEEDS_REVIEW_OLDEST = "admin_home_needs_review_oldest"
}

/** Design-time preview of the admin home with the sample campsites, users and reports. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminHomeContentPreview() {
    TrailsTheme {
        AdminHomeContent(
            state = buildAdminHomeState(
                user = TestUsers.all.firstOrNull { it.deviceId == TestUsers.ADMIN_ID },
                campsites = TestCampsites.all,
                users = TestUsers.all,
                hour = 9,
                reports = TestReports.all
            ),
            onCampsitesClick = {},
            onProfilesClick = {}
        )
    }
}
