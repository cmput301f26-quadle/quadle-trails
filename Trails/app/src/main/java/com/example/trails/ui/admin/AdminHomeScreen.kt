// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.04.01, 03.05.01
// PURPOSE: Admin's home screen (Home tab in Admin mode): entry to browsing campsites and profiles.
// Outstanding issues: no admin pill / mode switcher in the header yet (US 03.11.01 is Aarav's).
package com.example.trails.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects the admin home to navigation and renders [AdminHomeContent].
 *
 * @param navController used to open the admin campsites and profiles screens.
 * @param modifier modifier for the screen.
 */
@Composable
fun AdminHomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    AdminHomeContent(
        onCampsitesClick = { navController.navigate(AdminRoutes.ADMIN_CAMPSITES) },
        onProfilesClick = { navController.navigate(AdminRoutes.ADMIN_PROFILES) },
        modifier = modifier
    )
}

/**
 * Stateless admin home UI, split out so UI tests can drive it directly.
 *
 * @param onCampsitesClick called when "Campsites" is tapped (US 03.04.01).
 * @param onProfilesClick called when "Profiles" is tapped (US 03.05.01).
 * @param modifier modifier for the screen.
 */
@Composable
fun AdminHomeContent(
    onCampsitesClick: () -> Unit,
    onProfilesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(AdminHomeTags.SCREEN),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.admin_home_title), style = MaterialTheme.typography.titleLarge)
        AdminHomeButton(stringResource(R.string.admin_home_campsites), onCampsitesClick, AdminHomeTags.CAMPSITES)
        AdminHomeButton(stringResource(R.string.admin_home_profiles), onProfilesClick, AdminHomeTags.PROFILES)
    }
}

/** Full-width button in the same shape as the other primary buttons in the app. */
@Composable
private fun AdminHomeButton(text: String, onClick: () -> Unit, tag: String) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(tag)
    ) { Text(text) }
}

/** Test tags used by UI tests to find elements on this screen. */
object AdminHomeTags {
    const val SCREEN = "admin_home_screen"
    const val CAMPSITES = "admin_home_campsites"
    const val PROFILES = "admin_home_profiles"
}

/** Design-time preview of the admin home. */
@Preview(showBackground = true)
@Composable
private fun AdminHomeContentPreview() {
    TrailsTheme { AdminHomeContent(onCampsitesClick = {}, onProfilesClick = {}) }
}
