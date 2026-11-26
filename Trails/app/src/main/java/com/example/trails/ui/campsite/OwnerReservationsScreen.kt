// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.07.01, 02.07.02, 02.07.03
// PURPOSE: The owner's reservations across their campsites, filtered by Active / Past /
//  Cancelled (Figma · US 02.07.01–03 · Owner reservations). Shown as the Reservations tab of
//  Hosting.
// Outstanding issues: "View details" opens nothing yet (Reservation details is US 02.08.01);
//  cancelled cards show the reservation number instead of the cancel date (not stored).
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.TrailsTheme
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Connects [OwnerReservationsViewModel] to [OwnerReservationsContent].
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param onViewDetails called with a reservation id from "View details".
 */
@Composable
fun OwnerReservationsScreen(
    modifier: Modifier = Modifier,
    onViewDetails: (String) -> Unit = {},
    viewModel: OwnerReservationsViewModel = viewModel { OwnerReservationsViewModel() }
) {
    val state by viewModel.state.collectAsState()
    OwnerReservationsContent(
        state = state,
        onFilterChange = viewModel::onFilterChange,
        onCampsiteChange = viewModel::onCampsiteChange,
        onViewDetails = onViewDetails,
        modifier = modifier
    )
}

/**
 * Stateless reservations list: Campsite dropdown, Active / Past / Cancelled chips, a count
 * line and the cards, as in Figma.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param state what to show.
 * @param onFilterChange called when a chip is tapped.
 * @param onCampsiteChange called with a campsite id, or null for All campsites.
 * @param onViewDetails called with a reservation id from "View details".
 */
@Composable
fun OwnerReservationsContent(
    state: OwnerReservationsUiState,
    onFilterChange: (ReservationFilter) -> Unit,
    onCampsiteChange: (String?) -> Unit,
    onViewDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Column(modifier.fillMaxSize().testTag(ReservationsTags.SCREEN)) {
        Text(stringResource(R.string.reservations_campsite_label), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        CampsitePicker(state.campsites, state.selectedCampsiteId, onCampsiteChange)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReservationFilter.entries.forEach { chip -> FilterChipFor(chip, chip == state.filter, onFilterChange) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.reservations_summary, countLabel(state.filter, state.items.size), longDate(state.today)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(ReservationsTags.SUMMARY)
        )
        Spacer(Modifier.height(12.dp))
        if (state.items.isEmpty()) {
            Text(
                stringResource(emptyMessage(state.filter)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp).testTag(ReservationsTags.EMPTY)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.testTag(ReservationsTags.LIST)
            ) {
                items(state.items, key = { it.reservation.id }) { ReservationCard(it, onViewDetails) }
            }
        }
    }
}

/**
 * The Campsite dropdown, shared by My Reservations and Booked dates.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param selectedId the chosen campsite, or null for All campsites.
 * @param allowAll true to offer "All campsites" (My Reservations); false on Booked dates.
 */
@Composable
internal fun CampsitePicker(
    campsites: List<Campsite>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    allowAll: Boolean = true
) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val allLabel = stringResource(R.string.reservations_all_campsites)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .testTag(ReservationsTags.CAMPSITE_PICKER),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                campsites.find { it.id == selectedId }?.name ?: allLabel,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (allowAll) DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(null); open = false })
            campsites.forEach { site ->
                DropdownMenuItem(
                    text = { Text(site.name) },
                    onClick = { onSelect(site.id); open = false },
                    modifier = Modifier.testTag(ReservationsTags.campsiteOption(site.id))
                )
            }
        }
    }
}

/**
 * One chip; green with a check mark when selected, as in Figma.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun FilterChipFor(chip: ReservationFilter, selected: Boolean, onSelect: (ReservationFilter) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onSelect(chip) },
        label = { Text(stringResource(chip.label)) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.testTag(ReservationsTags.filter(chip))
    )
}

/**
 * A reservation card: photo, guest, campsite, dates and nights, guests and total, status chip,
 * reservation number and "View details".
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun ReservationCard(item: OwnerReservationItem, onViewDetails: (String) -> Unit) {
    val r = item.reservation
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
            .testTag(ReservationsTags.card(r.id))
    ) {
        Row {
            CardPhoto(item.campsite?.imageUrl)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.guestName ?: stringResource(R.string.reservations_unknown_guest),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    item.campsite?.name ?: stringResource(R.string.reservations_unknown_campsite),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                stayLine(r)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(
                    pluralStringResource(R.plurals.reservations_guests_total, r.partySize, r.partySize, CampsiteFormFields.formatPrice(r.totalPrice)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(item)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.reservations_reference, reservationReference(r.id)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onViewDetails(r.id) },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag(ReservationsTags.details(r.id))
        ) { Text(stringResource(R.string.reservations_view_details), color = MaterialTheme.colorScheme.onSurface) }
    }
}

/**
 * The campsite's photo, or a plain box when it has none.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun CardPhoto(url: String?) {
    val modifier = Modifier
        .size(64.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
    if (url.isNullOrBlank()) Box(modifier)
    else AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}

/**
 * CONFIRMED (green), COMPLETED (grey), CANCELLED or REJECTED (red), as in Figma.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun StatusChip(item: OwnerReservationItem) {
    val (label, colour) = when {
        item.reservation.status == ReservationStatus.REJECTED -> R.string.reservations_status_rejected to Ember
        item.filter == ReservationFilter.CANCELLED -> R.string.reservations_status_cancelled to Ember
        item.filter == ReservationFilter.PAST -> R.string.reservations_status_completed to MaterialTheme.colorScheme.onSurfaceVariant
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
            .testTag(ReservationsTags.status(item.reservation.id))
    )
}

/**
 * "May 15 – 17, 2027 · 2 nights", or null if a date is missing.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun stayLine(r: Reservation): String? {
    val start = r.startDate?.toDayKey() ?: return null
    val end = r.endDate?.toDayKey() ?: return null
    val nights = nightsOf(r) ?: 0
    return "${formatStayDates(start, end)} · ${pluralStringResource(R.plurals.reservations_nights, nights, nights)}"
}

/**
 * "2 upcoming reservations" / "2 completed reservations" / "2 cancelled reservations".
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun countLabel(filter: ReservationFilter, count: Int): String = pluralStringResource(
    when (filter) {
        ReservationFilter.ACTIVE -> R.plurals.reservations_count_active
        ReservationFilter.PAST -> R.plurals.reservations_count_past
        ReservationFilter.CANCELLED -> R.plurals.reservations_count_cancelled
    },
    count, count
)

/**
 * Message when a chip has no reservations.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
private fun emptyMessage(filter: ReservationFilter): Int = when (filter) {
    ReservationFilter.ACTIVE -> R.string.reservations_empty_active
    ReservationFilter.PAST -> R.string.reservations_empty_past
    ReservationFilter.CANCELLED -> R.string.reservations_empty_cancelled
}

/**
 * "May 1, 2027" in the phone's language.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
private fun longDate(key: Long): String {
    val m = monthOf(key)
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(GregorianCalendar(m.year, m.month - 1, dayOfMonth(key)).time)
}

/**
 * Test tags for My Reservations.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
object ReservationsTags {
    const val SCREEN = "reservations_screen"
    const val CAMPSITE_PICKER = "reservations_campsite_picker"
    const val SUMMARY = "reservations_summary"
    const val LIST = "reservations_list"
    const val EMPTY = "reservations_empty"

    /** Tag for a filter chip. */
    fun filter(chip: ReservationFilter) = "reservations_filter_${chip.name.lowercase()}"

    /** Tag for a campsite in the dropdown. */
    fun campsiteOption(id: String) = "reservations_campsite_$id"

    /** Tag for a reservation card. */
    fun card(id: String) = "reservations_card_$id"

    /** Tag for a card's status chip. */
    fun status(id: String) = "reservations_status_$id"

    /** Tag for a card's "View details" button. */
    fun details(id: String) = "reservations_details_$id"
}

/** Preview with two active reservations, as in Figma. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun OwnerReservationsPreview() {
    val site = Campsite(id = "c1", name = "Grande Cache Ridge")
    val r = Reservation(id = "qr1042", userId = "u1", campsiteId = "c1", partySize = 4, totalPrice = 130.0,
        startDate = dayKeyToTimestamp(dayKey(2027, 5, 15)), endDate = dayKeyToTimestamp(dayKey(2027, 5, 17)))
    TrailsTheme {
        OwnerReservationsContent(
            state = OwnerReservationsUiState(
                isLoading = false,
                campsites = listOf(site),
                items = listOf(OwnerReservationItem(r, "Alex Morgan", site, ReservationFilter.ACTIVE)),
                today = dayKey(2027, 5, 1)
            ),
            onFilterChange = {}, onCampsiteChange = {}, onViewDetails = {}
        )
    }
}
