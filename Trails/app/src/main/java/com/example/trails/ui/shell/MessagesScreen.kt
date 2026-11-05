// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: Screen behind the Messages tab on the bottom bar.
// Outstanding issues: placeholder only; messaging comes in Part 4.
package com.example.trails.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.ui.common.PlaceholderScreen

/**
 * Messages tab. Says "Coming in Part 4" until messaging is built.
 *
 * @param navController unused for now; kept so the real screen can navigate.
 * @param modifier modifier for the screen.
 */
@Composable
fun MessagesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        name = stringResource(R.string.shell_coming_in_part_4, stringResource(R.string.nav_messages)),
        modifier = modifier
    )
}
