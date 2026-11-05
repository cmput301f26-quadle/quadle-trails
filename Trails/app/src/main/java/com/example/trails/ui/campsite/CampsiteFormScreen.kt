// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.01.01, 02.01.02 (tags), 02.01.03
// PURPOSE: Register a campsite, or edit one when campsiteId is set (Figma · Register Campsite).
// Outstanding issues: none. Figma has one "Rules & cancellation policy" box; the model has two
//  fields, so the form has two boxes.
package com.example.trails.ui.campsite

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.ui.theme.TrailsTheme

/** Connects [CampsiteFormViewModel] to navigation and renders [CampsiteFormContent]. */
@Composable
fun CampsiteFormScreen(
    navController: NavController,
    campsiteId: String?,
    modifier: Modifier = Modifier,
    viewModel: CampsiteFormViewModel = viewModel {
        CampsiteFormViewModel(AppContainer.campsiteRepository, CurrentUser.id(), campsiteId)
    }
) {
    val state by viewModel.state.collectAsState()

    // After saving: a new campsite opens its page (replacing the form); an edit goes back.
    LaunchedEffect(state.savedId) {
        val id = state.savedId ?: return@LaunchedEffect
        if (state.isEditing) {
            navController.popBackStack()
        } else {
            navController.navigate(CampsiteRoutes.ownerCampsite(id, published = true)) {
                popUpTo(CampsiteRoutes.CAMPSITE_FORM) { inclusive = true }
            }
        }
    }

    CampsiteFormContent(
        state = state,
        onFieldsChange = viewModel::onFieldsChange,
        onSave = viewModel::onSave,
        onBack = { navController.popBackStack() },
        modifier = modifier
    )
}

/**
 * Stateless form UI, split out so UI tests can drive it directly.
 *
 * @param state what to show.
 * @param onFieldsChange called with the updated fields whenever the owner types or edits tags.
 * @param onSave called when Publish / Save changes is tapped.
 * @param onBack called when the back arrow is tapped.
 */
@Composable
fun CampsiteFormContent(
    state: CampsiteFormUiState,
    onFieldsChange: (CampsiteFormFields) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            FormHeader(
                stringResource(if (state.isEditing) R.string.campsite_form_edit_title else R.string.campsite_form_register_title),
                onBack
            )
        },
        bottomBar = {
            Button(
                onClick = onSave,
                enabled = !state.isSaving && !state.isLoading && !state.notFound,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(48.dp)
                    .testTag(FormTags.SAVE)
            ) {
                Text(
                    stringResource(
                        when {
                            state.isSaving -> R.string.campsite_form_saving
                            state.isEditing -> R.string.campsite_form_save_changes
                            else -> R.string.campsite_form_publish
                        }
                    )
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.notFound -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.campsite_form_not_found))
            }
            else -> FormFields(state, onFieldsChange, Modifier.padding(padding))
        }
    }
}

/** Back arrow and title. */
@Composable
internal fun FormHeader(title: String, onBack: () -> Unit) {
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
                .testTag(FormTags.BACK)
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

/** The scrolling list of inputs. */
@Composable
private fun FormFields(
    state: CampsiteFormUiState,
    onFieldsChange: (CampsiteFormFields) -> Unit,
    modifier: Modifier = Modifier
) {
    val f = state.fields
    val e = state.errors
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        if (state.saveFailed) {
            Text(stringResource(R.string.campsite_form_save_failed), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
        }
        val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onFieldsChange(f.copy(imageUrl = uri.toString()))
        }
        PhotoPicker(
            imageUrl = f.imageUrl,
            onPick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemove = { onFieldsChange(f.copy(imageUrl = null)) }
        )
        LabeledField(stringResource(R.string.campsite_form_name_label), f.name, { onFieldsChange(f.copy(name = it)) },
            stringResource(R.string.campsite_form_name_hint), e["name"], FormTags.NAME)
        LabeledField(stringResource(R.string.campsite_form_location_label), f.locationName, { onFieldsChange(f.copy(locationName = it)) },
            stringResource(R.string.campsite_form_location_hint), e["locationName"], FormTags.LOCATION)
        LabeledField(stringResource(R.string.campsite_form_description_label), f.description, { onFieldsChange(f.copy(description = it)) },
            stringResource(R.string.campsite_form_description_hint), e["description"], FormTags.DESCRIPTION, minLines = 3)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField(stringResource(R.string.campsite_form_occupancy_label), f.maxOccupancy, { onFieldsChange(f.copy(maxOccupancy = it)) },
                stringResource(R.string.campsite_form_occupancy_hint), e["maxOccupancy"], FormTags.OCCUPANCY, Modifier.weight(1f), KeyboardType.Number)
            LabeledField(stringResource(R.string.campsite_form_price_label), f.pricePerNight, { onFieldsChange(f.copy(pricePerNight = it)) },
                stringResource(R.string.campsite_form_price_hint), e["pricePerNight"], FormTags.PRICE, Modifier.weight(1f), KeyboardType.Decimal)
        }
        AvailabilityField(f.availableDays, e["availability"]) { key ->
            onFieldsChange(f.copy(availableDays = toggleDay(f.availableDays, key)))
        }
        var pickingPin by rememberSaveable { mutableStateOf(false) }
        MapPinField(f.latitude, f.longitude, e["location"], onClick = { pickingPin = true })
        if (pickingPin) {
            MapPinPickerDialog(
                initialLatitude = f.latitude,
                initialLongitude = f.longitude,
                onConfirm = { lat, lng ->
                    onFieldsChange(f.copy(latitude = lat, longitude = lng))
                    pickingPin = false
                },
                onDismiss = { pickingPin = false }
            )
        }
        TagsSection(f.tags) { onFieldsChange(f.copy(tags = it)) }
        LabeledField(stringResource(R.string.campsite_form_rules_label), f.rules, { onFieldsChange(f.copy(rules = it)) },
            stringResource(R.string.campsite_form_rules_hint), e["rules"], FormTags.RULES, minLines = 2)
        LabeledField(stringResource(R.string.campsite_form_cancellation_label), f.cancellationPolicy, { onFieldsChange(f.copy(cancellationPolicy = it)) },
            stringResource(R.string.campsite_form_cancellation_hint), e["cancellationPolicy"],
            FormTags.CANCELLATION, minLines = 2)
    }
}

/** Dashed "Add promo photo" box, or the chosen photo with Change and Remove (US 02.01.02). */
@Composable
private fun PhotoPicker(imageUrl: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val green = MaterialTheme.colorScheme.primary
    Column(Modifier.padding(bottom = 12.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(shape)
                .drawBehind {
                    drawRoundRect(
                        color = green,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f))),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                }
                .clickable(onClick = onPick)
                .testTag(FormTags.PHOTO),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.campsite_form_photo_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(stringResource(R.string.campsite_form_add_photo), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (imageUrl != null) {
            Row {
                TextButton(onClick = onPick) { Text(stringResource(R.string.campsite_form_change_photo)) }
                TextButton(onClick = onRemove, modifier = Modifier.testTag(FormTags.REMOVE_PHOTO)) {
                    Text(stringResource(R.string.campsite_form_remove_photo))
                }
            }
        }
    }
}

/** "Availability *" label, editable calendar, and the error if no day is picked. */
@Composable
private fun AvailabilityField(days: Set<Long>, error: String?, onToggleDay: (Long) -> Unit) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(stringResource(R.string.campsite_form_availability_label), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        AvailabilityCalendar(
            availableDays = days,
            onToggleDay = onToggleDay,
            hint = stringResource(R.string.campsite_calendar_hint)
        )
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/** "Map location *" box: shows the pin's coordinates, or prompts to drop one. Tapping opens the picker. */
@Composable
private fun MapPinField(latitude: Double?, longitude: Double?, error: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val borderColour = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    val coords = if (latitude != null && longitude != null) formatCoordinates(latitude, longitude) else null
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(stringResource(R.string.campsite_form_map_label), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(shape)
                .border(1.dp, borderColour, shape)
                .clickable(onClick = onClick)
                .testTag(FormTags.MAP_PIN),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📍", style = MaterialTheme.typography.titleLarge)
                Text(
                    coords ?: stringResource(R.string.campsite_form_map_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (coords != null) {
                    Text(
                        stringResource(R.string.campsite_form_map_change),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/** A label above an outlined text field, with the error underneath if there is one. */
@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    tag: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    Column(modifier.padding(bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = minLines == 1,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
    }
}

/** Tag chips with remove buttons, and "+ Add tag" which opens an inline input (US 02.01.02). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(tags: List<String>, onTagsChange: (List<String>) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val confirm = {
        onTagsChange(addTag(tags, draft))
        draft = ""
        adding = false
    }

    Column(Modifier.padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.campsite_form_tags_label), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { adding = true }, modifier = Modifier.testTag(FormTags.ADD_TAG)) {
                Text(stringResource(R.string.campsite_form_add_tag))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag ->
                TagChip(tag, onRemove = { onTagsChange(tags - tag) })
            }
        }
        if (adding) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.campsite_form_tag_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(FormTags.TAG_INPUT)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = confirm, modifier = Modifier.testTag(FormTags.TAG_CONFIRM)) {
                    Text(stringResource(R.string.campsite_form_tag_confirm))
                }
            }
        }
    }
}

/** One tag with a × to remove it. */
@Composable
private fun TagChip(tag: String, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tag, style = MaterialTheme.typography.labelLarge)
        Text(
            "×",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(horizontal = 8.dp)
                .testTag(FormTags.removeTag(tag))
        )
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object FormTags {
    const val BACK = "form_back"
    const val PHOTO = "form_photo"
    const val REMOVE_PHOTO = "form_remove_photo"
    const val MAP_PIN = "form_map_pin"
    const val NAME = "form_name"
    const val LOCATION = "form_location"
    const val DESCRIPTION = "form_description"
    const val OCCUPANCY = "form_occupancy"
    const val PRICE = "form_price"
    const val RULES = "form_rules"
    const val CANCELLATION = "form_cancellation"
    const val ADD_TAG = "form_add_tag"
    const val TAG_INPUT = "form_tag_input"
    const val TAG_CONFIRM = "form_tag_confirm"
    const val SAVE = "form_save"

    /** Tag for the × on a given campsite tag chip. */
    fun removeTag(tag: String) = "form_remove_tag_$tag"
}

/** Design-time preview of the empty Register form. */
@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun CampsiteFormPreview() {
    TrailsTheme {
        CampsiteFormContent(
            state = CampsiteFormUiState(
                fields = CampsiteFormFields(tags = listOf("Lakeside", "Cookhouse", "Tennis")),
                errors = mapOf("name" to "Name is required")
            ),
            onFieldsChange = {}, onSave = {}, onBack = {}
        )
    }
}
