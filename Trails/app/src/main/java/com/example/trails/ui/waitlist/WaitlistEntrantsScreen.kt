// screen displaying sorted entrants for a lottery
// owner: chitkarsh (chitkarshrathi) - waitlist
package com.example.trails.ui.waitlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WaitlistEntrantsScreen(
    lotteryId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WaitlistViewModel = viewModel(key = lotteryId) { WaitlistViewModel(lotteryId = lotteryId) }
) {
    val entrants by viewModel.entrants.collectAsState()
    val lottery by viewModel.lottery.collectAsState()
    val campsite by viewModel.campsite.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val isLoading = uiState is WaitlistUiState.Loading
    val errorMessage = (uiState as? WaitlistUiState.Error)?.message

    WaitlistEntrantsContent(
        entrants = entrants,
        lottery = lottery,
        campsite = campsite,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitlistEntrantsContent(
    entrants: List<WaitlistEntry>,
    lottery: Lottery? = null,
    campsite: Campsite? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBack: () -> Unit = {},
    onNotifyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorMessage)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("waitlist_entrants_screen"),
        containerColor = Night,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Waitlist",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("waitlist_entrants_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Night
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // lottery card with status tag and capacity
            LotterySummaryCard(
                lottery = lottery,
                campsite = campsite,
                entrantCount = entrants.size
            )

            Spacer(modifier = Modifier.height(16.dp))

            // header row with entrant count and sort order description
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entrants_header_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entrants.size} Entrants • Sorted by Join Date",
                    color = Mist,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("entrants_header_count")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // entrant lazy column or loading spinner or empty view
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TrailGreen,
                        modifier = Modifier.testTag("waitlist_loading_spinner")
                    )
                }
            } else if (entrants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("waitlist_empty_entrants"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Mist,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No entrants on this waitlist yet.",
                            color = Mist,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // adapted from official compose docs for lazycolumn
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("waitlist_entrants_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = entrants,
                        key = { it.userId }
                    ) { entrant ->
                        EntrantRow(entrant = entrant)
                    }
                }
            }

            // bottom notify action from figma 369:5153
            // following compose material3 guidelines for button styling
            Button(
                onClick = onNotifyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("notify_entrants_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrailGreen,
                    contentColor = NightSurface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Notify Entrants",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// lottery overview card showing current pool status
@Composable
private fun LotterySummaryCard(
    lottery: Lottery?,
    campsite: Campsite?,
    entrantCount: Int,
    modifier: Modifier = Modifier
) {
    val title = campsite?.name ?: lottery?.name?.ifBlank { null } ?: "Lottery Waitlist"
    val isFull = lottery?.waitlistCapacity?.let { entrantCount >= it } ?: false
    val tagText = if (isFull) "WAITLIST FULL" else "WAITLIST OPEN"
    val tagColor = if (isFull) Ember else TrailGreen
    val drawFormatted = formatJoinedDate(lottery?.bookingStart ?: lottery?.registrationEnd)
    val spots = lottery?.numWinners ?: 0
    val closesFormatted = formatJoinedDate(lottery?.registrationEnd)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NightSurface, RoundedCornerShape(10.dp))
            .border(1.dp, Moss, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("lottery_summary_title")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Forest, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = tagText,
                    color = tagColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Draw $drawFormatted",
                color = Mist,
                fontSize = 12.sp
            )
        }

        Text(
            text = "$entrantCount Entrants · $spots Spots · Closes $closesFormatted",
            color = Mist,
            fontSize = 12.sp
        )
    }
}

// single entrant row showing user id or name party size and join date
@Composable
fun EntrantRow(
    entrant: WaitlistEntry,
    modifier: Modifier = Modifier
) {
    val initial = entrant.userName.trim().firstOrNull()?.uppercase() ?: "U"
    val displayName = entrant.userName.ifBlank { entrant.userId }
    val formattedDate = formatJoinedDate(entrant.joinedAt)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("waitlist_entrant_item_${entrant.userId}")
            .background(NightSurface, RoundedCornerShape(8.dp))
            .border(1.dp, Moss, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // avatar circular badge with emerald border
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Forest)
                .border(1.5.dp, TrailGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // profile details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayName,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.testTag("entrant_name_${entrant.userId}")
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Joined $formattedDate · Party of ${entrant.partySize}",
                color = Mist,
                fontSize = 12.sp,
                modifier = Modifier.testTag("entrant_details_${entrant.userId}")
            )
        }

        // arrow to profile or details
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "View Profile",
            tint = Mist,
            modifier = Modifier.size(20.dp)
        )
    }
}

// formats timestamps into readable date format
fun formatJoinedDate(timestamp: Timestamp?): String {
    if (timestamp == null) return "—"
    val date = timestamp.toDate()
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
    return formatter.format(date)
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun WaitlistEntrantsScreenPreview() {
    TrailsTheme {
        WaitlistEntrantsContent(
            entrants = listOf(
                WaitlistEntry(
                    userId = "user-1",
                    userName = "Maya Singh",
                    lotteryId = "lottery-preview",
                    partySize = 2,
                    joinedAt = Timestamp(Date(1791648000000L)),
                    rulesAccepted = true
                ),
                WaitlistEntry(
                    userId = "user-2",
                    userName = "Daniel Kim",
                    lotteryId = "lottery-preview",
                    partySize = 4,
                    joinedAt = Timestamp(Date(1791734400000L)),
                    rulesAccepted = true
                ),
                WaitlistEntry(
                    userId = "user-3",
                    userName = "Lena Fischer",
                    lotteryId = "lottery-preview",
                    partySize = 3,
                    joinedAt = Timestamp(Date(1791820800000L)),
                )
            ),
            lottery = Lottery(
                id = "lottery-preview",
                name = "Grande Cache Ridge · June 2027",
                numWinners = 5,
                waitlistCapacity = 12
            ),
            campsite = Campsite(
                id = "campsite-preview",
                name = "Grande Cache Ridge"
            )
        )
    }
}
