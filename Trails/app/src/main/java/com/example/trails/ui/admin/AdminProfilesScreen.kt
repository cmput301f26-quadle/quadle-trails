// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.05.01
// PURPOSE: Admin list of every user profile with search and role chips (Figma Scenario 8 · Profiles).
// Outstanding issues: none; profiles are removed from the Profile page (US 03.02.01).
package com.example.trails.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import com.example.trails.data.model.User
import com.example.trails.ui.theme.TrailsTheme

/** Connects [AdminProfilesViewModel] to navigation and renders [AdminProfilesContent]. */
@Composable
fun AdminProfilesScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AdminProfilesViewModel = viewModel { AdminProfilesViewModel() }
) {
    val state by viewModel.state.collectAsState()
    AdminProfilesContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onBack = { navController.popBackStack() },
        // Only the id is passed; the profile page loads the user itself.
        onOpenProfile = { id -> navController.navigate(AdminRoutes.adminProfile(id)) },
        modifier = modifier
    )
}

/**
 * Stateless admin Profiles UI, split out so UI tests can drive it directly.
 *
 * @param state what to show, or null while loading.
 * @param onQueryChange called with the new text when the search field changes.
 * @param onFilterChange called with the chip's filter when a chip is tapped.
 * @param onBack called when the back arrow is tapped.
 * @param onOpenProfile called with a user's device id when their row is tapped.
 */
@Composable
fun AdminProfilesContent(
    state: AdminProfilesUiState?,
    onQueryChange: (String) -> Unit,
    onFilterChange: (ProfileFilter) -> Unit,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrailsTopBar(
                title = stringResource(R.string.admin_profiles_title),
                onBack = onBack,
                backTestTag = AdminProfilesTags.BACK,
                backContentDescription = stringResource(R.string.admin_profiles_back)
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
                    CircularProgressIndicator(Modifier.testTag(AdminProfilesTags.LOADING))
                }
            } else {
                ProfileList(state, onQueryChange, onFilterChange, onOpenProfile)
            }
        }
    }
}

/** Search, filter chips, count line and the list of profile rows (or an empty / no-match message). */
@Composable
private fun ProfileList(
    state: AdminProfilesUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (ProfileFilter) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.admin_profiles_search_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AdminProfilesTags.SEARCH)
    )
    FilterChips(selected = state.filter, onFilterChange = onFilterChange)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            adminProfileCountLine(state.totalCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.admin_profiles_sort_az),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    when {
        state.totalCount == 0 ->
            Message(stringResource(R.string.admin_profiles_empty), AdminProfilesTags.EMPTY)
        state.items.isEmpty() ->
            Message(stringResource(R.string.admin_profiles_no_match), AdminProfilesTags.NO_MATCH)
        else -> LazyColumn(
            modifier = Modifier.testTag(AdminProfilesTags.LIST),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(state.items, key = { it.user.deviceId }) { item ->
                ProfileRow(item, onClick = { onOpenProfile(item.user.deviceId) })
            }
        }
    }
}

/** All / Owners / Entrants / Admins; the selected chip is filled green with a check. */
@Composable
private fun FilterChips(selected: ProfileFilter, onFilterChange: (ProfileFilter) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onFilterChange(filter) },
                label = { Text(stringResource(filterLabel(filter))) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag(AdminProfilesTags.chip(filter))
            )
        }
    }
}

/** String resource for a chip's label. */
private fun filterLabel(filter: ProfileFilter): Int = when (filter) {
    ProfileFilter.ALL -> R.string.admin_profiles_filter_all
    ProfileFilter.OWNERS -> R.string.admin_profiles_filter_owners
    ProfileFilter.ENTRANTS -> R.string.admin_profiles_filter_entrants
    ProfileFilter.ADMINS -> R.string.admin_profiles_filter_admins
}

/** Row: avatar, name, grey role subtitle and chevron; the whole row is tappable. */
@Composable
private fun ProfileRow(item: AdminProfileItem, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AdminProfilesTags.row(item.user.deviceId))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(item.user)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(adminProfileDisplayName(item.user), style = MaterialTheme.typography.titleMedium)
                Text(
                    adminProfileSubtitle(item),
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

/** 40dp round profile photo, or a dark circle with the user's initials when there is none. */
@Composable
private fun Avatar(user: User) {
    val mod = Modifier
        .size(40.dp)
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
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
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
object AdminProfilesTags {
    const val BACK = "admin_profiles_back"
    const val SEARCH = "admin_profiles_search"
    const val LIST = "admin_profiles_list"
    const val EMPTY = "admin_profiles_empty"
    const val NO_MATCH = "admin_profiles_no_match"
    const val LOADING = "admin_profiles_loading"

    /** Tag for one filter chip. */
    fun chip(filter: ProfileFilter) = "admin_profiles_chip_${filter.name}"

    /** Tag for one user's row. */
    fun row(id: String) = "admin_profiles_row_$id"
}

/** Design-time preview with the sample users and campsites, for comparing against Scenario 8. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminProfilesContentPreview() {
    TrailsTheme {
        AdminProfilesContent(
            state = buildAdminProfilesState(TestUsers.all, TestCampsites.all, "", ProfileFilter.ALL),
            onQueryChange = {}, onFilterChange = {}, onBack = {}, onOpenProfile = {}
        )
    }
}

/** Design-time preview with the Owners chip selected. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminProfilesOwnersPreview() {
    TrailsTheme {
        AdminProfilesContent(
            state = buildAdminProfilesState(TestUsers.all, TestCampsites.all, "", ProfileFilter.OWNERS),
            onQueryChange = {}, onFilterChange = {}, onBack = {}, onOpenProfile = {}
        )
    }
}
