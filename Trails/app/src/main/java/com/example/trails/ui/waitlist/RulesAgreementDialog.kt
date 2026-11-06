// UI Controller component displaying campsite rules and cancellation policy agreement (UI Controller in MVVM).
// implements US 02.11.01
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.Forest
import com.example.trails.ui.theme.Mist
import com.example.trails.ui.theme.Moss
import com.example.trails.ui.theme.Night
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Date

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
 * Screen enabling an entrant to review campsite rules and confirm their reservation.
 *
 * Observes asynchronous retrieval of campsite rules and cancellation policy, as well as submission state.
 * Triggers back navigation via [navController] upon cancellation or successful confirmation.
 *
 * @param navController navigation controller for screen transitions.
 * @param campsiteId identifier of the campsite being reserved.
 * @param modifier optional [Modifier] for screen styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmReservationScreen(
    navController: NavController,
    campsiteId: String,
    modifier: Modifier = Modifier
) {
    var isChecked by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var campsiteRules by remember { mutableStateOf("Loading rules...") }
    var campsiteCancellation by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(campsiteId) {
        try {
            val campsite = AppContainer.campsiteRepository.getById(campsiteId)
            campsiteRules = campsite?.rules ?: "No rules available."
            campsiteCancellation = campsite?.cancellationPolicy ?: ""
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
                                    val campsite = AppContainer.campsiteRepository.getById(campsiteId)
                                    val lotteryId = campsite?.activeLotteryId
                                    if (!lotteryId.isNullOrBlank()) {
                                        val now = Timestamp(Date())
                                        val currentUserId = CurrentUser.id()
                                        val userProfile = AppContainer.userRepository.getById(currentUserId)
                                        val currentUserName = userProfile?.name?.ifBlank { null } ?: currentUserId
                                        val newEntry = WaitlistEntry(
                                            userId = currentUserId,
                                            userName = currentUserName,
                                            lotteryId = lotteryId,
                                            partySize = 1,
                                            joinedAt = now,
                                            status = WaitlistStatus.WAITING,
                                            rulesAcceptedAt = now,
                                            rulesAccepted = true
                                        )
                                        AppContainer.waitlistRepository.save(newEntry)
                                        navController.popBackStack()
                                    }
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
                            .testTag("confirm_reservation_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrailGreen,
                            contentColor = Night,
                            disabledContainerColor = Forest,
                            disabledContentColor = Mist
                        )
                    ) {
                        Text("Confirm Reservation")
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // embedded rules section instead of dialog popup
            RulesAgreementSection(
                rulesText = campsiteRules,
                cancellationPolicy = campsiteCancellation,
                isChecked = isChecked,
                onCheckedChange = { isChecked = it }
            )
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
