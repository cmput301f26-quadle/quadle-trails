// main waitlist screen matching story 01.01.06
// owner: chitkarsh (chitkarshrathi) - waitlist
package com.example.trails.ui.waitlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.data.AppContainer
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.Forest
import com.example.trails.ui.theme.Mist
import com.example.trails.ui.theme.Moss
import com.example.trails.ui.theme.Night
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme
import com.google.firebase.Timestamp
import java.util.Date

// entry point connecting navigation and view model to content
@Composable
fun WaitlistScreen(
    navController: NavController,
    campsiteId: String,
    modifier: Modifier = Modifier,
    lotteryId: String? = null,
    viewModel: WaitlistViewModel = viewModel(key = "$campsiteId-${lotteryId.orEmpty()}") {
        WaitlistViewModel(
            lotteryId = lotteryId ?: "",
            campsiteId = campsiteId,
            waitlistRepository = AppContainer.waitlistRepository,
            lotteryRepository = AppContainer.lotteryRepository,
            campsiteRepository = AppContainer.campsiteRepository,
            userRepository = AppContainer.userRepository
        )
    }
) {
    val state by viewModel.uiState.collectAsState()

    WaitlistContent(
        state = state,
        onRulesCheckedChange = { viewModel.onRulesAgreementChanged(it) },
        onJoinClick = { viewModel.joinWaitlist() },
        onLeaveClick = { viewModel.leaveWaitlist() },
        onBack = { navController.popBackStack() },
        onClearError = { viewModel.clearError() },
        onViewEntrants = { targetLotteryId ->
            navController.navigate(WaitlistRoutes.waitlistEntrants(targetLotteryId))
        },
        modifier = modifier
    )
}

// pure layout composable testable with static states
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitlistContent(
    state: WaitlistUiState,
    onRulesCheckedChange: (Boolean) -> Unit,
    onJoinClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    onViewEntrants: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // referenced android dev docs for scaffold and topappbar
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Join Lottery",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("waitlist_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Night)
            )
        },
        containerColor = Night,
        modifier = modifier
            .fillMaxSize()
            .testTag("waitlist_screen")
    ) { innerPadding ->
        if (state is WaitlistUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = TrailGreen,
                    modifier = Modifier.testTag("waitlist_loading")
                )
            }
        } else {
            val campsite: Campsite? = when (state) {
                is WaitlistUiState.NotWaitlisted -> state.campsite
                is WaitlistUiState.Waitlisted -> state.campsite
                else -> null
            }
            val lottery: Lottery? = when (state) {
                is WaitlistUiState.NotWaitlisted -> state.lottery
                is WaitlistUiState.Waitlisted -> state.lottery
                else -> null
            }
            val isRulesAccepted = (state as? WaitlistUiState.NotWaitlisted)?.isRulesAccepted ?: false

            // based on android developer guide for scrollable columns
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // error alert banner
                if (state is WaitlistUiState.Error) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NightSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, Ember, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .testTag("waitlist_error_banner"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Ember)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.message,
                            color = Ember,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("waitlist_error_message")
                        )
                        IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                            Text("✕", color = Ember)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // campsite photo card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NightSurface)
                        .testTag("campsite_photo")
                ) {
                    if (campsite?.imageUrl != null) {
                        AsyncImage(
                            model = campsite.imageUrl,
                            contentDescription = campsite.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Forest),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = campsite?.name ?: "Campsite Photo",
                                color = Mist,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // campsite identity section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = campsite?.name.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = campsite?.locationName.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist,
                            modifier = Modifier.weight(1f)
                        )
                        if (campsite != null && campsite.pricePerNight > 0) {
                            Text(
                                text = "$${campsite.pricePerNight.toInt()} / night",
                                style = MaterialTheme.typography.labelLarge,
                                color = TrailGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // information card 1: lottery registrations open
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Forest, RoundedCornerShape(8.dp))
                        .border(1.dp, TrailGreen, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Lottery Registrations Open",
                        style = MaterialTheme.typography.labelSmall,
                        color = TrailGreen,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lottery?.name.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Entry closes soon. Dates are subject to availability if selected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist
                    )
                    if (campsite != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your Preference: Up to ${campsite.maxOccupancy} Guests",
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist
                        )
                    }
                    if (lottery != null && lottery.id.isNotBlank()) {
                        val entrantCount = when (state) {
                            is WaitlistUiState.NotWaitlisted -> state.entrantCount
                            is WaitlistUiState.Waitlisted -> state.entrantCount
                            else -> 0
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { onViewEntrants(lottery.id) },
                            modifier = Modifier.testTag("view_entrants_button")
                        ) {
                            Text(
                                text = "View Entrants List ($entrantCount)",
                                color = TrailGreen,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // information card 2: before you join disclaimer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NightSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, Moss, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Before You Join",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Joining adds you to this lottery pool, not a booking. Selection does not guarantee any requested dates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If selected, continue to choose available dates and confirm your reservation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // embedded rules agreement section (story 02.11.01)
                RulesAgreementSection(
                    rulesText = campsite?.rules.orEmpty(),
                    isChecked = isRulesAccepted,
                    onCheckedChange = onRulesCheckedChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // embedded join and leave action section (stories 01.02.01 and 01.02.02)
                JoinLotterySection(
                    uiState = state,
                    onJoinClick = onJoinClick,
                    onLeaveClick = onLeaveClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                // secondary cancel action
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("waitlist_cancel_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Moss)
                    )
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// preview of waitlist screen before joining
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun WaitlistScreenNotWaitlistedPreview() {
    TrailsTheme {
        WaitlistContent(
            state = WaitlistUiState.NotWaitlisted(
                lottery = Lottery(
                    id = "lottery-preview",
                    name = "Summer Rockies Long Weekend",
                    waitlistCapacity = 10
                ),
                campsite = Campsite(
                    id = "campsite-preview",
                    name = "Tunnel Mountain Site 14",
                    locationName = "Banff, AB",
                    pricePerNight = 42.0,
                    rules = "Quiet hours 11pm-7am. Store all food in your vehicle."
                ),
                isFull = false,
                isRulesAccepted = false,
                entrantCount = 2
            ),
            onRulesCheckedChange = {},
            onJoinClick = {},
            onLeaveClick = {},
            onBack = {},
            onClearError = {}
        )
    }
}

// preview of waitlist screen when already in the pool
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun WaitlistScreenWaitlistedPreview() {
    TrailsTheme {
        WaitlistContent(
            state = WaitlistUiState.Waitlisted(
                entry = WaitlistEntry(
                    userId = "user-preview",
                    lotteryId = "lottery-preview",
                    partySize = 2,
                    joinedAt = Timestamp(Date()),
                    rulesAccepted = true
                ),
                lottery = Lottery(
                    id = "lottery-preview",
                    name = "Summer Rockies Long Weekend",
                    waitlistCapacity = 10
                ),
                campsite = Campsite(
                    id = "campsite-preview",
                    name = "Tunnel Mountain Site 14",
                    locationName = "Banff, AB",
                    pricePerNight = 42.0
                ),
                entrantCount = 3
            ),
            onRulesCheckedChange = {},
            onJoinClick = {},
            onLeaveClick = {},
            onBack = {},
            onClearError = {}
        )
    }
}
