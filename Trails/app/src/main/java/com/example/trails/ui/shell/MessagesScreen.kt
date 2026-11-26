// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: Screen behind the Messages tab on the bottom bar.
// Outstanding issues: none. The list inside is Neaz's MessagesInboxScreen (ui/messaging,
//   US 02.12.01, 01.14.01); MessagesContent is its empty state.
package com.example.trails.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.ui.messaging.MessagesInboxScreen
import com.example.trails.ui.theme.TrailsTheme

/**
 * Messages tab: the user's conversations, or [MessagesContent] when there are none.
 *
 * @param navController used to open a conversation.
 * @param modifier modifier for the screen.
 */
@Composable
fun MessagesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    MessagesInboxScreen(navController, modifier = modifier)
}

/**
 * Stateless empty Messages UI: top bar, green chat icon, "No messages yet" and a hint.
 *
 * @param modifier modifier for the screen.
 */
@Composable
fun MessagesContent(modifier: Modifier = Modifier) {
    ShellEmptyState(
        title = stringResource(R.string.shell_messages_title),
        icon = R.drawable.ic_nav_messages,
        message = stringResource(R.string.shell_messages_empty),
        hint = stringResource(R.string.shell_messages_empty_hint),
        screenTag = MessagesTags.SCREEN,
        messageTag = MessagesTags.EMPTY,
        modifier = modifier
    )
}

/** Test tags used by UI tests to find elements on this screen. */
object MessagesTags {
    const val SCREEN = "messages_screen"
    const val EMPTY = "messages_empty"
}

/** Design-time preview of the empty Messages tab. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun MessagesContentPreview() {
    TrailsTheme { MessagesContent() }
}
