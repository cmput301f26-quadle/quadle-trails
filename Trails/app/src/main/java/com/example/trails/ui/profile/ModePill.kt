// Shows the mode pill and its "Use Quadle as" sheet for switching roles (US 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: Admin home (Zainab's screen) does not show the pill yet.
package com.example.trails.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.model.Role
import com.example.trails.ui.navigation.goToHomeFor
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import androidx.compose.ui.semantics.Role as SemanticsRole

/** Colour of the Admin pill and option, as in the Scenario 8 storyboard. */
val AdminAmber = Color(0xFFF2C14E)

/** Colour of the Camper pill and option, as in the Scenario 1 storyboard. */
val CamperMint = Color(0xFF7DD3C0)

/**
 * The pill in the top bar showing the mode the user is acting as. Tapping it opens
 * the "Use Quadle as" sheet; after a switch the app goes to that mode's home.
 * Nothing is shown while the profile is loading or missing. Any screen can show it
 * with `TrailsTopBar(..., actions = { ModePill(navController) })`.
 *
 * @param navController used to go to the new mode's home; null before the app's nav graph exists.
 * @param modifier modifier for the pill.
 * @param viewModel the profile controller; shared with the screen when both use the default.
 */
@Composable
fun ModePill(
    navController: NavController?,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    val modeSwitch by viewModel.modeSwitch.collectAsState()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(modeSwitch) {
        val switched = modeSwitch as? ModeSwitchState.Switched ?: return@LaunchedEffect
        sheetOpen = false
        viewModel.onModeSwitchHandled()
        navController?.goToHomeFor(switched.mode)
    }

    val user = (state as? ProfileUiState.Loaded)?.user
    if (user != null) {
        ModePillChip(mode = effectiveMode(user), onClick = { sheetOpen = true }, modifier = modifier)
        if (sheetOpen) {
            val openDeviceProfile: (() -> Unit)? = if (navController != null) {
                {
                    sheetOpen = false
                    navController.navigate(ProfileRoutes.DEVICE_PROFILE)
                }
            } else {
                null
            }
            ChooseModeSheet(
                onDismiss = {
                    sheetOpen = false
                    viewModel.onModeSwitchHandled()
                }
            ) {
                ChooseModeContent(
                    user = user,
                    modeSwitch = modeSwitch,
                    onSwitch = viewModel::onModeSelected,
                    onProfile = openDeviceProfile
                )
            }
        }
    }
}

/**
 * The pill itself: a coloured dot, the mode's name and a down arrow.
 *
 * @param mode the mode to show.
 * @param onClick called when the pill is tapped.
 * @param modifier modifier for the pill.
 */
@Composable
fun ModePillChip(mode: Role, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = modeColor(mode)
    val name = stringResource(modeName(mode))
    val description = stringResource(R.string.profile_mode_pill_description, name)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .clickable(role = SemanticsRole.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(ModePillTags.PILL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(name, style = MaterialTheme.typography.labelMedium, color = color)
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
    }
}

/** Bottom sheet that holds the mode options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseModeSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NightSurface,
        modifier = Modifier.testTag(ModePillTags.SHEET)
    ) {
        content()
    }
}

/**
 * The string resource naming a mode, as the storyboard words it.
 *
 * @return the resource id.
 */
fun modeName(role: Role): Int = when (role) {
    Role.ENTRANT -> R.string.profile_mode_camper
    Role.OWNER -> R.string.profile_mode_owner
    Role.ADMIN -> R.string.profile_mode_admin
}

/**
 * The string resource describing what a mode is for.
 *
 * @return the resource id.
 */
fun modeBody(role: Role): Int = when (role) {
    Role.ENTRANT -> R.string.profile_mode_camper_body
    Role.OWNER -> R.string.profile_mode_owner_body
    Role.ADMIN -> R.string.profile_mode_admin_body
}

/**
 * The accent colour for a mode: amber for Admin, green for Owner, mint for Camper.
 *
 * @return the colour.
 */
fun modeColor(role: Role): Color = when (role) {
    Role.ENTRANT -> CamperMint
    Role.OWNER -> TrailGreen
    Role.ADMIN -> AdminAmber
}

/** Test tags for the mode pill and sheet. */
object ModePillTags {
    const val PILL = "mode_pill"
    const val SHEET = "mode_sheet"
}
