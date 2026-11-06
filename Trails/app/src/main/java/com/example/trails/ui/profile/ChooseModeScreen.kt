// Handles switching between the roles a user holds, the "Use Quadle as" list (US 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help implement authorized Admin, Owner, and Camper mode selection.
package com.example.trails.ui.profile

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.navigation.goToHomeFor
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.Forest
import com.example.trails.ui.theme.Mist
import com.example.trails.ui.theme.Moss
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme
import androidx.compose.ui.semantics.Role as SemanticsRole

/**
 * Connects Choose a mode, as a full screen, to its ViewModel. Goes to the new mode's
 * home once the switch is saved. The usual way in is the mode pill's sheet instead.
 *
 * @param navController used to go back and to the new mode's home.
 * @param modifier modifier for the screen.
 * @param viewModel the profile controller.
 */
@Composable
fun ChooseModeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    val modeSwitch by viewModel.modeSwitch.collectAsState()

    LaunchedEffect(modeSwitch) {
        val switched = modeSwitch as? ModeSwitchState.Switched ?: return@LaunchedEffect
        viewModel.onModeSwitchHandled()
        navController.goToHomeFor(switched.mode)
    }

    Column(modifier.fillMaxSize()) {
        TrailsTopBar(
            title = stringResource(R.string.profile_choose_mode_title),
            onBack = { navController.popBackStack() }
        )
        when (val current = state) {
            is ProfileUiState.Loaded -> ChooseModeContent(
                user = current.user,
                modeSwitch = modeSwitch,
                onSwitch = viewModel::onModeSelected,
                onProfile = { navController.navigate(ProfileRoutes.DEVICE_PROFILE) },
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
            ProfileUiState.Loading -> ProfileLoadingContent()
            else -> ProfileErrorContent(viewModel::retry)
        }
    }
}

/**
 * Stateless "Use Quadle as" list. Only the roles the user holds are listed, in the
 * storyboard's order: Admin, Owner, Camper.
 *
 * @param user the profile, for its name, roles and current mode.
 * @param modeSwitch progress of the switch.
 * @param onSwitch called with the selected mode from "Switch to …".
 * @param onProfile opens the device profile, or null to hide the "Profile" link.
 * @param modifier modifier for the list.
 */
@Composable
fun ChooseModeContent(
    user: User,
    modeSwitch: ModeSwitchState,
    onSwitch: (Role) -> Unit,
    onProfile: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val current = effectiveMode(user)
    var selected by rememberSaveable(user.deviceId) { mutableStateOf(current) }
    val switching = modeSwitch == ModeSwitchState.Switching

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .testTag(ChooseModeTags.SCREEN),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(name = user.name, color = modeColor(current))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(
                        R.string.profile_sheet_subtitle,
                        user.email.ifBlank { stringResource(R.string.profile_title) },
                        stringResource(modeName(current))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Mist
                )
            }
            if (onProfile != null) {
                TextButton(onClick = onProfile, modifier = Modifier.testTag(ChooseModeTags.PROFILE_LINK)) {
                    Text(stringResource(R.string.profile_title), color = TrailGreen)
                }
            }
        }

        Text(
            stringResource(R.string.profile_use_quadle_as).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Mist
        )

        authorisedModes(user).reversed().forEach { role ->
            ModeOption(
                role = role,
                isCurrent = role == current,
                isSelected = role == selected,
                enabled = !switching,
                onSelect = { selected = role }
            )
        }

        if (modeSwitch is ModeSwitchState.Failed) {
            Text(
                stringResource(R.string.profile_error_mode),
                color = Ember,
                modifier = Modifier.testTag(ChooseModeTags.ERROR)
            )
        }
        Button(
            onClick = { onSwitch(selected) },
            enabled = selected != current && !switching,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag(ChooseModeTags.SWITCH)
        ) {
            Text(stringResource(R.string.profile_switch_to, stringResource(modeName(selected))))
        }
        Text(
            stringResource(R.string.profile_mode_footer),
            style = MaterialTheme.typography.bodySmall,
            color = Mist,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModeOption(role: Role, isCurrent: Boolean, isSelected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    val color = modeColor(role)
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) Forest else NightSurface)
            .border(1.dp, if (isSelected) TrailGreen else Moss, shape)
            .selectable(selected = isSelected, enabled = enabled, role = SemanticsRole.RadioButton, onClick = onSelect)
            .padding(12.dp)
            .testTag(ChooseModeTags.option(role)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(modeName(role)), style = MaterialTheme.typography.titleSmall)
                if (isCurrent) {
                    Text(
                        stringResource(R.string.profile_mode_current),
                        style = MaterialTheme.typography.labelSmall,
                        color = AdminAmber,
                        modifier = Modifier
                            .border(1.dp, AdminAmber.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                            .testTag(ChooseModeTags.CURRENT_BADGE)
                    )
                }
            }
            Text(stringResource(modeBody(role)), style = MaterialTheme.typography.bodySmall, color = Mist)
        }
        if (isSelected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TrailGreen)
        } else {
            Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = Mist)
        }
    }
}

/**
 * Round avatar showing the user's initials, e.g. "ZS".
 *
 * @param name the user's name.
 * @param color the ring and text colour.
 */
@Composable
fun InitialsAvatar(name: String, color: androidx.compose.ui.graphics.Color) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials.ifBlank { "?" }, color = color, style = MaterialTheme.typography.titleSmall)
    }
}

/** Test tags for Choose a mode. */
object ChooseModeTags {
    const val SCREEN = "choose_mode"
    const val SWITCH = "choose_mode_switch"
    const val ERROR = "choose_mode_error"
    const val PROFILE_LINK = "choose_mode_profile"
    const val CURRENT_BADGE = "choose_mode_current"

    /** Tag of the option row for [role]. */
    fun option(role: Role): String = "choose_mode_${role.name.lowercase()}"
}

/** Preview: an admin who is also an owner and a camper, as in Scenario 8. */
@Preview(showBackground = true, backgroundColor = 0xFF0F1512)
@Composable
private fun ChooseModePreview() {
    val zainab = User(
        deviceId = "d", name = "Zainab Saghir", email = "zainab_admin",
        roles = listOf(Role.ENTRANT, Role.OWNER, Role.ADMIN), activeMode = Role.ADMIN
    )
    TrailsTheme { ChooseModeContent(zainab, ModeSwitchState.Idle, onSwitch = {}, onProfile = {}) }
}
