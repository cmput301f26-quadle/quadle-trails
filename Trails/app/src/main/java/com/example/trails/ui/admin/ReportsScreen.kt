// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Admin list of campsite owners with open policy reports (Figma Scenario 8 ·
//   Policy reports); tapping one opens their Policy review page.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestReports
import com.example.trails.data.fake.TestUsers
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.TrailsTheme
import com.google.firebase.Timestamp

/**
 * Connects [ReportsViewModel] to navigation and renders [ReportsContent].
 *
 * @param navController used to go back and to open an owner's Policy review page.
 */
@Composable
fun ReportsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = viewModel { ReportsViewModel() }
) {
    val state by viewModel.state.collectAsState()
    ReportsContent(
        state = state,
        onBack = { navController.popBackStack() },
        // Only the id is passed; the review page loads the owner itself.
        onOpenOwner = { id -> navController.navigate(AdminRoutes.adminPolicyReview(id)) },
        modifier = modifier
    )
}

/**
 * Stateless Policy reports UI, split out so UI tests can drive it directly.
 *
 * @param state what to show: a spinner, the empty message, or the reported owners.
 * @param onBack called when the back arrow is tapped.
 * @param onOpenOwner called with an owner's device id when their row is tapped.
 */
@Composable
fun ReportsContent(
    state: ReportsUiState,
    onBack: () -> Unit,
    onOpenOwner: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrailsTopBar(
                title = stringResource(R.string.admin_reports_title),
                onBack = onBack,
                backTestTag = ReportsTags.BACK,
                backContentDescription = stringResource(R.string.admin_profiles_back)
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            when {
                state is ReportsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.testTag(ReportsTags.LOADING))
                }
                state is ReportsUiState.Loaded && state.owners.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.admin_reports_empty), modifier = Modifier.testTag(ReportsTags.EMPTY))
                    }
                state is ReportsUiState.Loaded -> LazyColumn(
                    modifier = Modifier.testTag(ReportsTags.LIST),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.owners, key = { it.user.deviceId }) { owner ->
                        OwnerRow(owner, onClick = { onOpenOwner(owner.user.deviceId) })
                    }
                }
            }
        }
    }
}

/** Row: avatar, name, grey "n reports · oldest date" line and chevron; the whole row is tappable. */
@Composable
private fun OwnerRow(owner: ReportedOwner, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ReportsTags.row(owner.user.deviceId))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(owner.user)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(adminProfileDisplayName(owner.user), style = MaterialTheme.typography.titleMedium)
                Text(
                    reportedOwnerSubtitle(owner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object ReportsTags {
    const val BACK = "admin_reports_back"
    const val LOADING = "admin_reports_loading"
    const val EMPTY = "admin_reports_empty"
    const val LIST = "admin_reports_list"

    /** Tag for one owner's row. */
    fun row(id: String) = "admin_reports_row_$id"
}

/** Design-time preview with the sample reports, for comparing against Scenario 8. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ReportsContentPreview() {
    TrailsTheme {
        ReportsContent(
            state = ReportsUiState.Loaded(
                buildReportedOwners(TestReports.all, TestUsers.all, TestCampsites.all, Timestamp.now())
            ),
            onBack = {},
            onOpenOwner = {}
        )
    }
}

/** Design-time preview of the empty state. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ReportsEmptyPreview() {
    TrailsTheme {
        ReportsContent(state = ReportsUiState.Loaded(emptyList()), onBack = {}, onOpenOwner = {})
    }
}
