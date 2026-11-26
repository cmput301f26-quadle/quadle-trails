// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.08.01 (and the entry point to 02.12.01, Neaz's Message entrant)
// PURPOSE: The owner's Reservation details (Figma · US 02.08.01 · Reject a reservation): the
//  stay, the entrant, "Reject reservation" with a confirm dialog and a Try again dialog on
//  failure, and "Message entrant". Opened from My Reservations and Booked dates.
// Outstanding issues: tapping the entrant row opens nothing (no profile screen for owners).
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.model.User
import com.example.trails.ui.messaging.MessagingRoutes
import com.example.trails.ui.profile.Avatar
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.TrailsTheme
import java.text.SimpleDateFormat
import java.util.Locale

/** Key My Reservations reads to show the "Reservation rejected" banner after going back. */
const val REJECTED_BANNER_KEY = "reservation_rejected_banner"

/**
 * Connects [ReservationDetailsViewModel] to navigation. After a rejection it goes back and
 * leaves the banner text for My Reservations.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param reservationId the reservation to show.
 */
@Composable
fun ReservationDetailsScreen(
    navController: NavController,
    reservationId: String,
    modifier: Modifier = Modifier,
    viewModel: ReservationDetailsViewModel = viewModel(key = reservationId) { ReservationDetailsViewModel(reservationId) }
) {
    val state by viewModel.state.collectAsState()
    val reject by viewModel.rejectState.collectAsState()
    LaunchedEffect(reject) {
        val done = reject as? RejectState.Done ?: return@LaunchedEffect
        navController.previousBackStackEntry?.savedStateHandle?.set(REJECTED_BANNER_KEY, done.summary)
        navController.popBackStack()
    }
    ReservationDetailsContent(
        state = state,
        rejectState = reject,
        onBack = { navController.popBackStack() },
        onReject = viewModel::onRejectClick,
        onConfirmReject = viewModel::onConfirmReject,
        onDismissDialog = viewModel::onDismissDialog,
        onMessage = { r -> navController.navigate(MessagingRoutes.chat(r.campsiteId, r.userId)) },
        modifier = modifier
    )
}

/**
 * Stateless Reservation details, laid out as in Figma, with the confirm and error dialogs.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param state what to show.
 * @param rejectState progress of rejecting; decides which dialog is open.
 * @param onBack called from the back arrow.
 * @param onReject called from "Reject reservation".
 * @param onConfirmReject called from "Reject" in the dialog, or "Try again".
 * @param onDismissDialog called from "Cancel" or when a dialog is dismissed.
 * @param onMessage called with the reservation from "Message entrant".
 */
@Composable
fun ReservationDetailsContent(
    state: ReservationDetailsUiState,
    rejectState: RejectState,
    onBack: () -> Unit,
    onReject: () -> Unit,
    onConfirmReject: () -> Unit,
    onDismissDialog: () -> Unit,
    onMessage: (Reservation) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { FormHeader(stringResource(R.string.details_title), onBack) }
    ) { padding ->
        val inner = Modifier.padding(padding).fillMaxSize()
        when (state) {
            ReservationDetailsUiState.Loading -> Box(inner, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ReservationDetailsUiState.NotFound -> Box(inner.padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.details_not_found), textAlign = TextAlign.Center)
            }
            is ReservationDetailsUiState.Loaded -> {
                DetailsBody(state, rejectState, onReject, onMessage, inner)
                val guest = state.guest?.name?.takeIf(String::isNotBlank) ?: stringResource(R.string.reservations_unknown_guest)
                when (rejectState) {
                    RejectState.Confirming, RejectState.Rejecting -> ConfirmRejectDialog(
                        guest, state.campsite?.name, state.reservation, rejectState == RejectState.Rejecting,
                        onConfirmReject, onDismissDialog
                    )
                    RejectState.Failed -> RejectFailedDialog(guest, onConfirmReject, onDismissDialog)
                    else -> Unit
                }
            }
        }
    }
}

/**
 * Photo, summary card, entrant and the two buttons.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun DetailsBody(
    state: ReservationDetailsUiState.Loaded,
    rejectState: RejectState,
    onReject: () -> Unit,
    onMessage: (Reservation) -> Unit,
    modifier: Modifier
) {
    val r = state.reservation
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag(DetailsTags.SCREEN)
    ) {
        val photo = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.primary, shape)
        val url = state.campsite?.imageUrl
        if (url.isNullOrBlank()) Box(photo) else AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = photo)
        Spacer(Modifier.height(16.dp))
        SummaryCard(state)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.details_entrant), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        EntrantRow(state.guest, r, state.filter)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onReject,
            enabled = state.canReject && rejectState == RejectState.Idle,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = Ember, contentColor = MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag(DetailsTags.REJECT)
        ) { Text(stringResource(R.string.details_reject)) }
        if (!state.canReject) {
            Text(
                stringResource(if (state.filter == ReservationFilter.PAST) R.string.details_cant_reject_past else R.string.details_cant_reject_cancelled),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp).testTag(DetailsTags.CANT_REJECT)
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onMessage(r) },
            shape = shape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag(DetailsTags.MESSAGE)
        ) { Text(stringResource(R.string.details_message), color = MaterialTheme.colorScheme.onSurface) }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Status chip and reservation number, campsite and location, then Dates / Guests / Total.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun SummaryCard(state: ReservationDetailsUiState.Loaded) {
    val r = state.reservation
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DetailsStatusChip(r.status, state.filter)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.reservations_reference, reservationReference(r.id)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(state.campsite?.name ?: stringResource(R.string.reservations_unknown_campsite), style = MaterialTheme.typography.titleMedium)
        state.campsite?.locationName?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val start = r.startDate?.toDayKey()
        val end = r.endDate?.toDayKey()
        if (start != null && end != null) {
            val nights = nightsOf(r) ?: 0
            DetailRow(stringResource(R.string.details_dates), "${formatStayDates(start, end)} · ${pluralStringResource(R.plurals.reservations_nights, nights, nights)}")
        }
        DetailRow(stringResource(R.string.details_guests), pluralStringResource(R.plurals.details_guests_count, r.partySize, r.partySize))
        DetailRow(stringResource(R.string.details_total), stringResource(R.string.campsite_price, CampsiteFormFields.formatPrice(r.totalPrice)))
    }
}

/**
 * "Dates ……… Jun 5 – 7, 2027 · 2 nights".
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * CONFIRMED / COMPLETED / CANCELLED / REJECTED, coloured as on My Reservations.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun DetailsStatusChip(status: ReservationStatus, filter: ReservationFilter) {
    val (label, colour) = when {
        status == ReservationStatus.REJECTED -> R.string.reservations_status_rejected to Ember
        filter == ReservationFilter.CANCELLED -> R.string.reservations_status_cancelled to Ember
        filter == ReservationFilter.PAST -> R.string.reservations_status_completed to MaterialTheme.colorScheme.onSurfaceVariant
        else -> R.string.reservations_status_confirmed to MaterialTheme.colorScheme.primary
    }
    Text(
        stringResource(label),
        style = MaterialTheme.typography.labelSmall,
        color = colour,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colour.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(DetailsTags.STATUS)
    )
}

/**
 * Avatar, name and "Entrant · Booked Apr 12, 2027" (or "Stayed …" for a completed stay).
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun EntrantRow(guest: User?, r: Reservation, filter: ReservationFilter) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
            .testTag(DetailsTags.ENTRANT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(guest?.profileImageUrl, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(guest?.name?.takeIf(String::isNotBlank) ?: stringResource(R.string.reservations_unknown_guest), style = MaterialTheme.typography.titleSmall)
            val start = r.startDate?.toDayKey()
            val end = r.endDate?.toDayKey()
            val sub = when {
                filter == ReservationFilter.PAST && start != null && end != null ->
                    stringResource(R.string.details_entrant_stayed, formatStayDates(start, end))
                r.createdAt != null ->
                    stringResource(R.string.details_entrant_booked, SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(r.createdAt.toDate()))
                else -> stringResource(R.string.details_entrant_role)
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/**
 * "Reject reservation?" with Cancel / Reject, as in Figma.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun ConfirmRejectDialog(
    guest: String,
    campsite: String?,
    r: Reservation,
    saving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val start = r.startDate?.toDayKey()
    val end = r.endDate?.toDayKey()
    val dates = if (start != null && end != null) formatStayDates(start, end) else null
    val body = when {
        campsite != null && dates != null -> stringResource(R.string.details_confirm_body, guest, campsite, dates)
        else -> stringResource(R.string.details_confirm_body_short, guest)
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.details_confirm_title)) },
        text = { Text(body) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = Ember, contentColor = MaterialTheme.colorScheme.onPrimary),
                modifier = Modifier.testTag(DetailsTags.CONFIRM)
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.details_confirm_reject))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.testTag(DetailsTags.CANCEL)) {
                Text(stringResource(R.string.details_confirm_cancel), color = MaterialTheme.colorScheme.onSurface)
            }
        },
        modifier = Modifier.testTag(DetailsTags.CONFIRM_DIALOG)
    )
}

/**
 * "Couldn't reject reservation" with Try again, as in Figma.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun RejectFailedDialog(guest: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_failed_title)) },
        text = { Text(stringResource(R.string.details_failed_body, guest)) },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Ember, contentColor = MaterialTheme.colorScheme.onPrimary),
                modifier = Modifier.fillMaxWidth().testTag(DetailsTags.RETRY)
            ) { Text(stringResource(R.string.details_failed_retry)) }
        },
        modifier = Modifier.testTag(DetailsTags.FAILED_DIALOG)
    )
}

/**
 * Test tags for Reservation details.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
object DetailsTags {
    const val SCREEN = "details_screen"
    const val STATUS = "details_status"
    const val ENTRANT = "details_entrant"
    const val REJECT = "details_reject"
    const val CANT_REJECT = "details_cant_reject"
    const val MESSAGE = "details_message"
    const val CONFIRM_DIALOG = "details_confirm_dialog"
    const val CONFIRM = "details_confirm"
    const val CANCEL = "details_cancel"
    const val FAILED_DIALOG = "details_failed_dialog"
    const val RETRY = "details_retry"
}

/** Preview: an active reservation, as in Figma. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun ReservationDetailsPreview() {
    val r = Reservation(id = "qr1048", userId = "u1", campsiteId = "c1", partySize = 2, totalPrice = 110.0,
        startDate = dayKeyToTimestamp(dayKey(2027, 6, 5)), endDate = dayKeyToTimestamp(dayKey(2027, 6, 7)))
    TrailsTheme {
        ReservationDetailsContent(
            state = ReservationDetailsUiState.Loaded(
                r, Campsite(id = "c1", name = "Two Jack Lakeside", locationName = "Banff National Park, AB"),
                User(deviceId = "u1", name = "Priya Shah"), ReservationFilter.ACTIVE
            ),
            rejectState = RejectState.Idle,
            onBack = {}, onReject = {}, onConfirmReject = {}, onDismissDialog = {}, onMessage = {}
        )
    }
}
