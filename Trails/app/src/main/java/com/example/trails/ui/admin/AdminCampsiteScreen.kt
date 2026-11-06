// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01
// PURPOSE: Admin's view of one campsite, opened from the Campsites list, with a
//   Remove campsite button and confirmation dialog (Figma Scenario 8 · Remove campsite?).
// Outstanding issues:
//   - Guests with reservations aren't notified, since reservations and
//     notifications don't exist yet (Part 4).
//   - The campsite's ratings stay in storage after removal.
package com.example.trails.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.ui.discovery.DiscoveryRoutes
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects [AdminCampsiteViewModel] to navigation and renders [AdminCampsiteContent].
 * Only the campsite's id arrives through navigation; the ViewModel loads it itself.
 *
 * @param navController used to go back to the Campsites list and open the public page.
 * @param campsiteId id of the campsite to show.
 */
@Composable
fun AdminCampsiteScreen(
    navController: NavController,
    campsiteId: String,
    modifier: Modifier = Modifier,
    viewModel: AdminCampsiteViewModel = viewModel(key = campsiteId) { AdminCampsiteViewModel(campsiteId) }
) {
    val state by viewModel.state.collectAsState()
    val removeState by viewModel.removeState.collectAsState()
    LaunchedEffect(removeState.removed) {
        if (removeState.removed) navController.popBackStack()
    }
    AdminCampsiteContent(
        state = state,
        removeState = removeState,
        onBack = { navController.popBackStack() },
        onOpenPublicPage = { id -> navController.navigate(DiscoveryRoutes.campsiteDetails(id)) },
        onConfirmRemove = viewModel::onConfirmRemove,
        modifier = modifier
    )
}

/**
 * Stateless admin Campsite UI, split out so UI tests can drive it directly. Only whether the
 * confirmation dialog is open is kept here; it also stays open while a removal is running.
 *
 * @param state what to show: a spinner, a not-found message, or the campsite.
 * @param removeState progress of a confirmed removal.
 * @param onBack called when the back arrow is tapped.
 * @param onOpenPublicPage called with the campsite's id when "Open public page" is tapped.
 * @param onConfirmRemove called when the admin taps Remove in the dialog.
 */
@Composable
fun AdminCampsiteContent(
    state: AdminCampsiteUiState,
    removeState: AdminCampsiteRemoveState,
    onBack: () -> Unit,
    onOpenPublicPage: (String) -> Unit,
    onConfirmRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = { AdminCampsiteHeader(onBack) }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (state) {
                AdminCampsiteUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.testTag(AdminCampsiteTags.LOADING))
                }
                AdminCampsiteUiState.NotFound -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.admin_campsite_not_found),
                        modifier = Modifier.testTag(AdminCampsiteTags.NOT_FOUND)
                    )
                }
                is AdminCampsiteUiState.Loaded -> {
                    CampsiteDetails(
                        state = state,
                        failed = removeState.failed,
                        onOpenPublicPage = { onOpenPublicPage(state.campsite.id) },
                        onRemove = { showDialog = true }
                    )
                    if (showDialog || removeState.removing) {
                        RemoveCampsiteDialog(
                            message = removeCampsiteMessage(
                                state.campsite.name, state.lotteryCount, state.entryCount
                            ),
                            removing = removeState.removing,
                            onCancel = { showDialog = false },
                            onRemove = {
                                showDialog = false
                                onConfirmRemove()
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Green back arrow and the "Campsite" title, matching the Campsites list header. */
@Composable
private fun AdminCampsiteHeader(onBack: () -> Unit) {
    val backLabel = stringResource(R.string.admin_campsites_back)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .semantics { contentDescription = backLabel }
                .testTag(AdminCampsiteTags.BACK),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.admin_campsite_title), style = MaterialTheme.typography.titleLarge)
    }
}

/** Photo, name, location, owner, info card, public page link, and the Remove campsite button. */
@Composable
private fun CampsiteDetails(
    state: AdminCampsiteUiState.Loaded,
    failed: Boolean,
    onOpenPublicPage: () -> Unit,
    onRemove: () -> Unit
) {
    val campsite = state.campsite
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Photo(campsite.imageUrl)
            Spacer(Modifier.height(16.dp))
            Text(
                campsite.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag(AdminCampsiteTags.NAME)
            )
            if (campsite.locationName.isNotBlank()) {
                Text(
                    campsite.locationName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.admin_campsite_owner, state.ownerName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            InfoCard(state)
            TextButton(
                onClick = onOpenPublicPage,
                modifier = Modifier.testTag(AdminCampsiteTags.OPEN_PUBLIC)
            ) {
                Text(stringResource(R.string.admin_campsite_open_public))
            }
        }
        if (failed) {
            Text(
                stringResource(R.string.admin_campsite_remove_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag(AdminCampsiteTags.ERROR)
            )
        }
        // Muted red: a tinted fill with red text, so it reads as destructive without shouting.
        Button(
            onClick = onRemove,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ember.copy(alpha = 0.15f), contentColor = Ember),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag(AdminCampsiteTags.REMOVE_BUTTON)
        ) {
            Text(stringResource(R.string.admin_campsite_remove))
        }
    }
}

/** Full-width rounded promo photo, or a plain dark tile when the campsite has none. */
@Composable
private fun Photo(url: String?) {
    val mod = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .clip(RoundedCornerShape(12.dp))
    if (url != null) {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = mod)
    } else {
        Box(mod.background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

/** Card with the Lotteries, Lottery entries and Rating rows. */
@Composable
private fun InfoCard(state: AdminCampsiteUiState.Loaded) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        val divider = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        InfoRow(stringResource(R.string.admin_campsite_lotteries), state.lotteryCount.toString(), AdminCampsiteTags.LOTTERIES)
        HorizontalDivider(color = divider)
        InfoRow(stringResource(R.string.admin_campsite_entries), state.entryCount.toString(), AdminCampsiteTags.ENTRIES)
        HorizontalDivider(color = divider)
        InfoRow(stringResource(R.string.admin_campsite_rating), ratingLabel(state.campsite), AdminCampsiteTags.RATING)
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

/**
 * "Remove campsite?" confirmation with a trash icon, Cancel and a Remove button in the theme's green.
 *
 * @param message the body text from [removeCampsiteMessage].
 * @param removing true while the removal runs; both buttons are disabled and it can't be dismissed.
 * @param onCancel called when Cancel is tapped or the dialog is dismissed.
 * @param onRemove called when Remove is tapped.
 */
@Composable
private fun RemoveCampsiteDialog(
    message: String,
    removing: Boolean,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!removing) onCancel() },
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Ember) },
        title = { Text(stringResource(R.string.admin_campsite_remove_title)) },
        text = { Text(message, modifier = Modifier.testTag(AdminCampsiteTags.DIALOG_MESSAGE)) },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                enabled = !removing,
                modifier = Modifier.testTag(AdminCampsiteTags.DIALOG_CANCEL)
            ) {
                Text(stringResource(R.string.admin_campsite_remove_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = onRemove,
                enabled = !removing,
                modifier = Modifier.testTag(AdminCampsiteTags.DIALOG_REMOVE)
            ) {
                Text(stringResource(R.string.admin_campsite_remove_confirm))
            }
        },
        modifier = Modifier.testTag(AdminCampsiteTags.DIALOG)
    )
}

/** Test tags used by UI tests to find elements on this screen. */
object AdminCampsiteTags {
    const val BACK = "admin_campsite_back"
    const val LOADING = "admin_campsite_loading"
    const val NOT_FOUND = "admin_campsite_not_found"
    const val NAME = "admin_campsite_name"
    const val LOTTERIES = "admin_campsite_lotteries"
    const val ENTRIES = "admin_campsite_entries"
    const val RATING = "admin_campsite_rating"
    const val OPEN_PUBLIC = "admin_campsite_open_public"
    const val REMOVE_BUTTON = "admin_campsite_remove"
    const val ERROR = "admin_campsite_error"
    const val DIALOG = "admin_campsite_dialog"
    const val DIALOG_MESSAGE = "admin_campsite_dialog_message"
    const val DIALOG_CANCEL = "admin_campsite_dialog_cancel"
    const val DIALOG_REMOVE = "admin_campsite_dialog_remove"
}

/** Design-time preview of the sample Banff campsite, for comparing against Scenario 8. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminCampsitePreview() {
    TrailsTheme {
        AdminCampsiteContent(
            state = buildAdminCampsiteState(
                TestCampsites.all.first { it.id == TestCampsites.BANFF_ID },
                TestUsers.all,
                CampsiteImpact("Tunnel Mountain Site 14", 1, 2)
            ),
            removeState = AdminCampsiteRemoveState(),
            onBack = {}, onOpenPublicPage = {}, onConfirmRemove = {}
        )
    }
}

/** Design-time preview of a removal that failed. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminCampsiteFailedPreview() {
    TrailsTheme {
        AdminCampsiteContent(
            state = buildAdminCampsiteState(
                TestCampsites.all.first { it.id == TestCampsites.HINTON_ID },
                TestUsers.all,
                null
            ),
            removeState = AdminCampsiteRemoveState(failed = true),
            onBack = {}, onOpenPublicPage = {}, onConfirmRemove = {}
        )
    }
}
