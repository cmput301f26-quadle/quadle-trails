// OWNER: Zainab (zsaghir) — App shell; live list by Aarav (aarav-kapoor-cs) — Notifications
// USER STORIES: 01.03.01, 01.03.02 (lottery results), and every other notification the user receives
// PURPOSE: Screen behind the Notifications tab on the bottom bar: the user's notifications,
//  newest first, or an empty, loading or error state.
// Outstanding issues: none.
// AI assistance: Used AI to help connect the tab to the live notification list and its states.
package com.example.trails.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.model.Notification
import com.example.trails.ui.notification.NotificationList
import com.example.trails.ui.notification.NotificationsTabViewModel
import com.example.trails.ui.notification.NotificationsUiState
import com.example.trails.ui.theme.TrailsTheme

/**
 * Notifications tab. Connects [NotificationsTabViewModel] to [NotificationsContent].
 *
 * @param navController lets a notification open the reservations list.
 * @param modifier modifier for the screen.
 * @param viewModel the tab's controller.
 */
@Composable
fun NotificationsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: NotificationsTabViewModel = viewModel { NotificationsTabViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    NotificationsContent(
        modifier = modifier,
        state = state,
        onRetry = viewModel::load,
        listContent = { items -> NotificationList().RenderList(navController, items) }
    )
}

/**
 * Stateless Notifications UI: the list, or "No notifications yet", a spinner, or an error
 * with "Try again".
 *
 * @param modifier modifier for the screen.
 * @param state what to show; defaults to no notifications.
 * @param onRetry called from "Try again" after a loading error.
 * @param listContent draws the notification cards.
 */
@Composable
fun NotificationsContent(
    modifier: Modifier = Modifier,
    state: NotificationsUiState = NotificationsUiState.Loaded(emptyList()),
    onRetry: () -> Unit = {},
    listContent: @Composable (List<Notification>) -> Unit = {}
) {
    when {
        state is NotificationsUiState.Loaded && state.items.isEmpty() -> ShellEmptyState(
            title = stringResource(R.string.shell_notifications_title),
            icon = R.drawable.ic_nav_notifications,
            message = stringResource(R.string.shell_notifications_empty),
            hint = stringResource(R.string.shell_notifications_empty_hint),
            screenTag = NotificationsTags.SCREEN,
            messageTag = NotificationsTags.EMPTY,
            modifier = modifier
        )
        else -> Column(modifier.fillMaxSize().testTag(NotificationsTags.SCREEN)) {
            Text(
                stringResource(R.string.shell_notifications_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            when (state) {
                NotificationsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.testTag(NotificationsTags.LOADING))
                }
                is NotificationsUiState.Error -> Column(
                    Modifier.fillMaxSize().padding(24.dp).testTag(NotificationsTags.ERROR),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.profile_notifications_load_failed), textAlign = TextAlign.Center)
                    TextButton(onClick = onRetry, modifier = Modifier.testTag(NotificationsTags.RETRY)) {
                        Text(stringResource(R.string.profile_notifications_retry))
                    }
                }
                is NotificationsUiState.Loaded -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag(NotificationsTags.LIST)
                ) { listContent(state.items) }
            }
        }
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object NotificationsTags {
    const val SCREEN = "notifications_screen"
    const val EMPTY = "notifications_empty"
    const val LOADING = "notifications_loading"
    const val ERROR = "notifications_error"
    const val RETRY = "notifications_retry"
    const val LIST = "notifications_list"
}

/** Design-time preview of the empty Notifications tab. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun NotificationsContentPreview() {
    TrailsTheme { NotificationsContent() }
}
