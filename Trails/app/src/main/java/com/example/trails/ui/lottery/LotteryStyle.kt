// Colours and text sizes for the lottery screens, read from the Figma mockups (Skanda page).
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Outstanding issues: the colours below aren't in the shared theme yet. They could move to
//  ui/theme in a small shared PR if other screens need them.
package com.example.trails.ui.lottery

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Figma colours that the shared theme doesn't have a role for. */
internal object LotteryColors {
    /** Secondary text and labels. */
    val Muted = Color(0xFF9EA8A3)

    /** Card borders, dividers and empty progress bars. */
    val Line = Color(0xFF33483C)

    /** "Scheduled" badge. */
    val Amber = Color(0xFFF5C76A)

    /** Inline errors, e.g. a booking period that overlaps registration. */
    val Error = Color(0xFFFF8A8A)

    /** Disabled button fill and text. */
    val DisabledFill = Color(0xFF233329)
    val DisabledText = Color(0xFF7E9487)

    /** Bottom sheet background. */
    val Sheet = Color(0xFF111A15)

    /** Unselected filter chip text. */
    val ChipText = Color(0xFFD6D9DD)

    /** Middle of the screen background glow. */
    val Glow = Color(0xFF1A2B24)

    /** Light green fill behind a selected date range. */
    val RangeFill = Color(0x2E22C55E)
}

/** The dark → green glow → dark background on every lottery screen. */
@Composable
internal fun lotteryBackground(): Brush {
    val night = MaterialTheme.colorScheme.background
    return Brush.horizontalGradient(0f to night, 0.55f to LotteryColors.Glow, 1f to night)
}

/** Text sizes and weights from the mockups (Inter in Figma; the app's default font here). */
internal object LotteryType {
    val Header = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val Heading = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, letterSpacing = (-0.2).sp)
    val Title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.22).sp)
    val Hint = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val Label = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val Body = TextStyle(fontSize = 14.sp)
    val BodyStrong = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val CardTitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    val CardLine = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val Action = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    val Small = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val SmallStrong = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    val Tiny = TextStyle(fontSize = 11.sp)
    val TinyStrong = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val Button = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val BigNumber = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold)
    val Number = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
}
