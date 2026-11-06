// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.01.04
// PURPOSE: Owner's view of a single campsite they host (Figma · Campsite Details - Owner).
// Outstanding issues: booked days aren't shown on the calendar (no reservations yet); the
//  "Published" chip is always shown (no published flag in the model); the QR code card is
//  US 02.02.01, final part.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.ui.lottery.LotteryRoutes
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects [OwnerCampsiteViewModel] to navigation and renders [OwnerCampsiteContent].
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
fun OwnerCampsiteScreen(
    navController: NavController,
    campsiteId: String,
    modifier: Modifier = Modifier,
    justPublished: Boolean = false,
    viewModel: OwnerCampsiteViewModel = viewModel { OwnerCampsiteViewModel(campsiteId) }
) {
    val state by viewModel.uiState.collectAsState()
    // rememberSaveable: once dismissed, the dialog doesn't come back after rotation.
    var showPublished by rememberSaveable { mutableStateOf(justPublished) }
    OwnerCampsiteContent(
        state = state,
        showPublishedDialog = showPublished,
        onDismissPublished = { showPublished = false },
        onBack = { navController.popBackStack() },
        onEdit = { navController.navigate(CampsiteRoutes.campsiteForm(campsiteId)) },
        onCreateLottery = { navController.navigate(LotteryRoutes.lotteryForm(campsiteId = campsiteId)) },
        modifier = modifier
    )
}

/**
 * Stateless campsite page, split out so previews and UI tests can drive it directly.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param state what to show.
 * @param showPublishedDialog true to show "Campsite published" on top.
 * @param onDismissPublished called when the dialog is closed.
 * @param onBack called when the back arrow is tapped.
 * @param onEdit called when "Edit Campsite" is tapped.
 * @param onCreateLottery called when "Create Lottery" is tapped.
 */
@Composable
fun OwnerCampsiteContent(
    state: OwnerCampsiteUiState,
    showPublishedDialog: Boolean,
    onDismissPublished: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCreateLottery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { FormHeader(stringResource(R.string.campsite_details_title), onBack) }
    ) { padding ->
        val inner = Modifier
            .padding(padding)
            .fillMaxSize()
        when (state) {
            OwnerCampsiteUiState.Loading ->
                Box(inner, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            OwnerCampsiteUiState.NotFound ->
                Box(inner, contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.campsite_form_not_found))
                }
            is OwnerCampsiteUiState.Loaded -> {
                CampsiteDetails(state, onEdit, onCreateLottery, inner)
                if (showPublishedDialog) PublishedDialog(state.campsite.name, onDismissPublished)
            }
        }
    }
}

/**
 * The scrolling details, top to bottom as in Figma.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CampsiteDetails(
    state: OwnerCampsiteUiState.Loaded,
    onEdit: () -> Unit,
    onCreateLottery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = state.campsite
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Banner(c.imageUrl)
        Spacer(Modifier.height(16.dp))
        Text(c.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            c.locationName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.campsite_status_published), highlighted = true)
            Chip(
                state.lotteryName?.let { stringResource(R.string.campsite_in_lottery, it) }
                    ?: stringResource(R.string.campsite_no_active_lottery),
                highlighted = state.lotteryName != null
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                c.maxOccupancy.toString(),
                stringResource(R.string.campsite_stat_max_guests),
                Modifier.weight(1f)
            )
            StatTile(
                stringResource(R.string.campsite_price, CampsiteFormFields.formatPrice(c.pricePerNight)),
                stringResource(R.string.campsite_stat_per_night),
                Modifier.weight(1f)
            )
            if (c.ratingCount > 0) {
                StatTile(
                    stringResource(R.string.campsite_stat_rating, formatRating(c.ratingAverage)),
                    pluralStringResource(R.plurals.campsite_rating_count, c.ratingCount, c.ratingCount),
                    Modifier.weight(1f)
                )
            } else {
                StatTile("–", stringResource(R.string.campsite_no_ratings), Modifier.weight(1f))
            }
        }

        Section(stringResource(R.string.campsite_section_description)) { Body(c.description) }

        Section(stringResource(R.string.campsite_section_availability)) {
            val days = remember(c.availability) { availabilityToDays(c.availability) }
            if (days.isEmpty()) Muted(stringResource(R.string.campsite_no_availability))
            else AvailabilityCalendar(days)
        }

        Section(stringResource(R.string.campsite_section_location)) {
            val coords = formatCoordinates(c.latitude, c.longitude)
            if (coords == null) {
                LocationBox(stringResource(R.string.campsite_no_map_pin))
            } else {
                val shape = RoundedCornerShape(12.dp)
                CampsiteMap(
                    latitude = c.latitude,
                    longitude = c.longitude,
                    interactive = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(shape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                )
                Spacer(Modifier.height(8.dp))
                Body(coords)
            }
        }

        Section(stringResource(R.string.campsite_section_tags)) {
            if (c.tags.isEmpty()) {
                Muted(stringResource(R.string.campsite_no_tags))
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    c.tags.forEach { Chip(it, highlighted = false) }
                }
            }
        }

        Section(stringResource(R.string.campsite_section_rules)) { Body(c.rules) }
        Section(stringResource(R.string.campsite_section_cancellation)) { Body(c.cancellationPolicy) }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onEdit,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(OwnerCampsiteTags.EDIT)
        ) { Text(stringResource(R.string.campsite_edit)) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onCreateLottery,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(OwnerCampsiteTags.CREATE_LOTTERY)
        ) { Text(stringResource(R.string.campsite_create_lottery)) }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Full-width promo photo with a green border, or a dark tile when there's none.
 *
 * Written in assistance with Claude AI by Anthropic.
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
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = mod)
    } else {
        Box(mod.background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

/**
 * A titled block, e.g. "Rules" with its text underneath.
 *
 * Written in assistance with Claude AI by Anthropic.
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
 * Normal body text.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * Greyed-out text for empty sections.
 *
 * Written in assistance with Claude AI by Anthropic.
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
 * Outlined pill: green when [highlighted], grey otherwise.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun Chip(text: String, highlighted: Boolean) {
    val colour = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = colour,
        modifier = Modifier
            .border(1.dp, colour, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/**
 * Value over label in a green-bordered tile, e.g. "8" over "Max guests".
 *
 * Written in assistance with Claude AI by Anthropic.
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
 * Box showing the pin's coordinates, or that there's no pin yet.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun LocationBox(text: String) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center
    ) { Body(text) }
}

/**
 * "Campsite published" confirmation, shown once right after registering.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun PublishedDialog(name: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.campsite_published_title)) },
        text = { Text(stringResource(R.string.campsite_published_body, name)) },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag(OwnerCampsiteTags.VIEW_BUTTON)) {
                Text(stringResource(R.string.campsite_view))
            }
        },
        modifier = Modifier.testTag(OwnerCampsiteTags.PUBLISHED_DIALOG)
    )
}

/**
 * Test tags used by UI tests to find elements on this screen.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
object OwnerCampsiteTags {
    const val EDIT = "owner_campsite_edit"
    const val CREATE_LOTTERY = "owner_campsite_create_lottery"
    const val PUBLISHED_DIALOG = "owner_campsite_published_dialog"
    const val VIEW_BUTTON = "owner_campsite_view_button"
}

/**
 * Design-time preview with the Hinton sample campsite.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun OwnerCampsitePreview() {
    TrailsTheme {
        OwnerCampsiteContent(
            state = OwnerCampsiteUiState.Loaded(
                TestCampsites.all.first { it.id == TestCampsites.HINTON_ID },
                lotteryName = null
            ),
            showPublishedDialog = false,
            onDismissPublished = {}, onBack = {}, onEdit = {}, onCreateLottery = {}
        )
    }
}
