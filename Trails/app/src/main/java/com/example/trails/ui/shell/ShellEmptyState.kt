// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: Shared layout for an empty tab: shared top bar plus a centred icon, title and hint.
// Outstanding issues: none.
package com.example.trails.ui.shell

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.TrailGreen

/**
 * A tab with nothing to list yet: [TrailsTopBar] with no back arrow, then a centred
 * green icon, a title and one muted hint line.
 *
 * @param title top bar title, e.g. "Notifications".
 * @param icon the tab's bottom-bar icon, drawn in TrailGreen.
 * @param message the empty-state title, e.g. "No notifications yet".
 * @param hint the muted line under [message].
 * @param screenTag test tag for the whole screen.
 * @param messageTag test tag for [message].
 * @param modifier modifier for the screen.
 */
@Composable
internal fun ShellEmptyState(
    title: String,
    @DrawableRes icon: Int,
    message: String,
    hint: String,
    screenTag: String,
    messageTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .testTag(screenTag)
    ) {
        TrailsTopBar(title = title)
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painterResource(icon),
                    contentDescription = null,
                    tint = TrailGreen,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(messageTag)
                )
                Text(
                    hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
