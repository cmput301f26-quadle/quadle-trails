// Shows Welcome, Welcome back, Device profile and Profile deleted (US 01.09.01, 01.08.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: Welcome back has no "View my entries" button because there is no entries screen yet.
package com.example.trails.ui.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.ui.admin.AdminRoutes
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.Forest
import com.example.trails.ui.theme.Mist
import com.example.trails.ui.theme.Moss
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects the Device profile screen to its ViewModel and navigation.
 *
 * @param navController used to go back, to Edit profile and to the app policy.
 * @param modifier modifier for the screen.
 * @param viewModel the profile controller.
 */
@Composable
fun DeviceProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    when (val current = state) {
        is ProfileUiState.Loaded -> DeviceProfileContent(
            user = current.user,
            deviceIdLabel = viewModel.deviceIdLabel,
            onBack = { navController.popBackStack() },
            onManageProfile = { navController.navigate(ProfileRoutes.EDIT_PROFILE) },
            onPolicy = { navController.navigate(AdminRoutes.POLICY) },
            modifier = modifier,
            onUseSampleUser = rememberSampleUserSwitch()
        )
        ProfileUiState.Loading -> ProfileLoadingContent(modifier)
        else -> ProfileErrorContent(viewModel::retry, modifier)
    }
}

/**
 * Welcome, for a device with no profile yet (US 01.09.01). The hero image is the
 * bundled sample photo sample_campsite_hinton, credited in TestCampsites.kt.
 *
 * @param deviceIdLabel the masked device id.
 * @param onContinue called from "Continue to explore".
 * @param modifier modifier for the screen.
 * @param currentId the device id the app runs as, to mark it in the sample user list.
 * @param onUseSampleUser switches to a sample user, or null to hide that list.
 */
@Composable
fun WelcomeContent(
    deviceIdLabel: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    currentId: String? = null,
    onUseSampleUser: ((String) -> Unit)? = null
) {
    ProfilePage(
        title = stringResource(R.string.profile_welcome_screen_title),
        tag = ProfileTags.WELCOME,
        actions = { ModePillChip(mode = Role.ENTRANT, onClick = {}) ; Spacer(Modifier.width(12.dp)) },
        modifier = modifier
    ) {
        Image(
            painter = painterResource(R.drawable.sample_campsite_hinton),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag(ProfileTags.HERO)
        )
        Text(stringResource(R.string.profile_welcome_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.profile_welcome_body), style = MaterialTheme.typography.bodyMedium, color = Mist)
        GreenBadge(stringResource(R.string.profile_welcome_badge))
        GreenCard(stringResource(R.string.profile_no_password_title), stringResource(R.string.profile_no_password_body))
        DeviceIdCard(deviceIdLabel, stringResource(R.string.profile_device_identifier_created))
        Text(stringResource(R.string.profile_device_bound_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.profile_device_bound_body), style = MaterialTheme.typography.bodySmall, color = Mist)
        PrimaryButton(stringResource(R.string.profile_continue), onContinue, ProfileTags.CONTINUE)
        if (onUseSampleUser != null) SampleUserSwitcher(currentId, onUseSampleUser)
    }
}

/**
 * Welcome back, for a device whose profile was recognised (US 01.09.01).
 *
 * @param user the recognised profile.
 * @param deviceIdLabel the masked device id.
 * @param onContinue called from "Continue exploring".
 * @param onOpenDeviceProfile called from the profile row.
 * @param modifier modifier for the screen.
 * @param pill the mode pill shown in the top bar.
 * @param onUseSampleUser switches to a sample user, or null to hide that list.
 */
@Composable
fun WelcomeBackContent(
    user: User,
    deviceIdLabel: String,
    onContinue: () -> Unit,
    onOpenDeviceProfile: () -> Unit,
    modifier: Modifier = Modifier,
    pill: @Composable () -> Unit = { ModePillChip(effectiveMode(user), onClick = {}) },
    onUseSampleUser: ((String) -> Unit)? = null
) {
    ProfilePage(
        title = stringResource(R.string.profile_welcome_back_screen_title),
        tag = ProfileTags.WELCOME_BACK,
        actions = { pill(); Spacer(Modifier.width(12.dp)) },
        modifier = modifier
    ) {
        Text(
            stringResource(R.string.profile_welcome_back_title, user.name.ifBlank { stringResource(R.string.profile_mode_camper) }),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag(ProfileTags.GREETING)
        )
        Text(stringResource(R.string.profile_welcome_back_body), style = MaterialTheme.typography.bodyMedium, color = Mist)
        ProfileRow(user, onOpenDeviceProfile)
        GreenBadge(stringResource(R.string.profile_recognised_badge), withCheck = true)
        GreenCard(stringResource(R.string.profile_retained_title), stringResource(R.string.profile_retained_body))
        DeviceIdCard(deviceIdLabel, stringResource(R.string.profile_device_identifier_associated))
        PrimaryButton(stringResource(R.string.profile_continue_exploring), onContinue, ProfileTags.CONTINUE_EXPLORING)
        Text(
            stringResource(R.string.profile_recognition_note),
            style = MaterialTheme.typography.bodySmall,
            color = Mist,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (onUseSampleUser != null) SampleUserSwitcher(user.deviceId, onUseSampleUser)
    }
}

/**
 * Device profile: how this profile is tied to the device (US 01.09.01).
 *
 * @param user the profile.
 * @param deviceIdLabel the masked device id.
 * @param onBack called from the back chevron.
 * @param onManageProfile opens Edit profile.
 * @param onPolicy opens the app policy.
 * @param modifier modifier for the screen.
 * @param onUseSampleUser switches to a sample user, or null to hide that list.
 */
@Composable
fun DeviceProfileContent(
    user: User,
    deviceIdLabel: String,
    onBack: () -> Unit,
    onManageProfile: () -> Unit,
    onPolicy: () -> Unit,
    modifier: Modifier = Modifier,
    onUseSampleUser: ((String) -> Unit)? = null
) {
    ProfilePage(
        title = stringResource(R.string.profile_device_profile_title),
        tag = ProfileTags.DEVICE_PROFILE,
        onBack = onBack,
        modifier = modifier
    ) {
        ProfileRow(user, onManageProfile)
        GreenBadge(stringResource(R.string.profile_recognised_badge), withCheck = true)
        GreenCard(stringResource(R.string.profile_no_sign_in_title), stringResource(R.string.profile_no_sign_in_body))
        DeviceIdCard(deviceIdLabel, stringResource(R.string.profile_device_identifier_associated))
        Text(stringResource(R.string.profile_different_device_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.profile_different_device_body), style = MaterialTheme.typography.bodySmall, color = Mist)
        PrimaryButton(stringResource(R.string.profile_manage), onManageProfile, ProfileTags.MANAGE)
        OutlinedButton(
            onClick = onPolicy,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag(ProfileTags.POLICY)
        ) {
            Text(stringResource(R.string.profile_policy))
        }
        Text(stringResource(R.string.profile_breadcrumb), style = MaterialTheme.typography.labelSmall, color = Mist)
        if (onUseSampleUser != null) SampleUserSwitcher(user.deviceId, onUseSampleUser)
    }
}

/**
 * Switches the app to a sample user and restarts the screen stack, so every screen
 * loads again as that user.
 *
 * @return the switch action, or null when the app does not use sample users.
 */
@Composable
fun rememberSampleUserSwitch(): ((String) -> Unit)? {
    if (!CurrentUser.canUseSampleUsers) return null
    val context = LocalContext.current
    return { id ->
        CurrentUser.useSampleUser(id)
        context.findActivity()?.let { activity ->
            activity.startActivity(Intent(activity, activity.javaClass))
            activity.finish()
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Lets a tester run the app as one of the sample users in [TestUsers], so every
 * role can be tried without changing code.
 *
 * @param currentId the device id the app runs as, or null if unknown.
 * @param onUseSampleUser called with the chosen sample user's device id.
 */
@Composable
fun SampleUserSwitcher(currentId: String?, onUseSampleUser: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Moss, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag(ProfileTags.SAMPLE_USERS),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(stringResource(R.string.profile_sample_users_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.profile_sample_users_body), style = MaterialTheme.typography.bodySmall, color = Mist)
        TestUsers.all.forEach { sample ->
            FilterChip(
                selected = sample.deviceId == currentId,
                onClick = { onUseSampleUser(sample.deviceId) },
                label = {
                    Text(
                        stringResource(
                            R.string.profile_sheet_subtitle,
                            sample.name,
                            stringResource(modeName(effectiveMode(sample)))
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ProfileTags.sampleUser(sample.deviceId))
            )
        }
    }
}

/**
 * Shown after the profile is deleted (US 01.08.01).
 *
 * @param onBrowse called from "Browse Campsites".
 * @param onSetUpNew called from "Set Up New Profile".
 * @param modifier modifier for the screen.
 */
@Composable
fun ProfileDeletedContent(onBrowse: () -> Unit, onSetUpNew: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        TrailsTopBar(titleContent = { TitleWithIcon(stringResource(R.string.profile_title)) })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(ProfileTags.DELETED),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, TrailGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = TrailGreen, modifier = Modifier.size(40.dp))
            }
            Text(stringResource(R.string.profile_deleted_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.profile_deleted_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Mist,
                textAlign = TextAlign.Center
            )
            PrimaryButton(stringResource(R.string.profile_browse_campsites), onBrowse, ProfileTags.BROWSE)
            OutlinedButton(
                onClick = onSetUpNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag(ProfileTags.SET_UP_NEW)
            ) {
                Text(stringResource(R.string.profile_set_up_new))
            }
        }
    }
}

/**
 * Spinner shown while the profile loads.
 *
 * @param modifier modifier for the screen.
 */
@Composable
fun ProfileLoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().testTag(ProfileTags.LOADING), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Message and retry button shown when the profile cannot be loaded.
 *
 * @param onRetry called from "Try again".
 * @param modifier modifier for the screen.
 */
@Composable
fun ProfileErrorContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(ProfileTags.ERROR),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.profile_error_load), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onRetry, modifier = Modifier.testTag(ProfileTags.RETRY)) {
            Text(stringResource(R.string.profile_retry))
        }
    }
}

/**
 * Round profile photo, or a person icon when there is none.
 *
 * @param url the photo URL, or null.
 * @param size the diameter.
 */
@Composable
fun Avatar(url: String?, size: Dp) {
    val shape = Modifier
        .size(size)
        .clip(CircleShape)
        .background(Forest)
    if (url.isNullOrBlank()) {
        Box(shape, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = TrailGreen, modifier = Modifier.size(size / 2))
        }
    } else {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = shape)
    }
}

/**
 * Top-bar title with the small green person icon used across the storyboard.
 *
 * @param title the title text.
 */
@Composable
fun TitleWithIcon(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(TrailGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Forest, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfilePage(
    title: String,
    tag: String,
    modifier: Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxSize()) {
        if (onBack != null) {
            TrailsTopBar(title = title, onBack = onBack, useChevron = true, backTestTag = ProfileTags.BACK, actions = actions)
        } else {
            TrailsTopBar(titleContent = { TitleWithIcon(title) }, actions = actions)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(tag),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun ProfileRow(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NightSurface)
            .border(1.dp, Moss, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(ProfileTags.PROFILE_ROW),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(user.profileImageUrl, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.profile_on_this_device, stringResource(modeName(effectiveMode(user)))),
                style = MaterialTheme.typography.bodySmall,
                color = Mist
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Mist)
    }
}

@Composable
private fun GreenCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Forest)
            .border(1.dp, TrailGreen.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodySmall, color = Mist)
    }
}

@Composable
private fun DeviceIdCard(deviceIdLabel: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NightSurface)
            .border(1.dp, Moss, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(stringResource(R.string.profile_device_identifier), style = MaterialTheme.typography.titleSmall)
        Text(deviceIdLabel, style = MaterialTheme.typography.bodyMedium, color = Mist, modifier = Modifier.testTag(ProfileTags.DEVICE_ID))
        Text(hint, style = MaterialTheme.typography.labelSmall, color = Mist)
    }
}

@Composable
private fun GreenBadge(text: String, withCheck: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(TrailGreen)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (withCheck) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Forest, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = Forest)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, tag: String) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag(tag)
    ) {
        Text(text)
    }
}

/** Test tags for the profile screens. */
object ProfileTags {
    const val LOADING = "profile_loading"
    const val ERROR = "profile_error"
    const val RETRY = "profile_retry"
    const val WELCOME = "profile_welcome"
    const val HERO = "profile_hero"
    const val CONTINUE = "profile_continue"
    const val WELCOME_BACK = "profile_welcome_back"
    const val GREETING = "profile_greeting"
    const val PROFILE_ROW = "profile_row"
    const val CONTINUE_EXPLORING = "profile_continue_exploring"
    const val DEVICE_PROFILE = "profile_device_profile"
    const val DEVICE_ID = "profile_device_id"
    const val BACK = "profile_back"
    const val MANAGE = "profile_manage"
    const val POLICY = "profile_policy"
    const val DELETED = "profile_deleted"
    const val BROWSE = "profile_browse"
    const val SET_UP_NEW = "profile_set_up_new"
    const val SAMPLE_USERS = "profile_sample_users"

    /** Tag of the sample user option for [deviceId]. */
    fun sampleUser(deviceId: String): String = "profile_sample_user_$deviceId"
}

private val previewUser = User(
    deviceId = "device-entrant-001", name = "Alex Morgan", email = "alex.morgan@email.com",
    roles = listOf(Role.ENTRANT), activeMode = Role.ENTRANT
)

/** Preview: Welcome on a new device. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun WelcomePreview() {
    TrailsTheme { WelcomeContent("QD-••••-7A2F", onContinue = {}) }
}

/** Preview: Welcome back on a recognised device. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun WelcomeBackPreview() {
    TrailsTheme { WelcomeBackContent(previewUser, "QD-••••-7A2F", onContinue = {}, onOpenDeviceProfile = {}) }
}

/** Preview: Device profile. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun DeviceProfilePreview() {
    TrailsTheme { DeviceProfileContent(previewUser, "QD-••••-7A2F", onBack = {}, onManageProfile = {}, onPolicy = {}) }
}

/** Preview: after deleting the profile. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun DeletedPreview() {
    TrailsTheme { ProfileDeletedContent(onBrowse = {}, onSetUpNew = {}) }
}
