// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
// USER STORIES: 02.11.01
// PURPOSE: Asks an entrant to accept the campsite rules before joining.
// TODO: Show the campsite's rules and record rulesAcceptedAt on accept.
package com.example.trails.ui.waitlist

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Date

import androidx.compose.ui.tooling.preview.Preview
import com.example.trails.ui.theme.TrailsTheme
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmReservationScreen(
    navController: NavController,
    campsiteId: String,
    modifier: Modifier = Modifier
) {
    var isChecked by remember { mutableStateOf(false) }
    var campsiteRules by remember { mutableStateOf("Loading rules...") }
    val coroutineScope = rememberCoroutineScope()

    // Fetch the campsite rules from the fake database when the screen loads
    LaunchedEffect(campsiteId) {
        val campsite = AppContainer.campsiteRepository.getById(campsiteId)
        campsiteRules = campsite?.rules ?: "No rules available."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm reservation") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // get the campsite to find its active lottery ID
                                val campsite = AppContainer.campsiteRepository.getById(campsiteId)
                                val lotteryId = campsite?.activeLotteryId

                                if (lotteryId != null) {
                                    // create the waitlist entry with the current timestamp
                                    val newEntry = WaitlistEntry(
                                        userId = CurrentUser.id(),
                                        userName = "Current User", // Normally fetched from UserRepository
                                        lotteryId = lotteryId,
                                        partySize = 2,
                                        joinedAt = Timestamp(Date()),
                                        status = WaitlistStatus.WAITING,
                                        rulesAcceptedAt = Timestamp(Date())
                                    )
                                    //  save to the database (currently pointing to FakeWaitlistRepository)
                                    AppContainer.waitlistRepository.save(newEntry)

                                    // return to the campsite screen (or route to a success screen)
                                    navController.popBackStack()
                                }
                            }
                        },
                        enabled = isChecked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm reservation")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Campsite rules & policies",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = campsiteRules,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "I have read and agree to these rules",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isChecked) {
                        Text(
                            text = "Rules accepted. You can now confirm your reservation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Agree to the campsite rules to confirm your reservation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
