// App-wide Material theme matching the dark-green Figma mockups.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
// Outstanding issues: the design is dark-only, so there is no light scheme.
package com.example.trails.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Figma colours mapped onto Material roles. Screens should read colours from
 * MaterialTheme.colorScheme rather than using these values directly.
 */
private val TrailsColorScheme = darkColorScheme(
    primary = TrailGreen,
    onPrimary = Night,
    primaryContainer = Forest,
    onPrimaryContainer = Color.White,
    secondary = TrailGreen,
    onSecondary = Night,
    background = Night,
    onBackground = Color.White,
    surface = Night,
    onSurface = Color.White,
    surfaceVariant = Forest,
    onSurfaceVariant = Mist,
    surfaceContainer = NightSurface,
    surfaceContainerLow = Forest,
    surfaceContainerHigh = NightSurface,
    outline = TrailGreen,
    outlineVariant = Moss,
    error = Ember,
    onError = Night
)

/**
 * Wraps app content in the Trails theme. Always dark, and dynamic (wallpaper) colour
 * is off, so every device looks like the mockups.
 *
 * @param content the UI to theme.
 */
@Composable
fun TrailsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TrailsColorScheme,
        typography = Typography,
        content = content
    )
}
