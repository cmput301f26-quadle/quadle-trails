// UI Controller composable displaying winner booking details, calendar date selection, and decline action (MVVM pattern).
// implements US 01.04.01, US 01.04.02, US 01.04.03
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.ui.waitlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.data.AppContainer
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.ui.campsite.CalendarMonth
import com.example.trails.ui.campsite.dayOfMonth
import com.example.trails.ui.campsite.monthCells
import com.example.trails.ui.campsite.monthOf
import com.example.trails.ui.campsite.todayKey
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.Forest
import com.example.trails.ui.theme.Mist
import com.example.trails.ui.theme.Moss
import com.example.trails.ui.theme.Night
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme
import java.text.DateFormatSymbols
import java.util.Locale

/**
 * Screen destination allowing a lottery winner to pick booking dates, party size,
 * review contact details, or decline the lottery win.
 *
 * @param navController navigation controller.
 * @param lotteryId winning lottery identifier.
 * @param campsiteId campsite identifier.
 * @param modifier optional modifier.
 * @param viewModel scoped [ReserveDatesViewModel].
 */
@Composable
fun ReserveDatesScreen(
    navController: NavController,
    lotteryId: String,
    campsiteId: String,
    modifier: Modifier = Modifier,
    viewModel: ReserveDatesViewModel = viewModel(key = "reserve-dates-$lotteryId-$campsiteId") {
        ReserveDatesViewModel(
            lotteryId = lotteryId,
            campsiteId = campsiteId,
            lotteryRepository = AppContainer.lotteryRepository,
            campsiteRepository = AppContainer.campsiteRepository,
            reservationRepository = AppContainer.reservationRepository,
            waitlistRepository = AppContainer.waitlistRepository,
            userRepository = AppContainer.userRepository
        )
    }
) {
    val state by viewModel.uiState.collectAsState()

    ReserveDatesContent(
        state = state,
        onBack = { navController.popBackStack() },
        onNameChange = { viewModel.onNameChange(it) },
        onEmailChange = { viewModel.onEmailChange(it) },
        onPhoneChange = { viewModel.onPhoneChange(it) },
        onIncrementGuests = { viewModel.incrementGuests() },
        onDecrementGuests = { viewModel.decrementGuests() },
        onDayTapped = { viewModel.onDayTapped(it) },
        onReserveClick = {
            val minDay = state.selectedDays.minOrNull() ?: todayKey()
            val maxDay = state.selectedDays.maxOrNull() ?: minDay
            navController.navigate(
                WaitlistRoutes.confirmReservationWithDetails(
                    campsiteId = campsiteId,
                    lotteryId = lotteryId,
                    startDay = minDay,
                    endDay = maxDay,
                    guests = state.guests,
                    contactName = state.name,
                    contactEmail = state.email,
                    contactPhone = state.phone
                )
            )
        },
        onDeclineClick = {
            viewModel.declineLottery {
                navController.popBackStack()
            }
        },
        modifier = modifier
    )
}

/**
 * Stateless layout displaying date reservation details, availability calendar, and actions.
 *
 * @param state the current UI state.
 * @param onBack callback on back navigation.
 * @param onNameChange callback when entrant name changes.
 * @param onEmailChange callback when entrant email changes.
 * @param onPhoneChange callback when entrant phone changes.
 * @param onIncrementGuests callback to increment party size.
 * @param onDecrementGuests callback to decrement party size.
 * @param onDayTapped callback when a calendar date is tapped.
 * @param onReserveClick callback when reserve button is pressed.
 * @param onDeclineClick callback when user confirms declining the lottery.
 * @param modifier optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReserveDatesContent(
    state: ReserveDatesUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onIncrementGuests: () -> Unit,
    onDecrementGuests: () -> Unit,
    onDayTapped: (Long) -> Unit,
    onReserveClick: () -> Unit,
    onDeclineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showDeclineDialog by remember { mutableStateOf(false) }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = {
                Text(
                    text = "Decline Lottery Win?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to decline this invitation? Another entrant will be offered the spot. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeclineDialog = false
                        onDeclineClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ember,
                        contentColor = Night
                    ),
                    modifier = Modifier.testTag("decline_dialog_confirm_button")
                ) {
                    Text("Decline Lottery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeclineDialog = false },
                    modifier = Modifier.testTag("decline_dialog_cancel_button")
                ) {
                    Text("Keep Invitation", color = TrailGreen)
                }
            },
            containerColor = NightSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        topBar = {
            TrailsTopBar(
                title = "Reserve Dates",
                onBack = onBack,
                backTestTag = "reserve_dates_back_button"
            )
        },
        containerColor = Night,
        modifier = modifier.testTag("reserve_dates_screen")
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Campsite subtitle
            val campsiteName = state.campsite?.name.orEmpty()
            val location = state.campsite?.locationName.orEmpty()
            if (campsiteName.isNotBlank()) {
                Text(
                    text = if (location.isNotBlank()) "$campsiteName, $location" else campsiteName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Mist,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Booking Details Section
            Text(
                text = "Confirm your Booking Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Name field
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reserve_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TrailGreen,
                    unfocusedBorderColor = Moss,
                    focusedContainerColor = NightSurface,
                    unfocusedContainerColor = NightSurface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Email field
            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reserve_email_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TrailGreen,
                    unfocusedBorderColor = Moss,
                    focusedContainerColor = NightSurface,
                    unfocusedContainerColor = NightSurface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone field
            OutlinedTextField(
                value = state.phone,
                onValueChange = onPhoneChange,
                label = { Text("Phone") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reserve_phone_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TrailGreen,
                    unfocusedBorderColor = Moss,
                    focusedContainerColor = NightSurface,
                    unfocusedContainerColor = NightSurface
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Number of Guests stepper
            val maxOccupancy = state.campsite?.maxOccupancy ?: 8
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NightSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, Moss, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Number of Guests (Max $maxOccupancy)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDecrementGuests,
                        enabled = state.guests > 1,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("reserve_guests_decrease")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease guests",
                            tint = if (state.guests > 1) TrailGreen else Mist.copy(alpha = 0.4f)
                        )
                    }

                    Text(
                        text = "${state.guests}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("reserve_guests_count")
                    )

                    IconButton(
                        onClick = onIncrementGuests,
                        enabled = state.guests < maxOccupancy,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("reserve_guests_increase")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase guests",
                            tint = if (state.guests < maxOccupancy) TrailGreen else Mist.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Preferred Dates Section
            Text(
                text = "Select your Preferred Dates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Month availability calendar matching Figma node 391:1636
            WinnerAvailabilityCalendar(
                availableDays = state.availableDays,
                bookedDays = state.bookedDays,
                selectedDays = state.selectedDays,
                onDayTapped = onDayTapped,
                modifier = Modifier.testTag("reserve_dates_calendar")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            // Button 1: Reserve (green button, per user prompt)
            Button(
                onClick = onReserveClick,
                enabled = state.canProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrailGreen,
                    contentColor = Night,
                    disabledContainerColor = Forest,
                    disabledContentColor = Mist.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reserve_button")
            ) {
                Text(
                    text = "Reserve",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button 2: Decline Lottery (red button, story 01.04.03)
            Button(
                onClick = { showDeclineDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ember,
                    contentColor = Night
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("decline_lottery_button")
            ) {
                Text(
                    text = "Decline Lottery",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Calendar component designed for winners to view open/booked days and select reservation dates.
 *
 * @param availableDays days offering availability.
 * @param bookedDays days locked by active reservations (red).
 * @param selectedDays days selected by the entrant (solid green).
 * @param onDayTapped callback when a day key is clicked.
 * @param modifier optional modifier.
 */
@Composable
fun WinnerAvailabilityCalendar(
    availableDays: Set<Long>,
    bookedDays: Set<Long>,
    selectedDays: Set<Long>,
    onDayTapped: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { todayKey() }
    val initialMonth = remember {
        (selectedDays + availableDays + bookedDays).minOrNull()?.let(::monthOf) ?: monthOf(today)
    }
    var year by rememberSaveable { mutableIntStateOf(initialMonth.year) }
    var month by rememberSaveable { mutableIntStateOf(initialMonth.month) }
    val shown = CalendarMonth(year, month)
    val cells = monthCells(shown)
    val monthName = DateFormatSymbols(Locale.getDefault()).months[month - 1]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NightSurface)
            .border(1.dp, Moss, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Month navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    val prev = shown.plus(-1)
                    year = prev.year
                    month = prev.month
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = TrailGreen)
            }

            Text(
                text = "$monthName $year",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            IconButton(
                onClick = {
                    val next = shown.plus(1)
                    year = next.year
                    month = next.month
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = TrailGreen)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday initials
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { initial ->
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelSmall,
                    color = Mist,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 7-day grid rows
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val key = week.getOrNull(i)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key != null) {
                            val isSelected = key in selectedDays
                            val isBooked = key in bookedDays
                            val isOpen = key in availableDays && !isBooked

                            val background = when {
                                isSelected -> TrailGreen
                                isBooked -> Ember
                                isOpen -> TrailGreen.copy(alpha = 0.22f)
                                else -> Color.Transparent
                            }

                            val textColor = when {
                                isSelected -> Night
                                isBooked -> Night
                                isOpen -> TrailGreen
                                else -> Mist.copy(alpha = 0.4f)
                            }

                            val borderModifier = if (isOpen && !isSelected) {
                                Modifier.border(0.5.dp, TrailGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            } else {
                                Modifier
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(background)
                                    .then(borderModifier)
                                    .clickable(enabled = isOpen) { onDayTapped(key) }
                                    .testTag("calendar_day_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${dayOfMonth(key)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected || isBooked || isOpen) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend / summary footer
        val monthDays = cells.filterNotNull()
        val openCount = monthDays.count { it in availableDays && it !in bookedDays }
        val bookedCount = monthDays.count { it in bookedDays }

        Text(
            text = when {
                selectedDays.isNotEmpty() -> "${selectedDays.size} night${if (selectedDays.size > 1) "s" else ""} selected"
                bookedCount > 0 -> "$openCount days available · $bookedCount booked"
                else -> "$openCount days available this month"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (selectedDays.isNotEmpty()) TrailGreen else Mist,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun ReserveDatesPreview() {
    TrailsTheme {
        ReserveDatesContent(
            state = ReserveDatesUiState(
                isLoading = false,
                lottery = Lottery(name = "Summer Long Weekend Draw"),
                campsite = Campsite(
                    name = "Two Jack Lakeside Campground",
                    locationName = "Banff, AB",
                    maxOccupancy = 6
                ),
                name = "Alex Morgan",
                email = "alex.morgan@email.com",
                phone = "780-555-0142",
                guests = 2,
                availableDays = setOf(20000L, 20001L, 20002L, 20003L),
                bookedDays = setOf(20002L),
                openDays = setOf(20000L, 20001L, 20003L),
                selectedDays = setOf(20000L, 20001L)
            ),
            onBack = {},
            onNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onIncrementGuests = {},
            onDecrementGuests = {},
            onDayTapped = {},
            onReserveClick = {},
            onDeclineClick = {}
        )
    }
}
