// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.06.01
// PURPOSE: Booked dates (Figma · US 02.06.01 · View booked dates): pick a campsite, see its
//  month with booked nights red, tap a booked night to see its reservation. Opened from the
//  owner's campsite page.
// Outstanding issues: none. "View reservation" opens Reservation details (US 02.08.01).
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.DateRange
import com.example.trails.data.model.Reservation
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.TrailsTheme
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Connects [BookedDatesViewModel] to navigation and renders [BookedDatesContent].
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param campsiteId the campsite to open on.
 */
@Composable
fun BookedDatesScreen(
    navController: NavController,
    campsiteId: String?,
    modifier: Modifier = Modifier,
    viewModel: BookedDatesViewModel = viewModel { BookedDatesViewModel(campsiteId) }
) {
    val state by viewModel.state.collectAsState()
    BookedDatesContent(
        state = state,
        onBack = { navController.popBackStack() },
        onCampsiteChange = viewModel::onCampsiteChange,
        onMonthChange = viewModel::onMonthChange,
        onDayClick = viewModel::onDayClick,
        onEditAvailability = { id -> navController.navigate(CampsiteRoutes.campsiteForm(id)) },
        onViewReservation = { id -> navController.navigate(CampsiteRoutes.reservationDetails(id)) },
        modifier = modifier
    )
}

/**
 * Stateless Booked dates screen, laid out as in Figma.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param state what to show.
 * @param onBack called from the back arrow.
 * @param onCampsiteChange called with a campsite id from the dropdown.
 * @param onMonthChange called with -1 or 1 from the month arrows.
 * @param onDayClick called with a day key when a day is tapped.
 * @param onEditAvailability called with the campsite id from "Edit availability".
 * @param onViewReservation called with a reservation id from "View reservation".
 */
@Composable
fun BookedDatesContent(
    state: BookedDatesUiState,
    onBack: () -> Unit,
    onCampsiteChange: (String) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayClick: (Long) -> Unit,
    onEditAvailability: (String) -> Unit,
    onViewReservation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { FormHeader(stringResource(R.string.booked_title), onBack) }
    ) { padding ->
        val inner = Modifier.padding(padding).fillMaxSize()
        when {
            state.isLoading -> Box(inner, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.campsite == null -> Box(inner.padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.booked_no_campsites), textAlign = TextAlign.Center)
            }
            else -> Column(
                inner
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .testTag(BookedTags.SCREEN)
            ) {
                Text(stringResource(R.string.reservations_campsite_label), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                CampsitePicker(state.campsites, state.campsite.id, { id -> id?.let(onCampsiteChange) }, allowAll = false)
                Spacer(Modifier.height(16.dp))
                MonthBar(state.month, onMonthChange)
                Spacer(Modifier.height(12.dp))
                if (state.summary.reservations == 0) {
                    EmptyMonth(state.campsite, state.month) { onEditAvailability(state.campsite.id) }
                } else {
                    BookedGrid(state, onDayClick)
                    Spacer(Modifier.height(12.dp))
                    Legend(showSelected = state.selectedDay != null)
                    Spacer(Modifier.height(16.dp))
                    val selected = state.selectedReservation
                    if (selected != null && state.selectedDay != null) {
                        SelectedBooking(state.selectedDay, selected, state.guestNames[selected.userId], onViewReservation)
                    } else {
                        MonthSummaryCard(state.summary)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * ‹ May 2027 › with boxed arrow buttons.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun MonthBar(month: CalendarMonth, onMonthChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ArrowBox(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.campsite_calendar_prev), BookedTags.PREV) { onMonthChange(-1) }
        Text(
            monthLabel(month),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).testTag(BookedTags.MONTH)
        )
        ArrowBox(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.campsite_calendar_next), BookedTags.NEXT) { onMonthChange(1) }
    }
}

/**
 * A small outlined square with a green arrow.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun ArrowBox(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .size(32.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
}

/**
 * The month grid in a bordered box: green available, red booked (white ring when selected),
 * outlined otherwise.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun BookedGrid(state: BookedDatesUiState, onDayClick: (Long) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(8.dp)
    ) {
        Row {
            weekdayInitials().forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                )
            }
        }
        monthCells(state.month).chunked(7).forEach { week ->
            Row {
                for (i in 0 until 7) {
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) {
                        week.getOrNull(i)?.let { key ->
                            BookedDayCell(key, key in state.available, key in state.booked, key == state.selectedDay, onDayClick)
                        }
                    }
                }
            }
        }
    }
}

/**
 * One day cell.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun BookedDayCell(key: Long, available: Boolean, booked: Boolean, selected: Boolean, onClick: (Long) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val bookedState = stringResource(R.string.campsite_calendar_booked_state)
    val fill = when {
        booked -> Ember
        available -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val border = when {
        selected -> BorderStroke(2.dp, Color.White)
        booked || available -> null
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(fill)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable { onClick(key) }
            .semantics {
                this.selected = selected
                if (booked) stateDescription = bookedState
            }
            .testTag(BookedTags.day(key)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            dayOfMonth(key).toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (booked || available) FontWeight.Bold else FontWeight.Normal,
            color = if (booked || available) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Available · Booked · Unavailable (· Selected), as in Figma.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun Legend(showSelected: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(MaterialTheme.colorScheme.primary, null, stringResource(R.string.booked_legend_available))
        LegendItem(Ember, null, stringResource(R.string.booked_legend_booked))
        LegendItem(Color.Transparent, BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), stringResource(R.string.booked_legend_unavailable))
        if (showSelected) LegendItem(Ember, BorderStroke(1.5.dp, Color.White), stringResource(R.string.booked_legend_selected))
    }
}

/**
 * One legend swatch and label.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun LegendItem(fill: Color, border: BorderStroke?, label: String) {
    val shape = RoundedCornerShape(3.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .clip(shape)
                .background(fill)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * "1 reservation · 2 of 10 open nights booked" and the tap hint.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun MonthSummaryCard(summary: BookedMonthSummary) {
    CardBox(Modifier.testTag(BookedTags.SUMMARY)) {
        Text(
            pluralStringResource(R.plurals.booked_summary_reservations, summary.reservations, summary.reservations) + " · " +
                stringResource(R.string.booked_summary_nights, summary.bookedNights, summary.openNights),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.booked_tap_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The tapped night's booking, with "View reservation".
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun SelectedBooking(day: Long, r: Reservation, guestName: String?, onView: (String) -> Unit) {
    CardBox(Modifier.testTag(BookedTags.SELECTED)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.booked_day_booked, fullDayLabel(day)),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.reservations_status_confirmed),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            (guestName ?: stringResource(R.string.reservations_unknown_guest)) + " · " +
                pluralStringResource(R.plurals.reservations_guests_total, r.partySize, r.partySize, CampsiteFormFields.formatPrice(r.totalPrice)),
            style = MaterialTheme.typography.bodyMedium
        )
        val start = r.startDate?.toDayKey()
        val end = r.endDate?.toDayKey()
        if (start != null && end != null) {
            val nights = nightsOf(r) ?: 0
            Text(
                "${formatStayDates(start, end)} · ${pluralStringResource(R.plurals.reservations_nights, nights, nights)} · " +
                    stringResource(R.string.reservations_reference, reservationReference(r.id)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onView(r.id) },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag(BookedTags.VIEW_RESERVATION)
        ) { Text(stringResource(R.string.booked_view_reservation), color = MaterialTheme.colorScheme.onSurface) }
    }
}

/**
 * "No booked dates" with a tent-style icon and "Edit availability", as in Figma.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun EmptyMonth(campsite: Campsite, month: CalendarMonth, onEditAvailability: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp).testTag(BookedTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.booked_empty_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.booked_empty_body, campsite.name, monthLabel(month)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onEditAvailability,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag(BookedTags.EDIT_AVAILABILITY)
        ) { Text(stringResource(R.string.booked_edit_availability)) }
    }
}

/**
 * Bordered rounded card.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp)
    ) { content() }
}

/**
 * "Saturday, May 15" in the phone's language.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
private fun fullDayLabel(key: Long): String {
    val m = monthOf(key)
    return SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(GregorianCalendar(m.year, m.month - 1, dayOfMonth(key)).time)
}

/**
 * Test tags for Booked dates.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
object BookedTags {
    const val SCREEN = "booked_screen"
    const val MONTH = "booked_month"
    const val PREV = "booked_prev"
    const val NEXT = "booked_next"
    const val SUMMARY = "booked_summary"
    const val SELECTED = "booked_selected"
    const val VIEW_RESERVATION = "booked_view_reservation"
    const val EMPTY = "booked_empty"
    const val EDIT_AVAILABILITY = "booked_edit_availability"

    /** Tag for one day cell. */
    fun day(key: Long) = "booked_day_$key"
}

/** Preview: May 2027 weekends open, May 15–16 booked, as in Figma. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun BookedDatesPreview() {
    val site = Campsite(
        id = "c1", name = "Grande Cache Ridge",
        availability = listOf(DateRange(dayKeyToTimestamp(dayKey(2027, 5, 1)), dayKeyToTimestamp(dayKey(2027, 5, 2))))
    )
    val r = Reservation(id = "qr1042", userId = "u1", campsiteId = "c1", partySize = 4, totalPrice = 130.0,
        startDate = dayKeyToTimestamp(dayKey(2027, 5, 15)), endDate = dayKeyToTimestamp(dayKey(2027, 5, 16)))
    TrailsTheme {
        BookedDatesContent(
            state = BookedDatesUiState(
                isLoading = false, campsites = listOf(site), campsite = site, month = CalendarMonth(2027, 5),
                available = availabilityToDays(site.availability), booked = bookedDayKeys(listOf(r)),
                reservations = listOf(r), guestNames = mapOf("u1" to "Alex Morgan")
            ),
            onBack = {}, onCampsiteChange = {}, onMonthChange = {}, onDayClick = {},
            onEditAvailability = {}, onViewReservation = {}
        )
    }
}
