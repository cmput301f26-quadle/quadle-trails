// join and leave lottery section for campsite waitlists
// owner: chitkarsh (chitkarshrathi) - waitlist
package com.example.trails.ui.waitlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
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

// reusable component representing stories 01.02.01 and 01.02.02
@Composable
fun JoinLotterySection(
    uiState: WaitlistUiState,
    onJoinClick: () -> Unit,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("join_lottery_section")
    ) {
        // ai helped scaffold the state conditional branching here
        when (uiState) {
            is WaitlistUiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = TrailGreen,
                        modifier = Modifier.testTag("waitlist_loading")
                    )
                }
            }

            is WaitlistUiState.Waitlisted -> {
                // story 01.02.02: leave the waiting list
                WaitlistedContent(
                    entry = uiState.entry,
                    onLeaveClick = onLeaveClick
                )
            }

            is WaitlistUiState.NotWaitlisted -> {
                // story 01.02.01: join the waiting list
                NotWaitlistedContent(
                    isFull = uiState.isFull,
                    isRulesAccepted = uiState.isRulesAccepted,
                    entrantCount = uiState.entrantCount,
                    onJoinClick = onJoinClick
                )
            }

            is WaitlistUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Forest, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = uiState.message,
                        color = TrailGreen,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is WaitlistUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NightSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, Ember, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .testTag("waitlist_error_banner")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Ember)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.message,
                            color = Ember,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("waitlist_error_message")
                        )
                    }
                }
            }
        }
    }
}

// view when entrant is already in the lottery pool
@Composable
private fun WaitlistedContent(
    entry: WaitlistEntry,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NightSurface, RoundedCornerShape(8.dp))
            .border(1.dp, TrailGreen, RoundedCornerShape(8.dp))
            .padding(12.dp)
            .testTag("waitlisted_content")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = TrailGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ON WAITING LIST",
                    style = MaterialTheme.typography.labelMedium,
                    color = TrailGreen
                )
            }
            Text(
                text = "${entry.partySize} Guests",
                style = MaterialTheme.typography.bodySmall,
                color = Mist
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You are in this lottery pool. Selection does not guarantee a booking.",
            style = MaterialTheme.typography.bodySmall,
            color = Mist
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onLeaveClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("leave_lottery_button"),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Ember
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = androidx.compose.ui.graphics.SolidColor(Ember)
            )
        ) {
            Text("Leave Lottery Pool")
        }
    }
}

// view when entrant is not on the waitlist yet
@Composable
private fun NotWaitlistedContent(
    isFull: Boolean,
    isRulesAccepted: Boolean,
    entrantCount: Int,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isFull) {
            // full waitlist banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NightSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, Ember, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Ember)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Waitlist Is Full",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Maximum entrants reached ($entrantCount entrants).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = onJoinClick,
            enabled = !isFull && isRulesAccepted,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("join_lottery_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = TrailGreen,
                contentColor = Night,
                disabledContainerColor = Forest,
                disabledContentColor = Mist
            )
        ) {
            Text(if (isFull) "Waitlist Full" else "Join Lottery Pool")
        }
    }
}

// Campsite details button. Hidden when the campsite has no active lottery.
@Composable
fun JoinLotterySection(
    navController: NavController,
    campsiteId: String,
    modifier: Modifier = Modifier
) {
    var isWaitlisted by remember { mutableStateOf(false) }
    var activeLotteryId by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(campsiteId) {
        try {
            val campsite = AppContainer.campsiteRepository.getById(campsiteId)
            val lotteryId = campsite?.activeLotteryId
            activeLotteryId = lotteryId
            if (!lotteryId.isNullOrBlank()) {
                val entry = AppContainer.waitlistRepository.getEntry(lotteryId, CurrentUser.id())
                isWaitlisted = entry != null
            }
        } catch (_: Exception) {
            isWaitlisted = false
        } finally {
            loaded = true
        }
    }

    if (!loaded || activeLotteryId.isNullOrBlank()) return

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        if (isWaitlisted) {
            Button(
                onClick = {
                    navController.navigate(WaitlistRoutes.waitlistScreen(campsiteId))
                },
                modifier = Modifier.testTag("campsite_waitlisted_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NightSurface,
                    contentColor = TrailGreen
                )
            ) {
                Text("View My Waitlist Status")
            }
        } else {
            Button(
                onClick = {
                    navController.navigate(WaitlistRoutes.waitlistScreen(campsiteId))
                },
                modifier = Modifier.testTag("campsite_join_lottery_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrailGreen,
                    contentColor = Night
                )
            ) {
                Text("Join Lottery")
            }
        }
    }
}

// preview of join lottery section when open and not joined
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun JoinLotterySectionPreview_Open() {
    TrailsTheme {
        JoinLotterySection(
            uiState = WaitlistUiState.NotWaitlisted(
                isFull = false,
                isRulesAccepted = false,
                entrantCount = 3
            ),
            onJoinClick = {},
            onLeaveClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

// preview of join lottery section when already on waitlist
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun JoinLotterySectionPreview_Waitlisted() {
    TrailsTheme {
        JoinLotterySection(
            uiState = WaitlistUiState.Waitlisted(
                entry = WaitlistEntry(
                    userId = "user-preview",
                    userName = "Chitkarsh",
                    lotteryId = "lottery-preview",
                    partySize = 2,
                    joinedAt = Timestamp(Date(1791648000000L)),
                    rulesAccepted = true,
                    rulesAcceptedAt = Timestamp(Date(1791648000000L))
                ),
                entrantCount = 4
            ),
            onJoinClick = {},
            onLeaveClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

// preview of join lottery section when waitlist is full
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun JoinLotterySectionPreview_Full() {
    TrailsTheme {
        JoinLotterySection(
            uiState = WaitlistUiState.NotWaitlisted(
                isFull = true,
                isRulesAccepted = false,
                entrantCount = 10
            ),
            onJoinClick = {},
            onLeaveClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
