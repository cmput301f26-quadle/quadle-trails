// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.05.01, 03.02.01
// PURPOSE: Admin's view of one user profile, opened from the Profiles list, with a Remove profile
//   button and confirmation dialog (Figma Scenario 8 · Profile, Remove profile?).
// Outstanding issues:
//   - No reservation, lottery-entry or report rows yet (Part 4).
//   - Guests of the removed owner's campsites aren't notified, since reservations and
//     notifications don't exist yet (Part 4).
package com.example.trails.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects [AdminProfileViewModel] to navigation and renders [AdminProfileContent].
 * Only the user's id arrives through navigation; the ViewModel loads the profile itself.
 *
 * @param navController used to go back to the Profiles list.
 * @param userId device id of the user to show.
 */
@Composable
fun AdminProfileScreen(
    navController: NavController,
    userId: String,
    modifier: Modifier = Modifier,
    viewModel: AdminProfileViewModel = viewModel(key = userId) { AdminProfileViewModel(userId) }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.removed.collect { navController.popBackStack() }
    }
    AdminProfileContent(
        state = state,
        onBack = { navController.popBackStack() },
        onRemoveClick = viewModel::onRemoveClick,
        onDismiss = viewModel::onDismiss,
        onConfirmRemove = viewModel::onConfirmRemove,
        modifier = modifier
    )
}

/**
 * Stateless admin Profile UI, split out so UI tests can drive it directly.
 *
 * @param state what to show: a spinner, a not-found message, or the profile with its removal state.
 * @param onBack called when the back arrow is tapped.
 * @param onRemoveClick called when "Remove profile" is tapped.
 * @param onDismiss called when Cancel is tapped or the dialog is dismissed.
 * @param onConfirmRemove called when Remove is tapped in the dialog.
 */
@Composable
fun AdminProfileContent(
    state: AdminProfileUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onConfirmRemove: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrailsTopBar(
                title = stringResource(R.string.admin_profile_title),
                onBack = onBack,
                backTestTag = AdminProfileTags.BACK,
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
                AdminProfileUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.testTag(AdminProfileTags.LOADING))
                }
                AdminProfileUiState.NotFound -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.admin_profile_not_found),
                        modifier = Modifier.testTag(AdminProfileTags.NOT_FOUND)
                    )
                }
                is AdminProfileUiState.Loaded -> {
                    ProfileDetails(state, onRemoveClick)
                    val removing = state.removal == ProfileRemovalState.Removing
                    if (state.removal == ProfileRemovalState.Confirming || removing) {
                        AdminRemoveDialog(
                            title = stringResource(R.string.admin_profile_remove_title),
                            message = removeProfileMessage(
                                adminProfileDisplayName(state.user), state.campsiteCount, state.lotteryEntryCount
                            ),
                            removing = removing,
                            tags = AdminProfileTags.dialogTags,
                            onCancel = onDismiss,
                            onRemove = onConfirmRemove
                        )
                    }
                }
            }
        }
    }
}

/**
 * Avatar, name, email/joined line, role chips, the info card and, unless it's the admin's own
 * profile, the Remove profile button with any error above it.
 */
@Composable
private fun ProfileDetails(state: AdminProfileUiState.Loaded, onRemoveClick: () -> Unit) {
    val user = state.user
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LargeAvatar(user)
            Spacer(Modifier.height(12.dp))
            Text(
                adminProfileDisplayName(user),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag(AdminProfileTags.NAME)
            )
            val subLine = emailAndJoinedLine(user)
            if (subLine.isNotEmpty()) {
                Text(
                    subLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
            RoleChips(roleLabels(user))
            Spacer(Modifier.height(24.dp))
            InfoCard(user, state.campsiteCount)
        }
        if (!state.isSelf) {
            if (state.removal == ProfileRemovalState.Error) {
                Text(
                    stringResource(R.string.admin_profile_remove_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag(AdminProfileTags.ERROR)
                )
            }
            AdminRemoveButton(
                label = stringResource(R.string.admin_profile_remove),
                tag = AdminProfileTags.REMOVE_BUTTON,
                onClick = onRemoveClick
            )
        }
    }
}

/** 72dp round profile photo, or a dark circle with the user's initials when there is none. */
@Composable
private fun LargeAvatar(user: User) {
    val mod = Modifier
        .size(72.dp)
        .clip(CircleShape)
    val url = user.profileImageUrl
    if (url != null) {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = mod)
    } else {
        Box(
            mod.background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                profileInitials(user.name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Outlined green chips, one per role held; display only, not tappable. */
@Composable
private fun RoleChips(labels: List<String>) {
    Row(
        Modifier.testTag(AdminProfileTags.ROLES),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = TrailGreen,
                modifier = Modifier
                    .border(1.dp, TrailGreen, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

/** Card with the Phone row and, for owners, the Campsites owned row. */
@Composable
private fun InfoCard(user: User, campsiteCount: Int) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        InfoRow(stringResource(R.string.admin_profile_phone), phoneLabel(user.phone), AdminProfileTags.PHONE)
        if (Role.OWNER in user.roles) {
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            InfoRow(
                stringResource(R.string.admin_profile_campsites_owned),
                campsiteCount.toString(),
                AdminProfileTags.CAMPSITES
            )
        }
    }
}

/** One label/value row inside the info card; [tag] is on the value. */
@Composable
private fun InfoRow(label: String, value: String, tag: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag(tag))
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object AdminProfileTags {
    const val BACK = "admin_profile_back"
    const val LOADING = "admin_profile_loading"
    const val NOT_FOUND = "admin_profile_not_found"
    const val NAME = "admin_profile_name"
    const val ROLES = "admin_profile_roles"
    const val PHONE = "admin_profile_phone"
    const val CAMPSITES = "admin_profile_campsites"
    const val REMOVE_BUTTON = "admin_profile_remove"
    const val ERROR = "admin_profile_error"
    const val DIALOG = "admin_profile_dialog"
    const val DIALOG_MESSAGE = "admin_profile_dialog_message"
    const val DIALOG_CANCEL = "admin_profile_dialog_cancel"
    const val DIALOG_REMOVE = "admin_profile_dialog_remove"

    /** The dialog's tags, bundled for [AdminRemoveDialog]. */
    internal val dialogTags = AdminRemoveDialogTags(DIALOG, DIALOG_MESSAGE, DIALOG_CANCEL, DIALOG_REMOVE)
}

/** Design-time preview of the sample owner, for comparing against Scenario 8. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminProfileOwnerPreview() {
    TrailsTheme {
        AdminProfileContent(
            state = buildAdminProfileState(
                TestUsers.all.first { it.deviceId == TestUsers.OWNER_ID },
                TestCampsites.all
            ),
            onBack = {}
        )
    }
}

/** Design-time preview of the "Remove profile?" dialog for the sample owner. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminProfileRemoveDialogPreview() {
    TrailsTheme {
        AdminProfileContent(
            state = buildAdminProfileState(
                TestUsers.all.first { it.deviceId == TestUsers.OWNER_ID },
                TestCampsites.all,
                removal = ProfileRemovalState.Confirming
            ),
            onBack = {}
        )
    }
}

/** Design-time preview of a removal that failed. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminProfileRemoveErrorPreview() {
    TrailsTheme {
        AdminProfileContent(
            state = buildAdminProfileState(
                TestUsers.all.first { it.deviceId == TestUsers.OWNER_ID },
                TestCampsites.all,
                removal = ProfileRemovalState.Error
            ),
            onBack = {}
        )
    }
}

/** Design-time preview of a profile that no longer exists. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminProfileNotFoundPreview() {
    TrailsTheme {
        AdminProfileContent(state = AdminProfileUiState.NotFound, onBack = {})
    }
}
