// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: Screen behind the Notifications tab on the bottom bar.
// Outstanding issues: shows the empty state only; the real notifications list comes with
//   US 01.03.01 in Part 4.
package com.example.trails.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.ui.notification.NotificationList
import com.example.trails.ui.theme.TrailsTheme

/**
 * Notifications tab. Renders [NotificationsContent] until the real list is built.
 *
 * @param navController unused for now; kept so the real screen can navigate.
 * @param modifier modifier for the screen.
 */
@Composable
fun NotificationsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    NotificationsContent(modifier = modifier)
}

/**
 * Stateless empty Notifications UI: top bar, green bell, "No notifications yet" and a hint.
 *
 * @param modifier modifier for the screen.
 */
@Composable
fun NotificationsContent(modifier: Modifier = Modifier) {
//    ShellEmptyState(
//        title = stringResource(R.string.shell_notifications_title),
//        icon = R.drawable.ic_nav_notifications,
//        message = stringResource(R.string.shell_notifications_empty),
//        hint = stringResource(R.string.shell_notifications_empty_hint),
//        screenTag = NotificationsTags.SCREEN,
//        messageTag = NotificationsTags.EMPTY,
//        modifier = modifier
//    )

    Column(modifier = Modifier.fillMaxSize())
    {
        Row(modifier = Modifier.fillMaxWidth().padding(all = 5.dp)) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Notifications",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
        }
        NotificationList().RenderList()
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object NotificationsTags {
    const val SCREEN = "notifications_screen"
    const val EMPTY = "notifications_empty"
}

/** Design-time preview of the empty Notifications tab. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun NotificationsContentPreview() {
    TrailsTheme { NotificationsContent() }
}
