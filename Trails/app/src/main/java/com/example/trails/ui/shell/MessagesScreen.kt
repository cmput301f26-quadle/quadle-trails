// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: Screen behind the Messages tab on the bottom bar.
// Outstanding issues: shows the empty state only; the real messages list comes with
//   US 02.12.01 in Part 4.
package com.example.trails.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.ui.theme.TrailsTheme

/**
 * Messages tab. Renders [MessagesContent] until messaging is built.
 *
 * @param navController unused for now; kept so the real screen can navigate.
 * @param modifier modifier for the screen.
 */
@Composable
fun MessagesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    MessagesContent(modifier = modifier)
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
