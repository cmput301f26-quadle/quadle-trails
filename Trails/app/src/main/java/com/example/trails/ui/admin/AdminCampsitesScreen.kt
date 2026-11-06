// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.04.01, 03.01.01
// PURPOSE: Admin list of every campsite with search (Figma Scenario 8 · Campsites);
//   tapping one opens the admin Campsite page, where it can be removed.
// Outstanding issues: no Published/Drafts filter yet.
package com.example.trails.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.ui.theme.TrailsTheme

/** Connects [AdminCampsitesViewModel] to navigation and renders [AdminCampsitesContent]. */
@Composable
fun AdminCampsitesScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AdminCampsitesViewModel = viewModel { AdminCampsitesViewModel() }
) {
    val state by viewModel.state.collectAsState()
    AdminCampsitesContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onBack = { navController.popBackStack() },
        // Only the id is passed; the admin Campsite page loads the campsite itself.
        onOpenCampsite = { id -> navController.navigate(AdminRoutes.adminCampsite(id)) },
        modifier = modifier
    )
}

/**
 * Stateless admin Campsites UI, split out so UI tests can drive it directly.
 *
 * @param state what to show, or null while loading.
 * @param onQueryChange called with the new text when the search field changes.
 * @param onBack called when the back arrow is tapped.
 * @param onOpenCampsite called with a campsite's id when its card is tapped.
 */
@Composable
fun AdminCampsitesContent(
    state: AdminCampsitesUiState?,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenCampsite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrailsTopBar(
                title = stringResource(R.string.admin_campsites_title),
                onBack = onBack,
                backTestTag = AdminCampsitesTags.BACK,
                backContentDescription = stringResource(R.string.admin_campsites_back)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            if (state == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.testTag(AdminCampsitesTags.LOADING))
                }
            } else {
                CampsiteList(state, onQueryChange, onOpenCampsite)
            }
        }
    }
}

/** Search, count line and the list of campsite cards (or an empty / no-match message). */
@Composable
private fun CampsiteList(
    state: AdminCampsitesUiState,
    onQueryChange: (String) -> Unit,
    onOpenCampsite: (String) -> Unit
) {
    OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.admin_campsites_search_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AdminCampsitesTags.SEARCH)
    )
    Text(
        adminCampsiteCountLine(state.totalCount),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
    )
    when {
        state.totalCount == 0 ->
            Message(stringResource(R.string.admin_campsites_empty), AdminCampsitesTags.EMPTY)
        state.items.isEmpty() ->
            Message(stringResource(R.string.admin_campsites_no_match), AdminCampsitesTags.NO_MATCH)
        else -> LazyColumn(
            modifier = Modifier.testTag(AdminCampsitesTags.LIST),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(state.items, key = { it.campsite.id }) { item ->
                CampsiteCard(item, onClick = { onOpenCampsite(item.campsite.id) })
            }
        }
    }
}

/** Card: thumbnail, name, "location · Owner name" and chevron. */
@Composable
private fun CampsiteCard(item: AdminCampsiteItem, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AdminCampsitesTags.card(item.campsite.id))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(item.campsite.imageUrl)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(item.campsite.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    adminCampsiteSubtitle(item),
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

/** 64dp rounded promo photo, or a plain dark tile when the campsite has none. */
@Composable
private fun Thumbnail(url: String?) {
    val mod = Modifier
        .size(64.dp)
        .clip(RoundedCornerShape(8.dp))
    if (url != null) {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = mod)
    } else {
        Box(mod.background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

/** Centred message used for the empty and no-match states. */
@Composable
private fun Message(text: String, tag: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, modifier = Modifier.testTag(tag))
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object AdminCampsitesTags {
    const val BACK = "admin_campsites_back"
    const val SEARCH = "admin_campsites_search"
    const val LIST = "admin_campsites_list"
    const val EMPTY = "admin_campsites_empty"
    const val NO_MATCH = "admin_campsites_no_match"
    const val LOADING = "admin_campsites_loading"

    /** Tag for one campsite's card. */
    fun card(id: String) = "admin_campsites_card_$id"
}

/** Design-time preview with the sample campsites and users, for comparing against Scenario 8. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminCampsitesContentPreview() {
    TrailsTheme {
        AdminCampsitesContent(
            state = buildAdminCampsitesState(TestCampsites.all, TestUsers.all, ""),
            onQueryChange = {}, onBack = {}, onOpenCampsite = {}
        )
    }
}

/** Design-time preview of the screen before any campsite exists. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminCampsitesEmptyPreview() {
    TrailsTheme {
        AdminCampsitesContent(
            state = AdminCampsitesUiState(emptyList(), "", 0),
            onQueryChange = {}, onBack = {}, onOpenCampsite = {}
        )
    }
}
