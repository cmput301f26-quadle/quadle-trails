// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.01, 01.01.02, 01.01.03, 01.01.04
// PURPOSE: Lets entrants browse and search campsites.
// One sample card is shown so US 01.01.04 and 01.01.05 can be opened.
// TODO: Replace that card with the full list from observeAll() (US 01.01.03).
//       Add search (US 01.01.01) and filters (US 01.01.02).
//       Keep a title whose text is exactly "Browse": TrailsAppTest looks for it.
package com.example.trails.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.AppContainer

@Composable
fun BrowseScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val campsites by AppContainer.campsiteRepository.observeAll()
        .collectAsState(initial = emptyList())
    val sample = campsites.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.discovery_browse_title),
            style = MaterialTheme.typography.headlineSmall
        )
        if (sample != null) {
            Spacer(Modifier.height(16.dp))
            CampsiteSummaryCard(
                campsite = sample,
                onClick = { navController.navigate(DiscoveryRoutes.campsiteDetails(sample.id)) }
            )
        }
    }
}
