// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.05
// PURPOSE: Shows a campsite's details to an entrant.
// Outstanding issues: the Compose preview skips the live map because osmdroid has no storage there.
package com.example.trails.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.model.Campsite
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.ui.admin.RatingSection
import com.example.trails.ui.campsite.CampsiteFormFields
import com.example.trails.ui.campsite.CampsiteMap
import com.example.trails.ui.campsite.formatCoordinates
import com.example.trails.ui.campsite.formatDateRange
import com.example.trails.ui.campsite.formatRating
import com.example.trails.ui.theme.TrailsTheme
import com.example.trails.ui.waitlist.JoinLotterySection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Loads one campsite and shows its details to an entrant.
 *
 * The campsite comes from the repository in [AppContainer], so this keeps working when
 * Firestore is swapped in. The join section is shown only when the campsite has an
 * active lottery. The rating section below the details belongs to another owner.
 *
 * @param navController used to go back and to open the waitlist from the join section.
 * @param campsiteId the campsite to load.
 * @param modifier applied to the screen.
 * @param viewModel loads the campsite and exposes the page state.
 */
@Composable
fun CampsiteDetailsScreen(
    navController: NavController,
    campsiteId: String,
    modifier: Modifier = Modifier,
    viewModel: CampsiteDetailsViewModel = viewModel { CampsiteDetailsViewModel(campsiteId) }
) {
    val state by viewModel.uiState.collectAsState()
    CampsiteDetailsContent(
        state = state,
        campsiteId = campsiteId,
        onBack = { navController.popBackStack() },
        navController = navController,
        modifier = modifier
    )
}

/**
 * Watches one campsite and exposes the page state.
 *
 * The state starts as loading, then becomes loaded or not found as the repository reports.
 *
 * @param campsiteId the campsite to show.
 * @param campsiteRepository where campsites are stored.
 */
class CampsiteDetailsViewModel(
    campsiteId: String,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository
) : ViewModel() {

    /** Loading, not found, or the loaded campsite. */
    val uiState: StateFlow<CampsiteDetailsUiState> =
        campsiteRepository.observeById(campsiteId)
            .map(::buildCampsiteDetailsState)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CampsiteDetailsUiState.Loading
            )
}

/**
 * Stateless details page, split out so previews and UI tests can drive it directly.
 *
 * Shows a spinner while loading, a not-found message when the id is missing, and the
 * campsite body once it is loaded.
 *
 * @param state what to show.
 * @param campsiteId id passed through to the join and rating sections.
 * @param onBack called when the back arrow is tapped.
 * @param navController passed to the join section so it can open the waitlist.
 * @param modifier applied to the page.
 */
@Composable
fun CampsiteDetailsContent(
    state: CampsiteDetailsUiState,
    campsiteId: String,
    onBack: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { DetailsHeader(stringResource(R.string.discovery_details_title), onBack) }
    ) { padding ->
        val inner = Modifier
            .padding(padding)
            .fillMaxSize()
        when (state) {
            CampsiteDetailsUiState.Loading ->
                Box(inner.testTag(CampsiteDetailsTags.LOADING), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            CampsiteDetailsUiState.NotFound ->
                Box(inner, contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.discovery_details_not_found),
                        modifier = Modifier.testTag(CampsiteDetailsTags.NOT_FOUND)
                    )
                }
            is CampsiteDetailsUiState.Loaded ->
                DetailsBody(state.campsite, campsiteId, navController, inner)
        }
    }
}

/**
 * Back arrow and title.
 *
 * @param title text shown beside the arrow.
 * @param onBack called when the arrow is tapped.
 */
@Composable
private fun DetailsHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "‹",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(horizontal = 12.dp)
                .testTag(CampsiteDetailsTags.BACK)
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * Scrollable campsite details, followed by the rating section.
 *
 * The join section is included only when [Campsite.activeLotteryId] is set.
 * The live map is skipped in the Compose preview, where osmdroid has no storage.
 * On an emulator or device the map is shown.
 *
 * @param campsite the loaded campsite.
 * @param campsiteId id passed through to the join and rating sections.
 * @param navController passed to the join section so it can open the waitlist.
 * @param modifier applied to the scrollable column.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsBody(campsite: Campsite, campsiteId: String, navController: NavController, modifier: Modifier = Modifier) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag(CampsiteDetailsTags.CONTENT)
    ) {
        Banner(campsite.imageUrl)
        Spacer(Modifier.height(16.dp))
        Text(campsite.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            campsite.locationName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                campsite.maxOccupancy.toString(),
                stringResource(R.string.discovery_stat_capacity),
                Modifier.weight(1f)
            )
            StatTile(
                stringResource(R.string.discovery_price, CampsiteFormFields.formatPrice(campsite.pricePerNight)),
                stringResource(R.string.discovery_stat_per_night),
                Modifier.weight(1f)
            )
            if (campsite.ratingCount > 0) {
                StatTile(
                    stringResource(R.string.discovery_stat_rating, formatRating(campsite.ratingAverage)),
                    pluralStringResource(
                        R.plurals.discovery_rating_count,
                        campsite.ratingCount,
                        campsite.ratingCount
                    ),
                    Modifier.weight(1f)
                )
            } else {
                StatTile("–", stringResource(R.string.discovery_card_no_rating), Modifier.weight(1f))
            }
        }

        Section(stringResource(R.string.discovery_section_about)) { Body(campsite.description) }

        Section(stringResource(R.string.discovery_section_availability)) {
            val ranges = campsite.availability.mapNotNull { formatDateRange(it) }
            if (ranges.isEmpty()) Muted(stringResource(R.string.discovery_no_availability))
            else ranges.forEach { Body(it) }
        }

        Section(stringResource(R.string.discovery_section_location)) {
            val coords = formatCoordinates(campsite.latitude, campsite.longitude)
            if (coords == null) {
                Muted(stringResource(R.string.discovery_no_map_pin))
            } else {
                // Studio's preview has no app storage, so osmdroid crashes there.
                // On an emulator or device this is false and the real map is shown.
                if (!LocalInspectionMode.current) {
                    val shape = RoundedCornerShape(12.dp)
                    CampsiteMap(
                        latitude = campsite.latitude,
                        longitude = campsite.longitude,
                        interactive = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(shape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Body(coords)
            }
        }

        Section(stringResource(R.string.discovery_section_amenities)) {
            if (campsite.tags.isEmpty()) {
                Muted(stringResource(R.string.discovery_no_amenities))
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    campsite.tags.forEach { Chip(it) }
                }
            }
        }

        Section(stringResource(R.string.discovery_section_rules)) { Body(campsite.rules) }
        Section(stringResource(R.string.discovery_section_cancellation)) { Body(campsite.cancellationPolicy) }

        Spacer(Modifier.height(24.dp))
        // No lottery means there is nothing to join. The section itself also checks.
        if (!campsite.activeLotteryId.isNullOrBlank()) {
            JoinLotterySection(navController = navController, campsiteId = campsiteId)
        }
        Spacer(Modifier.height(16.dp))
        // Owned by Zainab. Leave this call as it is.
        RatingSection(campsiteId)
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Wide promo photo, or a plain tile when the campsite has none.
 *
 * @param url the photo address, or null when the campsite has no photo.
 */
@Composable
private fun Banner(url: String?) {
    val shape = RoundedCornerShape(12.dp)
    val mod = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .clip(shape)
        .border(1.dp, MaterialTheme.colorScheme.primary, shape)
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = stringResource(R.string.discovery_photo_desc),
            contentScale = ContentScale.Crop,
            modifier = mod
        )
    } else {
        Box(mod.background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

/**
 * A titled block on the details page.
 *
 * @param title heading for the block.
 * @param content body drawn under the heading.
 */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(top = 20.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

/**
 * Ordinary body text under a section heading.
 *
 * @param text the text to show.
 */
@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * Italic note used when a section has nothing to show.
 *
 * @param text the note to show.
 */
@Composable
private fun Muted(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
}

/**
 * One amenity label.
 *
 * @param text the amenity name.
 */
@Composable
private fun Chip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/**
 * One highlighted number, such as capacity, price, or rating.
 *
 * @param value the number or amount.
 * @param label what the number means.
 * @param modifier applied to the tile.
 */
@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary, shape)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Test tags for the details screen.
 *
 * @property BACK the back arrow.
 * @property LOADING the spinner shown while the campsite loads.
 * @property NOT_FOUND the message shown when the campsite id does not exist.
 * @property CONTENT the loaded campsite body.
 */
object CampsiteDetailsTags {
    const val BACK = "discovery_details_back"
    const val LOADING = "discovery_details_loading"
    const val NOT_FOUND = "discovery_details_not_found"
    const val CONTENT = "discovery_details_content"
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun CampsiteDetailsPreview() {
    TrailsTheme {
        CampsiteDetailsContent(
            state = CampsiteDetailsUiState.Loaded(
                TestCampsites.all.first { it.id == TestCampsites.BANFF_ID }
            ),
            campsiteId = TestCampsites.BANFF_ID,
            onBack = {},
            navController = rememberNavController()
        )
    }
}
