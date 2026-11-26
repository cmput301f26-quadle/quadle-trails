// UI Controller composable displaying winner multi-campsite selection with OR dividers (MVVM pattern).
// implements US 01.04.02, US 02.04.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.ui.waitlist

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.data.AppContainer
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.Forest
import com.example.trails.ui.theme.Mist
import com.example.trails.ui.theme.Moss
import com.example.trails.ui.theme.Night
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme

/**
 * Screen destination allowing a lottery winner to choose which campsite to reserve.
 *
 * @param navController controller for navigating across destinations.
 * @param lotteryId identifier of the winning lottery.
 * @param modifier optional [Modifier] for screen styling.
 * @param viewModel scoped [SelectCampsiteViewModel] instance.
 */
@Composable
fun SelectCampsiteScreen(
    navController: NavController,
    lotteryId: String,
    modifier: Modifier = Modifier,
    viewModel: SelectCampsiteViewModel = viewModel(key = "select-campsite-$lotteryId") {
        SelectCampsiteViewModel(
            lotteryId = lotteryId,
            lotteryRepository = AppContainer.lotteryRepository,
            campsiteRepository = AppContainer.campsiteRepository
        )
    }
) {
    val state by viewModel.uiState.collectAsState()

    SelectCampsiteContent(
        state = state,
        onBack = { navController.popBackStack() },
        onSelectCampsite = { campsiteId ->
            navController.navigate(WaitlistRoutes.reserveDates(lotteryId, campsiteId))
        },
        onRetry = { viewModel.loadCampsites() },
        modifier = modifier
    )
}

/**
 * Stateless layout displaying campsite options with OR dividers.
 *
 * @param state the current UI state to render.
 * @param onBack callback triggered when back button is pressed.
 * @param onSelectCampsite callback triggered when a campsite's reserve button is clicked.
 * @param onRetry callback triggered to retry data loading on error.
 * @param modifier optional [Modifier] for layout adjustments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCampsiteContent(
    state: SelectCampsiteUiState,
    onBack: () -> Unit,
    onSelectCampsite: (String) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TrailsTopBar(
                title = "Select Campsite",
                onBack = onBack,
                backTestTag = "select_campsite_back_button"
            )
        },
        containerColor = Night,
        modifier = modifier.testTag("select_campsite_screen")
    ) { innerPadding ->
        when (state) {
            is SelectCampsiteUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TrailGreen,
                        modifier = Modifier.testTag("select_campsite_loading")
                    )
                }
            }

            is SelectCampsiteUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = TrailGreen, contentColor = Night)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is SelectCampsiteUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (state.campsites.size > 1) {
                            "You won the lottery for multiple locations. Choose one to claim your dates:"
                        } else {
                            "You won entry for this campsite! Claim your dates below:"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Mist,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("select_campsite_subtitle")
                    )

                    state.campsites.forEachIndexed { index, campsite ->
                        if (index > 0) {
                            // green OR divider between campsites
                            AlternativeDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .testTag("select_campsite_or_divider")
                            )
                        }

                        CampsiteSelectionCard(
                            campsite = campsite,
                            onReserveClick = { onSelectCampsite(campsite.id) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Reusable green OR divider separating mutually exclusive campsite options.
 *
 * @param modifier optional [Modifier] for layout adjustments.
 */
@Composable
fun AlternativeDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = TrailGreen.copy(alpha = 0.35f),
            thickness = 1.dp
        )
        Text(
            text = "OR",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TrailGreen,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = TrailGreen.copy(alpha = 0.35f),
            thickness = 1.dp
        )
    }
}

/**
 * Card displaying campsite details and a reserve dates button.
 *
 * @param campsite campsite details.
 * @param onReserveClick callback when reserve button is pressed.
 * @param modifier optional [Modifier] for layout adjustments.
 */
@Composable
fun CampsiteSelectionCard(
    campsite: Campsite,
    onReserveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NightSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Moss, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("select_campsite_card_${campsite.id}")
    ) {
        Text(
            text = campsite.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Mist,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = campsite.locationName,
                style = MaterialTheme.typography.bodySmall,
                color = Mist,
                modifier = Modifier.weight(1f)
            )
            if (campsite.pricePerNight > 0) {
                Text(
                    text = "$${campsite.pricePerNight.toInt()} / night",
                    style = MaterialTheme.typography.labelMedium,
                    color = TrailGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Campsite photo thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Forest)
        ) {
            val image = campsite.imageUrl
            if (!image.isNullOrBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = campsite.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onReserveClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reserve_campsite_button_${campsite.id}"),
            colors = ButtonDefaults.buttonColors(
                containerColor = TrailGreen,
                contentColor = Night
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Reserve dates",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun SelectCampsitePreview() {
    TrailsTheme {
        SelectCampsiteContent(
            state = SelectCampsiteUiState.Success(
                lottery = Lottery(id = "lottery-1", name = "Canadian Rockies Draw"),
                campsites = listOf(
                    Campsite(
                        id = "site-1",
                        name = "Grande Cache Ridge",
                        locationName = "Hinton, AB",
                        pricePerNight = 45.0
                    ),
                    Campsite(
                        id = "site-2",
                        name = "Two Jack Lakeside Campground",
                        locationName = "Banff, AB",
                        pricePerNight = 35.0
                    )
                )
            ),
            onBack = {},
            onSelectCampsite = {}
        )
    }
}
