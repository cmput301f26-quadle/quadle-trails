// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.01, 01.01.02, 01.01.03, 01.01.04
// PURPOSE: Search, a filters page, the filtered list, and a summary preview.
// Outstanding issues: the search title must stay "Browse" because TrailsAppTest looks for it.
package com.example.trails.ui.discovery

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.AppContainer
import com.example.trails.data.model.Campsite
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.ui.common.TrailsTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class BrowsePane { Search, Filters, Results, Preview }

/**
 * Entrant home: keyword search, then a filters page, then the filtered list.
 *
 * The search title is "Browse" so the app-shell test can find this screen.
 * Location, guests, and dates change the list only after Apply filters.
 * Tapping a campsite opens a summary, and View details opens the campsite.
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
    val draft by viewModel.draftFilters.collectAsState()
    var pane by rememberSaveable { mutableStateOf(BrowsePane.Search) }
    var forceSearch by rememberSaveable { mutableStateOf(false) }
    var previewId by rememberSaveable { mutableStateOf<String?>(null) }
    var previewFrom by rememberSaveable { mutableStateOf(BrowsePane.Search) }
    var calendarKey by rememberSaveable { mutableIntStateOf(0) }
    var calendarStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var calendarEnd by rememberSaveable { mutableStateOf<Long?>(null) }
    val hasDetails = state.filters.hasDetailFilters()

    LaunchedEffect(hasDetails, forceSearch, pane) {
        if (hasDetails && !forceSearch && pane == BrowsePane.Search) {
            pane = BrowsePane.Results
        } else if (!hasDetails && pane == BrowsePane.Results) {
            pane = BrowsePane.Search
        }
    }

    val preview = state.shown.firstOrNull { it.id == previewId }
    LaunchedEffect(pane, preview) {
        if (pane == BrowsePane.Preview && preview == null) pane = previewFrom
    }

    fun openFilters() {
        viewModel.beginFilters()
        calendarStart = state.filters.arriveMillis
        calendarEnd = state.filters.leaveMillis
        calendarKey++
        pane = BrowsePane.Filters
    }

    fun leaveFilters() {
        pane = if (hasDetails && !forceSearch) BrowsePane.Results else BrowsePane.Search
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            when (pane) {
                BrowsePane.Search -> TrailsTopBar(
                    title = stringResource(R.string.discovery_browse_title),
                    subtitle = stringResource(R.string.discovery_search_hint)
                )
                BrowsePane.Filters -> TrailsTopBar(
                    title = stringResource(R.string.discovery_filters_title),
                    onBack = ::leaveFilters,
                    useChevron = true
                )
                BrowsePane.Results -> TrailsTopBar(
                    title = stringResource(R.string.discovery_search_results),
                    onBack = {
                        forceSearch = true
                        pane = BrowsePane.Search
                    },
                    useChevron = true
                )
                BrowsePane.Preview -> TrailsTopBar(
                    title = preview?.name ?: stringResource(R.string.discovery_search_results),
                    onBack = { pane = previewFrom },
                    useChevron = true
                )
            }
        },
        bottomBar = {
            if (pane == BrowsePane.Preview && preview != null) {
                Button(
                    onClick = { navController.navigate(DiscoveryRoutes.campsiteDetails(preview.id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.discovery_view_details))
                }
            }
        }
    ) { padding ->
        when (pane) {
            BrowsePane.Search -> SearchPane(
                state = state,
                onQueryChange = viewModel::onQueryChange,
                onClearSearch = viewModel::onClearSearch,
                onOpenFilters = ::openFilters,
                onOpenCampsite = {
                    previewFrom = BrowsePane.Search
                    previewId = it.id
                    pane = BrowsePane.Preview
                },
                onSelectPlace = {
                    forceSearch = false
                    viewModel.onSelectPlace(it)
                    pane = BrowsePane.Results
                },
                modifier = Modifier.padding(padding)
            )
            BrowsePane.Filters -> FiltersPane(
                draft = draft,
                calendarKey = calendarKey,
                calendarStart = calendarStart,
                calendarEnd = calendarEnd,
                onLocation = viewModel::onDraftLocation,
                onGuests = viewModel::onDraftGuests,
                onArrive = viewModel::onDraftArrive,
                onLeave = viewModel::onDraftLeave,
                onApply = {
                    viewModel.onApplyFilters()
                    forceSearch = false
                    pane = BrowsePane.Results
                },
                onReset = {
                    viewModel.onResetDraft()
                    calendarStart = null
                    calendarEnd = null
                    calendarKey++
                },
                modifier = Modifier.padding(padding)
            )
            BrowsePane.Results -> ResultsPane(
                state = state,
                onEditFilters = ::openFilters,
                onResetAll = {
                    viewModel.onResetAll()
                    forceSearch = false
                    pane = BrowsePane.Search
                },
                onOpenCampsite = {
                    previewFrom = BrowsePane.Results
                    previewId = it.id
                    pane = BrowsePane.Preview
                },
                modifier = Modifier.padding(padding)
            )
            BrowsePane.Preview -> {
                if (preview != null) {
                    Column(
                        Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        CampsiteSummaryCard(campsite = preview, onClick = {})
                    }
                }
            }
        }
    }
}

/**
 * Keyword search and the campsites that match it.
 *
 * @param state the applied list.
 * @param onQueryChange called with the search text.
 * @param onClearSearch called by the clear control.
 * @param onOpenFilters called by the Filters button.
 * @param onOpenCampsite called when a card is tapped.
 * @param onSelectPlace called when a suggested place is tapped.
 * @param modifier applied to the pane.
 */
@Composable
private fun SearchPane(
    state: BrowseUiState,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenCampsite: (Campsite) -> Unit,
    onSelectPlace: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        OutlinedTextField(
            value = state.filters.query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.discovery_search_hint)) },
            trailingIcon = {
                if (state.filters.query.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.discovery_clear_search))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(BrowseTags.SEARCH)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onOpenFilters) {
                Text(stringResource(R.string.discovery_open_filters))
            }
            TextButton(onClick = onClearSearch) {
                Text(stringResource(R.string.discovery_clear_search))
            }
        }
        CampsiteResults(
            state = state,
            onOpenCampsite = onOpenCampsite,
            modifier = Modifier.weight(1f),
            header = {
                if (state.filters.query.isNotBlank()) {
                    Text(
                        pluralStringResource(
                            R.plurals.discovery_search_count,
                            state.shown.size,
                            state.shown.size,
                            state.filters.query.trim()
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            },
            footer = {
                if (state.filters.query.isNotBlank() && state.places.isNotEmpty()) {
                    Text(
                        stringResource(R.string.discovery_search_place),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                    state.places.forEach { place ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(place, style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = { onSelectPlace(place) }) {
                                Text(stringResource(R.string.discovery_see_area))
                            }
                        }
                    }
                }
            },
            empty = { Text(stringResource(R.string.discovery_no_match)) }
        )
    }
}

/**
 * Location, date, and guest filters. Nothing here changes the list until Apply.
 *
 * @param draft the filters being edited.
 * @param calendarKey recreates the calendar after reset.
 * @param calendarStart check-in to show when the calendar is recreated.
 * @param calendarEnd check-out to show when the calendar is recreated.
 * @param onLocation called with the location text.
 * @param onGuests called with the guest text.
 * @param onArrive called with the check-in day, or null.
 * @param onLeave called with the check-out day, or null.
 * @param onApply copies [draft] onto the list.
 * @param onReset clears [draft].
 * @param modifier applied to the pane.
 */
@Composable
private fun FiltersPane(
    draft: BrowseFilters,
    calendarKey: Int,
    calendarStart: Long?,
    calendarEnd: Long?,
    onLocation: (String) -> Unit,
    onGuests: (String) -> Unit,
    onArrive: (Long?) -> Unit,
    onLeave: (Long?) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = draft.location,
            onValueChange = onLocation,
            label = { Text(stringResource(R.string.discovery_filter_location)) },
            placeholder = { Text(stringResource(R.string.discovery_filter_location_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(BrowseTags.LOCATION)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateReadout(
                stringResource(R.string.discovery_filter_arrive),
                draft.arriveMillis,
                Modifier
                    .weight(1f)
                    .testTag(BrowseTags.ARRIVE)
            )
            DateReadout(
                stringResource(R.string.discovery_filter_leave),
                draft.leaveMillis,
                Modifier
                    .weight(1f)
                    .testTag(BrowseTags.LEAVE)
            )
        }
        key(calendarKey) {
            RangeCalendar(
                start = calendarStart,
                end = calendarEnd,
                onStart = onArrive,
                onEnd = onLeave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .padding(top = 8.dp)
            )
        }
        Text(
            stringResource(R.string.discovery_filter_guests),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                val next = (draft.guestsText.toIntOrNull() ?: 0) - 1
                onGuests(if (next < 1) "" else next.toString())
            }) { Text("−") }
            OutlinedTextField(
                value = draft.guestsText,
                onValueChange = onGuests,
                placeholder = { Text(stringResource(R.string.discovery_filter_guests_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag(BrowseTags.GUESTS)
            )
            TextButton(onClick = {
                onGuests(((draft.guestsText.toIntOrNull() ?: 0) + 1).toString())
            }) { Text("+") }
        }
        Text(
            stringResource(R.string.discovery_date_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.discovery_apply_filters))
        }
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            Text(stringResource(R.string.discovery_reset_filters))
        }
    }
}

/**
 * Campsites that match the applied filters, with chips for each active filter.
 *
 * @param state the applied list.
 * @param onEditFilters called by Edit filters.
 * @param onResetAll clears the keyword and every filter.
 * @param onOpenCampsite called when a card is tapped.
 * @param modifier applied to the pane.
 */
@Composable
private fun ResultsPane(
    state: BrowseUiState,
    onEditFilters: () -> Unit,
    onResetAll: () -> Unit,
    onOpenCampsite: (Campsite) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        CampsiteResults(
            state = state,
            onOpenCampsite = onOpenCampsite,
            modifier = Modifier.weight(1f),
            header = {
                FilterChips(state.filters)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        pluralStringResource(
                            R.plurals.discovery_matching_count,
                            state.shown.size,
                            state.shown.size
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                    TextButton(onClick = onEditFilters) {
                        Text(stringResource(R.string.discovery_edit_filters))
                    }
                }
            },
            empty = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.discovery_no_match), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.discovery_no_match_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                Button(
                    onClick = onResetAll,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.discovery_reset_all))
                }
            }
        )
    }
}

/**
 * The campsite list, or the empty message when nothing matches.
 *
 * @param state the applied list.
 * @param onOpenCampsite called when a card is tapped.
 * @param modifier applied to the list.
 * @param header drawn above the cards.
 * @param footer drawn below the cards.
 * @param empty drawn when campsites exist but none match.
 */
@Composable
private fun CampsiteResults(
    state: BrowseUiState,
    onOpenCampsite: (Campsite) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    empty: @Composable () -> Unit
) {
    if (state.totalCount == 0) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.discovery_no_campsites))
        }
        return
    }
    if (state.shown.isEmpty()) {
        Column(modifier.fillMaxSize()) {
            header()
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { empty() }
        }
        return
    }
    LazyColumn(
        modifier = modifier.testTag(BrowseTags.LIST),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item { header() }
        items(state.shown, key = { it.id }) { campsite ->
            CampsiteSummaryCard(campsite = campsite, onClick = { onOpenCampsite(campsite) })
        }
        item { footer() }
    }
}

/**
 * One chip for each applied location, date range, and guest count.
 *
 * @param filters the filters currently applied to the list.
 */
@Composable
private fun FilterChips(filters: BrowseFilters) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (filters.location.isNotBlank()) FilterChip(filters.location.trim())
        dateChipLabel(filters)?.let { FilterChip(it) }
        if (filters.guestsText.isNotBlank()) {
            FilterChip(stringResource(R.string.discovery_chip_guests, filters.guestsText))
        }
    }
}

private fun dateChipLabel(filters: BrowseFilters): String? {
    val arrive = filters.arriveMillis?.let(::formatBrowseDate)
    val leave = filters.leaveMillis?.let(::formatBrowseDate)
    return when {
        arrive != null && leave != null -> "$arrive – $leave"
        arrive != null -> arrive
        leave != null -> leave
        else -> null
    }
}

/**
 * A short label for one applied filter.
 *
 * @param text the filter value.
 */
@Composable
private fun FilterChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

/**
 * Check-in or check-out as text. The calendar below is what changes it.
 *
 * @param label Check-in or Check-out.
 * @param millis the picked day, or null when empty. UTC midnight.
 * @param modifier applied to the field.
 */
@Composable
private fun DateReadout(label: String, millis: Long?, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = millis?.let(::formatBrowseDate).orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    )
}

/**
 * The on-screen calendar for a check-in and check-out range.
 *
 * @param start check-in when this calendar is first shown.
 * @param end check-out when this calendar is first shown.
 * @param onStart called when check-in changes.
 * @param onEnd called when check-out changes.
 * @param modifier applied to the calendar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeCalendar(
    start: Long?,
    end: Long?,
    onStart: (Long?) -> Unit,
    onEnd: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = start,
        initialSelectedEndDateMillis = end
    )
    LaunchedEffect(state.selectedStartDateMillis, state.selectedEndDateMillis) {
        onStart(state.selectedStartDateMillis)
        onEnd(state.selectedEndDateMillis)
    }
    DateRangePicker(
        state = state,
        modifier = modifier,
        showModeToggle = false,
        title = {}
    )
}

/**
 * Formats a date-picker value for the check-in and check-out fields.
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

private fun BrowseFilters.hasDetailFilters(): Boolean =
    location.isNotBlank() || guestsText.isNotBlank() || arriveMillis != null || leaveMillis != null

/**
 * Holds the applied filters, the draft on the filters page, and the matching campsites.
 *
 * The keyword updates the list immediately. Location, guests, and dates update it
 * when [onApplyFilters] copies the draft onto the applied filters.
 *
 * @param campsiteRepository where the campsites are loaded from.
 */
class BrowseViewModel(
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository
) : ViewModel() {
    private val applied = MutableStateFlow(BrowseFilters())
    private val draft = MutableStateFlow(BrowseFilters())

    /** The list, the applied filters, and the total campsite count. */
    val uiState: StateFlow<BrowseUiState> = combine(
        campsiteRepository.observeAll(),
        applied
    ) { campsites, current ->
        val shown = buildBrowseList(campsites, current)
        BrowseUiState(
            shown = shown,
            filters = current,
            totalCount = campsites.size,
            places = shown.map { it.locationName }.filter { it.isNotBlank() }.distinct().sorted()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState())

    /** Location, guests, and dates being edited on the filters page. */
    val draftFilters: StateFlow<BrowseFilters> = draft.asStateFlow()

    /**
     * Sets the keyword search. The list updates immediately.
     *
     * @param value the text in the search field.
     */
    fun onQueryChange(value: String) = applied.update { it.copy(query = value) }

    /** Clears the keyword and leaves the other applied filters as they are. */
    fun onClearSearch() = applied.update { it.copy(query = "") }

    /** Copies the applied filters into the draft so the filters page starts from them. */
    fun beginFilters() {
        draft.value = applied.value
    }

    /**
     * Sets the draft location.
     *
     * @param value the text in the location field.
     */
    fun onDraftLocation(value: String) = draft.update { it.copy(location = value) }

    /**
     * Sets the draft guest count, keeping digits only.
     *
     * @param value the text in the guests field.
     */
    fun onDraftGuests(value: String) = draft.update { it.copy(guestsText = value.filter { ch -> ch.isDigit() }) }

    /**
     * Sets the draft check-in.
     *
     * @param millis the picked day, or null to clear it. UTC midnight.
     */
    fun onDraftArrive(millis: Long?) = draft.update { it.copy(arriveMillis = millis) }

    /**
     * Sets the draft check-out.
     *
     * @param millis the picked day, or null to clear it. UTC midnight.
     */
    fun onDraftLeave(millis: Long?) = draft.update { it.copy(leaveMillis = millis) }

    /** Copies the draft location, guests, and dates onto the list. The keyword stays. */
    fun onApplyFilters() {
        val editing = draft.value
        applied.update {
            it.copy(
                location = editing.location,
                guestsText = editing.guestsText,
                arriveMillis = editing.arriveMillis,
                leaveMillis = editing.leaveMillis
            )
        }
    }

    /** Clears the draft location, guests, and dates. */
    fun onResetDraft() = draft.update {
        it.copy(location = "", guestsText = "", arriveMillis = null, leaveMillis = null)
    }

    /** Clears the keyword and every filter, both applied and draft. */
    fun onResetAll() {
        applied.value = BrowseFilters()
        draft.value = BrowseFilters()
    }

    /**
     * Applies a suggested place as the location filter.
     *
     * @param place the location name to apply.
     */
    fun onSelectPlace(place: String) = applied.update { it.copy(location = place) }

    /**
     * Sets the applied check-in directly.
     *
     * @param millis the picked day, or null to clear it. UTC midnight.
     */
    fun onArriveChange(millis: Long?) = applied.update { it.copy(arriveMillis = millis) }

    /**
     * Sets the applied check-out directly.
     *
     * @param millis the picked day, or null to clear it. UTC midnight.
     */
    fun onLeaveChange(millis: Long?) = applied.update { it.copy(leaveMillis = millis) }
}

/**
 * Test tags for the browse screen.
 *
 * @property LIST the scrollable campsite list.
 * @property SEARCH the keyword field.
 * @property LOCATION the location filter.
 * @property GUESTS the guest filter.
 * @property ARRIVE the check-in field.
 * @property LEAVE the check-out field.
 */
object BrowseTags {
    const val LIST = "discovery_list"
    const val SEARCH = "discovery_search"
    const val LOCATION = "discovery_location"
    const val GUESTS = "discovery_guests"
    const val ARRIVE = "discovery_arrive"
    const val LEAVE = "discovery_leave"
}
