// UI Controller component displaying campsite rules agreement and confirmed reservation creation (MVVM pattern).
// implements US 01.04.01, US 01.04.02, US 02.11.01
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.ReservationRepository
import com.example.trails.data.repository.WaitlistRepository
import com.example.trails.ui.campsite.dayKeyToTimestamp
import com.example.trails.ui.campsite.dayOfMonth
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
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Date
import java.util.Locale
import java.util.UUID

// standard embedded scrollable rules section matching story 02.11.01
/**
 * Scrollable card and agreement checkbox presenting campsite rules and cancellation policy.
 *
 * Observes [rulesText], [cancellationPolicy], and [isChecked].
 * Triggers [onCheckedChange] when the user toggles the acceptance checkbox.
 *
 * @param rulesText rules text retrieved from the campsite document.
 * @param cancellationPolicy cancellation policy text from the campsite.
 * @param isChecked whether the user has checked the agreement checkbox.
 * @param onCheckedChange callback triggered when the checkbox state changes.
 * @param modifier optional [Modifier] for layout adjustments.
 */
@Composable
fun RulesAgreementSection(
    rulesText: String,
    cancellationPolicy: String = "",
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Campsite Rules & Policies",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // scrollable card displaying house rules and cancellation policy
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .background(NightSurface, RoundedCornerShape(8.dp))
                .border(1.dp, Moss, RoundedCornerShape(8.dp))
                .padding(12.dp)
                .verticalScroll(scrollState)
        ) {
            Column {
                if (rulesText.isNotBlank()) {
                    RuleRow(
                        icon = Icons.Default.Policy,
                        title = "Campsite Rules",
                        detail = rulesText
                    )
                }
                if (cancellationPolicy.isNotBlank()) {
                    if (rulesText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    RuleRow(
                        icon = Icons.Default.AccessTime,
                        title = "Cancellation Policy",
                        detail = cancellationPolicy
                    )
                }
                if (rulesText.isBlank() && cancellationPolicy.isBlank()) {
                    Text(
                        text = "Standard park guidelines apply. Treat the campsite with care and respect fellow campers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Mist
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // checkbox agreement row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag("rules_checkbox"),
                colors = CheckboxDefaults.colors(
                    checkedColor = TrailGreen,
                    checkmarkColor = Night,
                    uncheckedColor = Mist
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "I have read and agree to the campsite rules",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                if (isChecked) {
                    Text(
                        text = "Rules accepted. You can now confirm your reservation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrailGreen,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "Agree to the campsite rules to confirm your reservation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// helper row for displaying a single rule with icon
// ai helped scaffold this icon and text row helper
@Composable
private fun RuleRow(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TrailGreen,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = Mist
            )
        }
    }
}

// screen for confirming reservation with embedded rules agreement section
/**
 * Formats calendar day keys into a human-readable stay date range (e.g. "May 22 – 24, 2027").
 *
 * @param startKey check-in day key.
 * @param endKey check-out day key.
 * @return formatted date string.
 */
fun formatStayDates(startKey: Long, endKey: Long): String {
    val startMonth = monthOf(startKey)
    val endMonth = monthOf(endKey)
    val startMonthName = DateFormatSymbols(Locale.getDefault()).shortMonths[startMonth.month - 1]
    val startDay = dayOfMonth(startKey)
    val endDay = dayOfMonth(endKey)
    val year = startMonth.year
    return if (startKey == endKey) {
        "$startMonthName $startDay, $year"
    } else if (startMonth == endMonth) {
        "$startMonthName $startDay – $endDay, $year"
    } else {
        val endMonthName = DateFormatSymbols(Locale.getDefault()).shortMonths[endMonth.month - 1]
        "$startMonthName $startDay – $endMonthName $endDay, $year"
    }
}

/**
 * Screen enabling an entrant to review campsite rules, booking summary, and confirm their reservation.
 *
 * Observes asynchronous retrieval of campsite rules and cancellation policy, as well as submission state.
 * Triggers back navigation via [navController] upon cancellation or successful confirmation.
 *
 * @param navController navigation controller for screen transitions.
 * @param campsiteId identifier of the campsite being reserved.
 * @param lotteryId optional identifier of the lottery won.
 * @param startDayKey optional start date day key.
 * @param endDayKey optional end date day key.
 * @param guests party size for the reservation.
 * @param contactName optional entrant contact name.
 * @param contactEmail optional entrant contact email.
 * @param contactPhone optional entrant contact phone.
 * @param modifier optional [Modifier] for screen styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmReservationScreen(
    navController: NavController,
    campsiteId: String,
    lotteryId: String? = null,
    startDayKey: Long? = null,
    endDayKey: Long? = null,
    guests: Int = 1,
    contactName: String? = null,
    contactEmail: String? = null,
    contactPhone: String? = null,
    modifier: Modifier = Modifier,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    reservationRepository: ReservationRepository = AppContainer.reservationRepository,
    waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository
) {
    var isChecked by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var campsite by remember { mutableStateOf<Campsite?>(null) }
    var campsiteRules by remember { mutableStateOf("Loading rules...") }
    var campsiteCancellation by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val today = remember { todayKey() }
    val startKey = startDayKey ?: today
    val endKey = endDayKey ?: startKey
    val nights = ((endKey - startKey) + 1).coerceAtLeast(1)
    val pricePerNight = campsite?.pricePerNight ?: 0.0
    val totalPrice = pricePerNight * nights

    LaunchedEffect(campsiteId) {
        try {
            val site = campsiteRepository.getById(campsiteId)
            campsite = site
            campsiteRules = site?.rules ?: "No rules available."
            campsiteCancellation = site?.cancellationPolicy ?: ""
        } catch (_: Exception) {
            campsiteRules = "No rules available."
            campsiteCancellation = ""
        }
    }

    Scaffold(
        topBar = {
            TrailsTopBar(
                title = "Confirm Reservation",
                onBack = { navController.popBackStack() },
                backTestTag = "confirm_reservation_back_button"
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NightSurface,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (isSubmitting) return@Button
                            isSubmitting = true
                            coroutineScope.launch {
                                try {
                                    val currentUserId = CurrentUser.id()
                                    val targetLotteryId = lotteryId?.ifBlank { null }
                                        ?: campsite?.activeLotteryId?.ifBlank { null }
                                    val now = Timestamp.now()
                                    val startTimestamp = dayKeyToTimestamp(startKey)
                                    val endTimestamp = dayKeyToTimestamp(endKey)

                                    // Check if user already has an active reservation for overlapping dates
                                    val userReservations = try {
                                        reservationRepository.observeForUser(currentUserId).first()
                                    } catch (_: Exception) {
                                        emptyList()
                                    }

                                    val hasConflict = userReservations.any { existing ->
                                        existing.status == ReservationStatus.ACTIVE &&
                                            existing.startDate != null &&
                                            existing.endDate != null &&
                                            maxOf(existing.startDate.seconds, startTimestamp.seconds) <= minOf(existing.endDate.seconds, endTimestamp.seconds)
                                    }

                                    if (hasConflict) {
                                        errorMessage = "You already have an active reservation for overlapping dates."
                                        isSubmitting = false
                                        return@launch
                                    }

                                    // create and persist confirmed reservation (story 02.11.01)
                                    val newReservation = Reservation(
                                        id = UUID.randomUUID().toString(),
                                        userId = currentUserId,
                                        campsiteId = campsiteId,
                                        lotteryId = targetLotteryId,
                                        partySize = guests,
                                        startDate = dayKeyToTimestamp(startKey),
                                        endDate = dayKeyToTimestamp(endKey),
                                        totalPrice = totalPrice,
                                        rulesAccepted = true,
                                        rulesAcceptedAt = now,
                                        createdAt = now,
                                        status = ReservationStatus.ACTIVE
                                    )
                                    reservationRepository.save(newReservation)

                                    // update waitlist entry status to RESERVED if attached to a lottery
                                    if (!targetLotteryId.isNullOrBlank()) {
                                        waitlistRepository.updateStatus(
                                            lotteryId = targetLotteryId,
                                            userId = currentUserId,
                                            status = WaitlistStatus.RESERVED
                                        )
                                    }

                                    navController.popBackStack()
                                } catch (_: Exception) {
                                    // ignore transient failure and re-enable button
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = isChecked && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_reservation_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrailGreen,
                            contentColor = Night,
                            disabledContainerColor = Forest,
                            disabledContentColor = Mist.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Confirm Reservation",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = Night
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (!errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Ember.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, Ember, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .testTag("confirm_reservation_error_banner")
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Ember,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Booking summary card matching Figma node 367:2870
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NightSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, Moss, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .testTag("confirm_booking_summary_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val imageUrl = campsite?.imageUrl
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Forest)
                    ) {
                        if (!imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = campsite?.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = campsite?.name ?: "Loading...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = campsite?.locationName.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Mist
                        )
                        if (pricePerNight > 0) {
                            Text(
                                text = "$${pricePerNight.toInt()} / night",
                                style = MaterialTheme.typography.labelMedium,
                                color = TrailGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stay details rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Dates", style = MaterialTheme.typography.bodySmall, color = Mist)
                    Text(
                        text = "${formatStayDates(startKey, endKey)} · $nights night${if (nights > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Guests", style = MaterialTheme.typography.bodySmall, color = Mist)
                    Text(
                        text = "$guests guest${if (guests > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total", style = MaterialTheme.typography.bodySmall, color = Mist)
                    Text(
                        text = "$${totalPrice.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // embedded rules section (story 02.11.01)
            RulesAgreementSection(
                rulesText = campsiteRules,
                cancellationPolicy = campsiteCancellation,
                isChecked = isChecked,
                onCheckedChange = { isChecked = it }
            )

            // Feedback helper text
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isChecked) {
                    "Rules accepted. You can now confirm your reservation."
                } else {
                    "You must review and accept the campsite rules."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isChecked) TrailGreen else Mist.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// preview of rules agreement section with policies and checkbox
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun RulesAgreementSectionPreview() {
    TrailsTheme {
        RulesAgreementSection(
            rulesText = "Quiet hours 10pm-7am. No generators allowed.",
            cancellationPolicy = "Free cancellation up to 7 days before arrival.",
            isChecked = false,
            onCheckedChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
