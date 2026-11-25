// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Admin's Policy review page for one reported owner (Figma Scenario 8 · Policy review):
//   the rule broken, their recent reports, and Remove owner / Dismiss reports.
// Outstanding issues:
//   - Guests of the removed owner's campsites aren't notified, since reservations and
//     notifications don't exist yet (Part 4).
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestReports
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Report
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme
import com.google.firebase.Timestamp

/** Amber used for the report count chip and the rule broken card. Not sampled from Figma. */
internal val ReportAmber = Color(0xFFF59E0B)

/**
 * Connects [PolicyReviewViewModel] to navigation and renders [PolicyReviewContent].
 * Only the owner's id arrives through navigation; the ViewModel loads the rest itself.
 *
 * @param navController used to go back to the Policy reports list.
 * @param userId device id of the reported owner.
 */
@Composable
fun PolicyReviewScreen(
    navController: NavController,
    userId: String,
    modifier: Modifier = Modifier,
    viewModel: PolicyReviewViewModel = viewModel(key = userId) { PolicyReviewViewModel(userId) }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.done.collect { navController.popBackStack() }
    }
    PolicyReviewContent(
        state = state,
        onBack = { navController.popBackStack() },
        onRemoveClick = viewModel::onRemoveClick,
        onDismissDialog = viewModel::onDismissDialog,
        onConfirmRemove = viewModel::onConfirmRemove,
        onDismissReports = viewModel::onDismissReports,
        modifier = modifier
    )
}

/**
 * Stateless Policy review UI, split out so UI tests can drive it directly.
 *
 * @param state what to show: a spinner, a not-found message, or the owner with their reports.
 * @param onBack called when the back arrow is tapped.
 * @param onRemoveClick called when "Remove owner" is tapped.
 * @param onDismissDialog called when Cancel is tapped or the dialog is dismissed.
 * @param onConfirmRemove called when Remove is tapped in the dialog.
 * @param onDismissReports called when "Dismiss reports" is tapped.
 */
@Composable
fun PolicyReviewContent(
    state: PolicyReviewUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveClick: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onConfirmRemove: () -> Unit = {},
    onDismissReports: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrailsTopBar(
                title = stringResource(R.string.admin_policy_review_title),
                onBack = onBack,
                backTestTag = PolicyReviewTags.BACK,
                backContentDescription = stringResource(R.string.admin_profiles_back)
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (state) {
                PolicyReviewUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.testTag(PolicyReviewTags.LOADING))
                }
                PolicyReviewUiState.NotFound -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.admin_policy_review_not_found),
                        modifier = Modifier.testTag(PolicyReviewTags.NOT_FOUND)
                    )
                }
                is PolicyReviewUiState.Loaded -> {
                    ReviewDetails(state, onRemoveClick, onDismissReports)
                    if (state.action == PolicyActionState.Confirming) {
                        AdminRemoveDialog(
                            title = stringResource(R.string.admin_policy_review_remove_title),
                            message = removeProfileMessage(
                                adminProfileDisplayName(state.user),
                                state.impact.campsiteCount,
                                state.impact.entryCount
                            ),
                            removing = false,
                            tags = PolicyReviewTags.dialogTags,
                            onCancel = onDismissDialog,
                            onRemove = onConfirmRemove
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header, rule broken card, report cards and impact line, then (unless it's the admin
 * themselves) any error and the Remove owner / Dismiss reports buttons.
 */
@Composable
private fun ReviewDetails(
    state: PolicyReviewUiState.Loaded,
    onRemoveClick: () -> Unit,
    onDismissReports: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            OwnerHeader(state)
            state.ruleBroken?.let {
                Spacer(Modifier.height(24.dp))
                RuleBrokenCard(it)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.admin_policy_review_reports_heading),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.reports.forEach { report ->
                    ReportCard(report, state.campsiteNames[report.campsiteId] ?: report.campsiteId)
                }
            }
            val impactLine = removeOwnerImpactLine(state.impact.campsiteCount, state.impact.entryCount)
            if (impactLine.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    impactLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(PolicyReviewTags.IMPACT)
                )
            }
        }
        if (!state.isSelf) ActionButtons(state.action, onRemoveClick, onDismissReports)
    }
}

/** Avatar, name, "Owner · n campsites · Joined" line, and the Owner / report count chips. */
@Composable
private fun OwnerHeader(state: PolicyReviewUiState.Loaded) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        LargeAvatar(state.user)
        Spacer(Modifier.height(12.dp))
        Text(
            adminProfileDisplayName(state.user),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag(PolicyReviewTags.NAME)
        )
        Text(
            policyOwnerLine(state.user, state.campsiteCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(PolicyReviewTags.OWNER_LINE)
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.testTag(PolicyReviewTags.CHIPS), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminChip(stringResource(R.string.admin_policy_review_owner_chip), TrailGreen)
            AdminChip(reportCountLabel(state.reports.size), ReportAmber)
        }
    }
}

/** Amber outlined card with the small "RULE BROKEN" label and the rule. */
@Composable
private fun RuleBrokenCard(rule: String) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ReportAmber.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, ReportAmber),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PolicyReviewTags.RULE_CARD)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.admin_policy_review_rule_label),
                style = MaterialTheme.typography.labelSmall,
                color = ReportAmber
            )
            Spacer(Modifier.height(4.dp))
            Text(rule, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** One report: the reason in bold and the grey campsite · campers · date line. */
@Composable
private fun ReportCard(report: Report, campsiteName: String) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PolicyReviewTags.report(report.id))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(report.reason, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                reportSubtitle(report, campsiteName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Error text if the last action failed, then the red Remove owner and green outlined Dismiss reports buttons. */
@Composable
private fun ActionButtons(action: PolicyActionState, onRemoveClick: () -> Unit, onDismissReports: () -> Unit) {
    val working = action == PolicyActionState.Working
    if (action == PolicyActionState.Error) {
        Text(
            stringResource(R.string.admin_policy_review_error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag(PolicyReviewTags.ERROR)
        )
    }
    AdminRemoveButton(
        label = stringResource(R.string.admin_policy_review_remove),
        tag = PolicyReviewTags.REMOVE_BUTTON,
        enabled = !working,
        onClick = onRemoveClick
    )
    OutlinedButton(
        onClick = onDismissReports,
        enabled = !working,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, TrailGreen),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag(PolicyReviewTags.DISMISS_BUTTON)
    ) {
        Text(stringResource(R.string.admin_policy_review_dismiss), color = TrailGreen)
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object PolicyReviewTags {
    const val BACK = "admin_policy_review_back"
    const val LOADING = "admin_policy_review_loading"
    const val NOT_FOUND = "admin_policy_review_not_found"
    const val NAME = "admin_policy_review_name"
    const val OWNER_LINE = "admin_policy_review_owner_line"
    const val CHIPS = "admin_policy_review_chips"
    const val RULE_CARD = "admin_policy_review_rule"
    const val IMPACT = "admin_policy_review_impact"
    const val ERROR = "admin_policy_review_error"
    const val REMOVE_BUTTON = "admin_policy_review_remove"
    const val DISMISS_BUTTON = "admin_policy_review_dismiss"
    const val DIALOG = "admin_policy_review_dialog"
    const val DIALOG_MESSAGE = "admin_policy_review_dialog_message"
    const val DIALOG_CANCEL = "admin_policy_review_dialog_cancel"
    const val DIALOG_REMOVE = "admin_policy_review_dialog_remove"

    /** Tag for one report's card. */
    fun report(id: String) = "admin_policy_review_report_$id"

    /** The dialog's tags, bundled for [AdminRemoveDialog]. */
    internal val dialogTags = AdminRemoveDialogTags(DIALOG, DIALOG_MESSAGE, DIALOG_CANCEL, DIALOG_REMOVE)
}

/** The sample owner's review state, for the previews. */
private fun previewState(action: PolicyActionState = PolicyActionState.Idle) = buildPolicyReviewState(
    user = TestUsers.all.first { it.deviceId == TestUsers.OWNER_ID },
    reports = TestReports.all,
    campsites = TestCampsites.all,
    impact = null,
    currentUserId = TestUsers.ADMIN_ID,
    now = Timestamp.now(),
    action = action
)

/** Design-time preview of the sample owner, for comparing against Scenario 8. */
@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PolicyReviewPreview() {
    TrailsTheme {
        PolicyReviewContent(state = previewState(), onBack = {})
    }
}

/** Design-time preview of the "Remove owner?" dialog. */
@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PolicyReviewRemoveDialogPreview() {
    TrailsTheme {
        PolicyReviewContent(state = previewState(PolicyActionState.Confirming), onBack = {})
    }
}

/** Design-time preview of an action that failed. */
@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PolicyReviewErrorPreview() {
    TrailsTheme {
        PolicyReviewContent(state = previewState(PolicyActionState.Error), onBack = {})
    }
}
