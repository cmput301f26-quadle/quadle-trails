// Screen composable rendering CampMatch amenity tag search and ranked campsite recommendations (UI Controller).
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.trails.data.model.Campsite
import com.example.trails.matcher.model.CamperTagOption
import com.example.trails.matcher.model.CamperTagRegistry
import com.example.trails.matcher.model.MatchResult
import com.example.trails.matcher.model.MatchScoreBreakdown
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.Forest
import com.example.trails.ui.theme.Mist
import com.example.trails.ui.theme.Moss
import com.example.trails.ui.theme.Night
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme

/**
 * Stateful entry point for the CampMatch discovery screen.
 *
 * @param onBack callback on back navigation.
 * @param onCampsiteClick callback when an entrant taps a campsite to inspect details.
 * @param onJoinLotteryClick callback when an entrant taps to enter an active lottery.
 * @param modifier optional layout modifier.
 * @param viewModel controller managing search, filtering, and scoring.
 */
@Composable
fun CampMatchScreen(
    onBack: () -> Unit,
    onCampsiteClick: (campsiteId: String) -> Unit,
    onJoinLotteryClick: (lotteryId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CampMatchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    CampMatchContent(
        state = state,
        onBack = onBack,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onClearSearch = viewModel::clearSearchQuery,
        onTagToggled = viewModel::onTagToggled,
        onClearAllTags = viewModel::onClearAllTags,
        onMaxPriceChanged = viewModel::onMaxPriceChanged,
        onTogglePrioritizeOdds = viewModel::onTogglePrioritizeOdds,
        onCampsiteClick = onCampsiteClick,
        onJoinLotteryClick = onJoinLotteryClick,
        modifier = modifier
    )
}

/**
 * Stateless presentation composable rendering the CampMatch screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampMatchContent(
    state: CampMatchUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onTagToggled: (String) -> Unit,
    onClearAllTags: () -> Unit,
    onMaxPriceChanged: (Double?) -> Unit,
    onTogglePrioritizeOdds: () -> Unit,
    onCampsiteClick: (String) -> Unit,
    onJoinLotteryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TrailsTopBar(
                title = "CampMatch",
                onBack = onBack,
                backTestTag = "camp_match_back_button"
            )
        },
        containerColor = Night,
        modifier = modifier.testTag("camp_match_screen")
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TrailGreen)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Find Your Ideal Campsite",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Select your preferred amenities and let our matching engine calculate your best campsite matches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Search Bar for amenities
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search amenities (e.g. Fire pit, Showers, Lake)...", color = Mist) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Mist)
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Mist)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camp_match_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TrailGreen,
                        unfocusedBorderColor = Moss,
                        focusedContainerColor = NightSurface,
                        unfocusedContainerColor = NightSurface
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Selected tags active row
            if (state.selectedTags.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NightSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, Moss, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .testTag("camp_match_selected_container")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Selected Preferences (${state.selectedTags.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TrailGreen
                            )
                            Text(
                                text = "Clear all",
                                style = MaterialTheme.typography.labelSmall,
                                color = Mist,
                                modifier = Modifier
                                    .clickable { onClearAllTags() }
                                    .testTag("camp_match_clear_all_tags")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.selectedTags.forEach { tag ->
                                SelectedTagChip(
                                    tag = tag,
                                    onRemove = { onTagToggled(tag) }
                                )
                            }
                        }
                    }
                }
            }

            // Available Tags Flow / Grid
            item {
                Column {
                    Text(
                        text = if (state.searchQuery.isBlank()) "Browse Amenities" else "Matching Amenities",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("camp_match_available_tags"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.availableTags.forEach { option ->
                            val isSelected = option.tag in state.selectedTags
                            AmenityToggleChip(
                                option = option,
                                isSelected = isSelected,
                                onToggle = { onTagToggled(option.tag) }
                            )
                        }
                    }
                }
            }

            // Quick Budget & Odds Filter Toggles
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        label = "Any Budget",
                        isSelected = state.maxPricePerNight == null,
                        onClick = { onMaxPriceChanged(null) }
                    )
                    FilterPill(
                        label = "Under $50",
                        isSelected = state.maxPricePerNight == 50.0,
                        onClick = { onMaxPriceChanged(if (state.maxPricePerNight == 50.0) null else 50.0) }
                    )
                    FilterPill(
                        label = "Under $75",
                        isSelected = state.maxPricePerNight == 75.0,
                        onClick = { onMaxPriceChanged(if (state.maxPricePerNight == 75.0) null else 75.0) }
                    )
                    FilterPill(
                        label = "Prioritize High Odds",
                        isSelected = state.prioritizeHighWinOdds,
                        onClick = onTogglePrioritizeOdds
                    )
                }
            }

            // Results Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ranked Recommendations (${state.rankedMatches.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (state.rankedMatches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No campsites found matching these filters. Try adjusting your preferences.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Mist
                        )
                    }
                }
            } else {
                items(state.rankedMatches, key = { it.campsite.id }) { item ->
                    CampMatchCard(
                        campsite = item.campsite,
                        matchResult = item.matchResult,
                        onCampsiteClick = { onCampsiteClick(item.campsite.id) },
                        onJoinLotteryClick = {
                            item.campsite.activeLotteryId?.let(onJoinLotteryClick)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Amenity selection chip with relevant symbol.
 */
@Composable
private fun AmenityToggleChip(
    option: CamperTagOption,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val containerColor = if (isSelected) TrailGreen else NightSurface
    val contentColor = if (isSelected) Night else Color.White
    val borderColor = if (isSelected) TrailGreen else Moss

    Row(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("tag_chip_${option.tag.replace(" ", "_")}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Removable chip shown in the active selected tags row.
 */
@Composable
private fun SelectedTagChip(
    tag: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(Forest, RoundedCornerShape(12.dp))
            .border(1.dp, TrailGreen, RoundedCornerShape(12.dp))
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove $tag",
            tint = Mist,
            modifier = Modifier
                .size(16.dp)
                .clickable { onRemove() }
        )
    }
}

/**
 * Toggle pill for quick budget and lottery odds filtering.
 */
@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) TrailGreen.copy(alpha = 0.20f) else NightSurface
    val contentColor = if (isSelected) TrailGreen else Mist
    val borderColor = if (isSelected) TrailGreen else Moss

    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor
        )
    }
}

/**
 * Ranked recommendation card displaying campsite metadata, match badge, and highlights.
 */
@Composable
fun CampMatchCard(
    campsite: Campsite,
    matchResult: MatchResult,
    onCampsiteClick: () -> Unit,
    onJoinLotteryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NightSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Moss, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("camp_match_card_${campsite.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val imageUrl = campsite.imageUrl
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Forest)
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = campsite.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = campsite.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = campsite.locationName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist
                    )
                    Text(
                        text = "$${campsite.pricePerNight.toInt()}/night · Sleeps up to ${campsite.maxOccupancy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TrailGreen,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            MatchScoreBadge(score = matchResult.matchPercentage)
        }

        // Match highlights
        if (matchResult.matchHighlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Forest.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                matchResult.matchHighlights.take(3).forEach { highlight ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = TrailGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = highlight,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCampsiteClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Moss),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("match_view_campsite_button")
            ) {
                Text("View Details", style = MaterialTheme.typography.labelMedium)
            }

            if (!campsite.activeLotteryId.isNullOrBlank()) {
                Button(
                    onClick = onJoinLotteryClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrailGreen,
                        contentColor = Night
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("match_join_lottery_button")
                ) {
                    Text("Join Lottery", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(name = "CampMatch Full Screen", showBackground = true, backgroundColor = 0xFF141915)
@Composable
private fun CampMatchScreenPreview() {
    val sampleSite1 = Campsite(
        id = "site-preview-1",
        name = "Two Jack Lakeside",
        locationName = "Banff, AB",
        pricePerNight = 45.0,
        maxOccupancy = 6,
        tags = listOf("Fire pit", "Showers", "Lakeside"),
        activeLotteryId = "lottery-preview-1"
    )
    val sampleSite2 = Campsite(
        id = "site-preview-2",
        name = "Tunnel Mountain Village",
        locationName = "Banff, AB",
        pricePerNight = 40.0,
        maxOccupancy = 4,
        tags = listOf("Fire pit", "Power hookup")
    )
    val result1 = MatchResult(
        campsiteId = sampleSite1.id,
        matchPercentage = 94,
        matchHighlights = listOf(
            "Matches all 2 desired amenities",
            "High lottery win odds (85%)",
            "Within your budget ($45/night)"
        ),
        breakdown = MatchScoreBreakdown(1.0, 0.85, 1.0, 0.9)
    )
    val result2 = MatchResult(
        campsiteId = sampleSite2.id,
        matchPercentage = 76,
        matchHighlights = listOf(
            "Matches 1 of 2 desired amenities",
            "Within your budget ($40/night)"
        ),
        breakdown = MatchScoreBreakdown(0.5, 0.75, 1.0, 0.8)
    )

    val previewState = CampMatchUiState(
        isLoading = false,
        searchQuery = "",
        selectedTags = setOf("Fire pit", "Lakeside"),
        maxPricePerNight = 50.0,
        prioritizeHighWinOdds = true,
        availableTags = CamperTagRegistry.options,
        rankedMatches = listOf(
            CampMatchItem(sampleSite1, result1),
            CampMatchItem(sampleSite2, result2)
        )
    )

    TrailsTheme {
        CampMatchContent(
            state = previewState,
            onBack = {},
            onSearchQueryChange = {},
            onClearSearch = {},
            onTagToggled = {},
            onClearAllTags = {},
            onMaxPriceChanged = {},
            onTogglePrioritizeOdds = {},
            onCampsiteClick = {},
            onJoinLotteryClick = {}
        )
    }
}

@Preview(name = "CampMatch Card Component", showBackground = true, backgroundColor = 0xFF141915)
@Composable
private fun CampMatchCardPreview() {
    val sampleSite = Campsite(
        id = "site-card-preview",
        name = "Two Jack Lakeside",
        locationName = "Banff, AB",
        pricePerNight = 45.0,
        maxOccupancy = 6,
        activeLotteryId = "lottery-card-preview"
    )
    val sampleResult = MatchResult(
        campsiteId = sampleSite.id,
        matchPercentage = 92,
        matchHighlights = listOf(
            "Matches all 2 desired amenities",
            "High lottery win odds (85%)"
        ),
        breakdown = MatchScoreBreakdown(1.0, 0.85, 1.0, 0.9)
    )

    TrailsTheme {
        CampMatchCard(
            campsite = sampleSite,
            matchResult = sampleResult,
            onCampsiteClick = {},
            onJoinLotteryClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
