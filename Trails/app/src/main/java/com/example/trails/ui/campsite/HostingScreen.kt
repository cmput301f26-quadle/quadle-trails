// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.03.01, 03.11.01, 02.07.01–03 (Reservations tab)
// PURPOSE: Owner's home screen listing the campsites they host (Figma F01 · My campsites), with
//   the mode pill in the header for switching modes (US 03.11.01, Aarav's ModePill), and a
//   Reservations tab showing My Reservations (Figma · Owner reservations).
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.ui.lottery.MyLotteriesScreen
import com.example.trails.ui.profile.ModePill
import com.example.trails.ui.profile.ModePillChip
import com.example.trails.ui.theme.TrailsTheme

/**
 * The sections of the Hosting home: Campsites and Lotteries (Figma F01), and Reservations
 * (US 02.07.01–03).
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property title the header title while this tab is showing.
 */
enum class HostingTab(@StringRes val label: Int, @StringRes val title: Int) {
    CAMPSITES(R.string.campsite_tab_campsites, R.string.campsite_hosting_title),
    LOTTERIES(R.string.campsite_tab_lotteries, R.string.campsite_hosting_title),
    RESERVATIONS(R.string.campsite_tab_reservations, R.string.reservations_title)
}

/**
 * Connects [HostingViewModel] to navigation and renders [HostingContent].
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
fun HostingScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: HostingViewModel = viewModel {
        HostingViewModel(AppContainer.campsiteRepository, AppContainer.lotteryRepository, CurrentUser.id())
    }
) {
    val state by viewModel.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(HostingTab.CAMPSITES) }
    HostingContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onAddCampsite = { navController.navigate(CampsiteRoutes.campsiteForm()) },
        onOpenCampsite = { id -> navController.navigate(CampsiteRoutes.ownerCampsite(id)) },
        modifier = modifier,
        selectedTab = tab,
        onTabChange = { tab = it },
        lotteriesContent = { MyLotteriesScreen(navController) }, // Skanda's screen, US 02.04.x
        reservationsContent = { OwnerReservationsScreen() }, // details screen comes with US 02.08.01
        modePill = { ModePill(navController) }
    )
}

/**
 * Stateless Hosting home UI, split out so UI tests can drive it directly.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param state what to show on the Campsites tab, or null while loading.
 * @param onQueryChange called with the new text when the search field changes.
 * @param onAddCampsite called when "Add a campsite" is tapped.
 * @param onOpenCampsite called with a campsite's id when its card is tapped.
 * @param selectedTab which tab is showing.
 * @param onTabChange called when the user taps a tab.
 * @param lotteriesContent what to show on the Lotteries tab.
 * @param reservationsContent what to show on the Reservations tab (US 02.07.01–03).
 * @param modePill the mode pill shown at the right of the header (US 03.11.01); empty by
 *   default so tests and previews don't need a ViewModel.
 */
@Composable
fun HostingContent(
    state: HostingUiState?,
    onQueryChange: (String) -> Unit,
    onAddCampsite: () -> Unit,
    onOpenCampsite: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: HostingTab = HostingTab.CAMPSITES,
    onTabChange: (HostingTab) -> Unit = {},
    lotteriesContent: @Composable () -> Unit = {},
    reservationsContent: @Composable () -> Unit = {},
    modePill: @Composable () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = { HostingHeader(selectedTab.title, modePill) },
        bottomBar = {
            if (selectedTab == HostingTab.CAMPSITES) {
                Button(
                    onClick = onAddCampsite,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(48.dp)
                        .testTag(HostingTags.ADD_BUTTON)
                ) { Text(stringResource(R.string.campsite_add)) }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            HostingTabs(selectedTab, onTabChange)
            Spacer(Modifier.height(16.dp))
            when (selectedTab) {
                HostingTab.LOTTERIES -> Box(Modifier.fillMaxSize().testTag(HostingTags.LOTTERIES)) {
                    lotteriesContent()
                }
                HostingTab.RESERVATIONS -> Box(Modifier.fillMaxSize().testTag(HostingTags.RESERVATIONS)) {
                    reservationsContent()
                }
                HostingTab.CAMPSITES -> CampsitesTab(state, onQueryChange, onOpenCampsite)
            }
        }
    }
}

/**
 * Avatar, title ("Hosting", or "My Reservations" on that tab) and the mode pill.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param title the title for the selected tab.
 * @param modePill the mode pill shown at the right of the header.
 */
@Composable
private fun HostingHeader(@StringRes title: Int, modePill: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar()
        Spacer(Modifier.width(12.dp))
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Box(Modifier.testTag(HostingTags.ROLE_PILL)) {
            modePill()
        }
    }
}

/**
 * Green circle with a simple person glyph, drawn so no icon library is needed.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun Avatar() {
    val glyph = MaterialTheme.colorScheme.onPrimary
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            drawCircle(glyph, radius = size.minDimension * 0.22f, center = Offset(size.width / 2, size.height * 0.32f))
            drawArc(
                glyph, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(size.width * 0.15f, size.height * 0.62f),
                size = Size(size.width * 0.7f, size.height * 0.7f)
            )
        }
    }
}

/**
 * Segmented pill toggle between Campsites, Lotteries and Reservations.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun HostingTabs(selected: HostingTab, onSelect: (HostingTab) -> Unit) {
    val outer = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(outer)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, outer)
            .padding(5.dp)
    ) {
        HostingTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .testTag(HostingTags.tab(tab)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(tab.label),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Search, counts and the list of campsite cards.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun CampsitesTab(
    state: HostingUiState?,
    onQueryChange: (String) -> Unit,
    onOpenCampsite: (String) -> Unit
) {
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Column {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.campsite_search_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HostingTags.SEARCH)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                pluralStringResource(R.plurals.campsite_count, state.totalCount, state.totalCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.campsite_in_lottery_count, state.inLotteryCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when {
            state.totalCount == 0 ->
                Message(stringResource(R.string.campsite_none_registered), HostingTags.EMPTY_MESSAGE)
            state.items.isEmpty() ->
                Message(stringResource(R.string.campsite_no_match, state.query.trim()), HostingTags.NO_MATCH)
            else -> LazyColumn(
                modifier = Modifier.testTag(HostingTags.LIST),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(state.items, key = { it.campsite.id }) { item ->
                    CampsiteCard(item, onClick = { onOpenCampsite(item.campsite.id) })
                }
            }
        }
    }
}

/**
 * Card: thumbnail, name, "location · Up to N guests", lottery chip and chevron.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun CampsiteCard(item: HostingItem, onClick: () -> Unit) {
    val c = item.campsite
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(c.imageUrl)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.campsite_location_guests, c.locationName, c.maxOccupancy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LotteryChip(item.lotteryName)
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 64dp rounded promo photo, or a plain tile when the campsite has none.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun Thumbnail(url: String?) {
    val shape = RoundedCornerShape(8.dp)
    val mod = Modifier
        .size(64.dp)
        .clip(shape)
    if (url != null) {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = mod)
    } else {
        Box(mod.background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

/**
 * Green "In lottery · <name>" when in a lottery, grey "Not in a lottery" otherwise.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun LotteryChip(lotteryName: String?) {
    val inLottery = lotteryName != null
    val colour = if (inLottery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        if (lotteryName != null) stringResource(R.string.campsite_in_lottery, lotteryName)
        else stringResource(R.string.campsite_not_in_lottery),
        style = MaterialTheme.typography.labelSmall,
        color = colour,
        modifier = Modifier
            .border(1.dp, colour, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/**
 * Centred message used for the empty and no-match states.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun Message(text: String, tag: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, modifier = Modifier.testTag(tag))
    }
}

/**
 * Test tags used by UI tests to find elements on this screen.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
object HostingTags {
    const val ADD_BUTTON = "hosting_add_button"
    const val SEARCH = "hosting_search"
    const val LIST = "hosting_list"
    const val EMPTY_MESSAGE = "hosting_empty_message"
    const val NO_MATCH = "hosting_no_match"
    const val ROLE_PILL = "hosting_role_pill"
    const val LOTTERIES = "hosting_lotteries"
    const val RESERVATIONS = "hosting_reservations"

    /** Tag for a tab segment. */
    fun tab(tab: HostingTab) = "hosting_tab_${tab.name.lowercase()}"
}

/**
 * Design-time preview with the Figma sample data, for comparing against F01 · My campsites.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun HostingContentPreview() {
    val alpine1 = Campsite(id = "a1", name = "Alpine Lake · Site 01", locationName = "Banff, AB", maxOccupancy = 6)
    val alpine2 = alpine1.copy(id = "a2", name = "Alpine Lake · Site 02", maxOccupancy = 8)
    val twoJack = alpine1.copy(id = "tj", name = "Two Jack Lakeside", locationName = "Banff National Park, AB", maxOccupancy = 4)
    TrailsTheme {
        HostingContent(
            state = HostingUiState(
                items = listOf(
                    HostingItem(alpine1, "May long weekend"),
                    HostingItem(alpine2, "May long weekend"),
                    HostingItem(twoJack, null)
                ),
                query = "", totalCount = 3, inLotteryCount = 2
            ),
            onQueryChange = {}, onAddCampsite = {}, onOpenCampsite = {},
            modePill = { ModePillChip(Role.OWNER, onClick = {}) }
        )
    }
}
