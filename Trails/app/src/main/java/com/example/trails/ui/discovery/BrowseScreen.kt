// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.01, 01.01.02, 01.01.03, 01.01.04
// PURPOSE: Scrollable campsite list with keyword search and filters.
// Outstanding issues: the title text must stay "Browse" because TrailsAppTest looks for it.
package com.example.trails.ui.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Entrant home screen: a scrollable list of campsite cards with search and filters.
 *
 * The title text is "Browse" so the app-shell test can find this screen.
 * Tapping a card opens that campsite's details.
 *
 * @param navController used to open a campsite's details.
 * @param modifier applied to the screen.
 * @param viewModel supplies the list and records what the user types.
 */
@Composable
fun BrowseScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = viewModel { BrowseViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.discovery_browse_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
        )
        OutlinedTextField(
            value = state.filters.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text(stringResource(R.string.discovery_search_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(BrowseTags.SEARCH)
        )
        OutlinedTextField(
            value = state.filters.location,
            onValueChange = viewModel::onLocationChange,
            label = { Text(stringResource(R.string.discovery_filter_location)) },
            placeholder = { Text(stringResource(R.string.discovery_filter_location_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(BrowseTags.LOCATION)
        )
        OutlinedTextField(
            value = state.filters.guestsText,
            onValueChange = viewModel::onGuestsChange,
            label = { Text(stringResource(R.string.discovery_filter_guests)) },
            placeholder = { Text(stringResource(R.string.discovery_filter_guests_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(BrowseTags.GUESTS)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateField(
                label = stringResource(R.string.discovery_filter_arrive),
                millis = state.filters.arriveMillis,
                onPick = viewModel::onArriveChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag(BrowseTags.ARRIVE)
            )
            DateField(
                label = stringResource(R.string.discovery_filter_leave),
                millis = state.filters.leaveMillis,
                onPick = viewModel::onLeaveChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag(BrowseTags.LEAVE)
            )
        }
        if (state.filters.arriveMillis != null || state.filters.leaveMillis != null) {
            TextButton(onClick = viewModel::onClearDates) {
                Text(stringResource(R.string.discovery_filter_clear_dates))
            }
        }
        if (state.shown.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(
                        if (state.totalCount == 0) R.string.discovery_no_campsites
                        else R.string.discovery_no_match
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp)
                    .testTag(BrowseTags.LIST),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(state.shown, key = { it.id }) { campsite ->
                    CampsiteSummaryCard(
                        campsite = campsite,
                        onClick = { navController.navigate(DiscoveryRoutes.campsiteDetails(campsite.id)) }
                    )
                }
            }
        }
    }
}

/**
 * A read-only field that opens a date picker.
 *
 * @param label text shown above the field.
 * @param millis the currently picked day, or null when the field is empty. UTC midnight.
 * @param onPick called with the confirmed day, or null when the picker has no selection.
 * @param modifier applied to the field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    millis: Long?,
    onPick: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val text = millis?.let(::formatBrowseDate).orEmpty()
    Box(modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable { open = true }
        )
    }
    if (open) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onPick(pickerState.selectedDateMillis)
                    open = false
                }) { Text(stringResource(R.string.discovery_date_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text(stringResource(R.string.discovery_date_cancel))
                }
            }
        ) { DatePicker(state = pickerState) }
    }
}

/**
 * Formats a date-picker value for the Arrive and Leave fields.
 *
 * The picker stores UTC midnight, so the calendar day is formatted in UTC.
 *
 * @param millis UTC midnight of the picked day, in milliseconds.
 * @return the date as text, for example "May 8, 2027".
 */
fun formatBrowseDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(millis))

/**
 * Holds the browse filters and the campsite list that matches them.
 *
 * The list updates whenever the repository changes or the user edits a filter.
 *
 * @param campsiteRepository where the campsites are loaded from.
 */
class BrowseViewModel(
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository
) : ViewModel() {
    private val filters = MutableStateFlow(BrowseFilters())

    /** The list, filters, and total campsite count currently shown. */
    val uiState: StateFlow<BrowseUiState> = combine(
        campsiteRepository.observeAll(),
        filters
    ) { campsites, current ->
        BrowseUiState(buildBrowseList(campsites, current), current, campsites.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState())

    /**
     * Sets the keyword search.
     *
     * @param value the text in the search field.
     */
    fun onQueryChange(value: String) = filters.update { it.copy(query = value) }

    /**
     * Sets the location filter.
     *
     * @param value the text in the location field.
     */
    fun onLocationChange(value: String) = filters.update { it.copy(location = value) }

    /**
     * Sets the guest filter, keeping digits only.
     *
     * @param value the text in the guests field.
     */
    fun onGuestsChange(value: String) = filters.update { it.copy(guestsText = value.filter { ch -> ch.isDigit() }) }

    /**
     * Sets the arrival date.
     *
     * @param millis the picked day, or null to clear it. UTC midnight from the date picker.
     */
    fun onArriveChange(millis: Long?) = filters.update { it.copy(arriveMillis = millis) }

    /**
     * Sets the leaving date.
     *
     * @param millis the picked day, or null to clear it. UTC midnight from the date picker.
     */
    fun onLeaveChange(millis: Long?) = filters.update { it.copy(leaveMillis = millis) }

    /** Clears both the arrival and leaving dates. */
    fun onClearDates() = filters.update { it.copy(arriveMillis = null, leaveMillis = null) }
}

/**
 * Test tags for the browse screen.
 *
 * @property LIST the scrollable campsite list.
 * @property SEARCH the keyword field.
 * @property LOCATION the location filter.
 * @property GUESTS the guest filter.
 * @property ARRIVE the arrival date field.
 * @property LEAVE the leaving date field.
 */
object BrowseTags {
    const val LIST = "discovery_list"
    const val SEARCH = "discovery_search"
    const val LOCATION = "discovery_location"
    const val GUESTS = "discovery_guests"
    const val ARRIVE = "discovery_arrive"
    const val LEAVE = "discovery_leave"
}
