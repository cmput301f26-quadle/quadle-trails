// Handles profile editing and deletion on the Profile screen (US 01.07.03, 01.08.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: "Change photo" is shown but disabled because Firebase Storage is not enabled.
package com.example.trails.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.discovery.DiscoveryRoutes
import com.example.trails.ui.navigation.goToHomeFor
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.Forest
import com.example.trails.ui.theme.Mist
import com.example.trails.ui.theme.Moss
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects the profile screen to its ViewModel and navigation. Shows Welcome when the
 * device has no profile, and Profile deleted after a deletion.
 *
 * @param navController used to go back, to the device profile, and to Browse.
 * @param modifier modifier for the screen.
 * @param viewModel the profile controller.
 */
@Composable
fun EditProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    val form by viewModel.form.collectAsState()
    val errors by viewModel.errors.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    val loaded = (state as? ProfileUiState.Loaded)?.user
    LaunchedEffect(loaded?.deviceId) {
        if (loaded != null) viewModel.startEditing(loaded)
    }

    when (val current = state) {
        ProfileUiState.Loading -> ProfileLoadingContent(modifier)
        is ProfileUiState.Error -> ProfileErrorContent(viewModel::retry, modifier)
        ProfileUiState.NoProfile -> WelcomeContent(
            deviceIdLabel = viewModel.deviceIdLabel,
            onContinue = {
                viewModel.ensureProfileExists()
                navController.navigate(DiscoveryRoutes.BROWSE)
            },
            modifier = modifier
        )
        ProfileUiState.Deleted -> ProfileDeletedContent(
            onBrowse = {
                viewModel.onStartOver()
                navController.goToHomeFor(Role.ENTRANT)
            },
            onSetUpNew = viewModel::onStartOver,
            modifier = modifier
        )
        is ProfileUiState.Loaded -> EditProfileContent(
            user = current.user,
            form = form,
            errors = errors,
            saveState = saveState,
            deleteState = deleteState,
            onBack = { navController.popBackStack() },
            onNameChange = viewModel::onNameChange,
            onEmailChange = viewModel::onEmailChange,
            onPhoneChange = viewModel::onPhoneChange,
            onSave = viewModel::onSaveClicked,
            onDeleteConfirmed = viewModel::onDeleteConfirmed,
            onOpenDeviceProfile = { navController.navigate(ProfileRoutes.DEVICE_PROFILE) },
            topBarActions = {
                ModePill(navController, viewModel = viewModel)
                Spacer(Modifier.width(12.dp))
            },
            modifier = modifier
        )
    }
}

/**
 * Stateless profile UI, so previews and tests can supply any values.
 *
 * @param user the profile being edited, for its photo.
 * @param form the values typed in.
 * @param errors validation problems to show under the fields.
 * @param saveState progress of saving.
 * @param deleteState progress of deleting.
 * @param onBack called from the back arrow.
 * @param onNameChange called as the name is typed.
 * @param onEmailChange called as the email is typed.
 * @param onPhoneChange called as the phone is typed.
 * @param onSave called from "Save Changes".
 * @param onDeleteConfirmed called when deletion is confirmed in the dialog.
 * @param onOpenDeviceProfile called from the "Signed in with this device" card.
 * @param modifier modifier for the screen.
 * @param topBarActions shown at the right of the top bar; the mode pill in the app.
 */
@Composable
fun EditProfileContent(
    user: User,
    form: ProfileForm,
    errors: ProfileFormErrors,
    saveState: SaveState,
    deleteState: DeleteState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onOpenDeviceProfile: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable RowScope.() -> Unit = {}
) {
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val saving = saveState == SaveState.Saving
    val deleting = deleteState == DeleteState.Deleting

    Column(modifier.fillMaxSize()) {
        TrailsTopBar(
            title = stringResource(R.string.profile_title),
            onBack = onBack,
            backTestTag = EditProfileTags.BACK,
            actions = topBarActions
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(EditProfileTags.SCREEN),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(96.dp)
                        .border(2.dp, TrailGreen, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Avatar(user.profileImageUrl, size = 86.dp)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.profile_change_photo),
                    style = MaterialTheme.typography.labelMedium,
                    color = Mist.copy(alpha = 0.6f),
                    modifier = Modifier.testTag(EditProfileTags.CHANGE_PHOTO)
                )
            }

            ProfileField(stringResource(R.string.profile_name_label), form.name, errors.name, onNameChange, EditProfileTags.NAME)
            ProfileField(
                stringResource(R.string.profile_email_label), form.email, errors.email, onEmailChange,
                EditProfileTags.EMAIL, KeyboardType.Email
            )
            ProfileField(
                stringResource(R.string.profile_phone_label), form.phone, errors.phone, onPhoneChange,
                EditProfileTags.PHONE, KeyboardType.Phone
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Forest)
                    .border(1.dp, Moss, RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenDeviceProfile)
                    .padding(12.dp)
                    .testTag(EditProfileTags.DEVICE_CARD),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.PhoneAndroid, contentDescription = null, tint = TrailGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.profile_signed_in_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = Mist,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Mist)
            }

            when (saveState) {
                SaveState.Saved -> StatusText(stringResource(R.string.profile_saved), EditProfileTags.SAVED, TrailGreen)
                is SaveState.Failed -> StatusText(stringResource(R.string.profile_error_save_failed), EditProfileTags.SAVE_ERROR, Ember)
                else -> Unit
            }
            if (deleteState is DeleteState.Failed) {
                StatusText(stringResource(R.string.profile_error_delete), EditProfileTags.DELETE_ERROR, Ember)
            }

            Button(
                onClick = onSave,
                enabled = !saving && !deleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag(EditProfileTags.SAVE)
            ) {
                Text(stringResource(if (saving) R.string.profile_saving else R.string.profile_save_changes))
            }
            Button(
                onClick = { confirmingDelete = true },
                enabled = !saving && !deleting,
                colors = ButtonDefaults.buttonColors(containerColor = Ember),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag(EditProfileTags.DELETE)
            ) {
                Text(stringResource(if (deleting) R.string.profile_deleting else R.string.profile_delete))
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            containerColor = NightSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Forest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = TrailGreen, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.profile_delete_dialog_title), style = MaterialTheme.typography.titleMedium)
                }
            },
            text = { Text(stringResource(R.string.profile_delete_dialog_body), color = Mist) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmingDelete = false
                        onDeleteConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ember),
                    modifier = Modifier.testTag(EditProfileTags.DIALOG_CONFIRM)
                ) {
                    Text(stringResource(R.string.profile_delete))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { confirmingDelete = false },
                    modifier = Modifier.testTag(EditProfileTags.DIALOG_CANCEL)
                ) {
                    Text(stringResource(R.string.profile_delete_cancel))
                }
            },
            modifier = Modifier
                .border(1.dp, TrailGreen.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                .testTag(EditProfileTags.DIALOG)
        )
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    error: ProfileFieldError?,
    onChange: (String) -> Unit,
    tag: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Mist)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error.message(), modifier = Modifier.testTag("${tag}_error")) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
    }
}

@Composable
private fun StatusText(text: String, tag: String, color: Color) {
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag(tag))
}

/** The message shown for a validation problem. */
@Composable
private fun ProfileFieldError.message(): String = stringResource(
    when (this) {
        ProfileFieldError.NAME_REQUIRED -> R.string.profile_error_name_required
        ProfileFieldError.EMAIL_REQUIRED -> R.string.profile_error_email_required
        ProfileFieldError.EMAIL_INVALID -> R.string.profile_error_email_invalid
        ProfileFieldError.PHONE_INVALID -> R.string.profile_error_phone_invalid
    }
)

/** Test tags for the profile screen. */
object EditProfileTags {
    const val SCREEN = "edit_profile"
    const val BACK = "edit_profile_back"
    const val CHANGE_PHOTO = "edit_profile_change_photo"
    const val NAME = "edit_profile_name"
    const val EMAIL = "edit_profile_email"
    const val PHONE = "edit_profile_phone"
    const val DEVICE_CARD = "edit_profile_device_card"
    const val SAVE = "edit_profile_save"
    const val SAVED = "edit_profile_saved"
    const val SAVE_ERROR = "edit_profile_save_error"
    const val DELETE = "edit_profile_delete"
    const val DELETE_ERROR = "edit_profile_delete_error"
    const val DIALOG = "edit_profile_delete_dialog"
    const val DIALOG_CONFIRM = "edit_profile_delete_confirm"
    const val DIALOG_CANCEL = "edit_profile_delete_cancel"
}

/** Preview of the profile screen with sample values. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun EditProfilePreview() {
    val alex = User(deviceId = "d", name = "Alex Morgan", email = "alex.morgan@email.com", phone = "780-555-0142")
    TrailsTheme {
        EditProfileContent(
            user = alex, form = alex.toProfileForm(), errors = ProfileFormErrors(),
            saveState = SaveState.Idle, deleteState = DeleteState.Idle,
            onBack = {}, onNameChange = {}, onEmailChange = {}, onPhoneChange = {}, onSave = {},
            onDeleteConfirmed = {}, onOpenDeviceProfile = {},
            topBarActions = { ModePillChip(Role.ENTRANT, onClick = {}) }
        )
    }
}
