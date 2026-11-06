// UI (intent) tests for the period picker, US 02.04.03 (registration) and 02.04.04 (booking).
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DateRangePickerContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    // A fixed future April so the days are always tappable (Figma F11 uses April 2027).
    private val apr1 = CalendarDay(2030, 4, 1)
    private val apr8 = CalendarDay(2030, 4, 8)

    private fun show(onConfirm: (Long, Long) -> Unit = { _, _ -> }, onDismiss: () -> Unit = {}) {
        composeRule.setContent {
            TrailsTheme {
                DateRangePickerContent(
                    title = "Registration period",
                    confirmLabel = "Set registration period",
                    initialStart = null,
                    initialEnd = null,
                    defaultStartMinute = 9 * 60,
                    defaultEndMinute = 9 * 60,
                    minDay = apr1,
                    lockedStart = null,
                    summary = { s, e -> "${spanText(spanBetween(s, e))} to enter" },
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun nothingPicked_confirmDisabled() {
        show()
        composeRule.onNodeWithText("Tap the first day, then the last day.").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.SHEET_CONFIRM).assertIsNotEnabled()
    }

    // Tapping Apr 1 then Apr 8 gives a 7-day period at 09:00, as in Figma F11.
    @Test
    fun tapStartThenEnd_confirmsThatPeriod() {
        var picked: Pair<Long, Long>? = null
        show(onConfirm = { s, e -> picked = s to e })
        composeRule.onNodeWithTag(LotteryTags.day(apr1)).performClick()
        composeRule.onNodeWithTag(LotteryTags.day(apr8)).performClick()
        composeRule.onNodeWithText("7 days to enter").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.SHEET_CONFIRM).assertIsEnabled().performClick()
        assertEquals(apr1.atMinute(9 * 60) to apr8.atMinute(9 * 60), picked)
    }

    @Test
    fun opensAndClosesBoxes_showPickedTimes() {
        show()
        composeRule.onNodeWithTag(LotteryTags.day(apr1)).performClick()
        composeRule.onNodeWithTag(LotteryTags.day(apr8)).performClick()
        composeRule.onNodeWithText(formatPickerDateTime(apr1.atMinute(9 * 60))).assertIsDisplayed()
        composeRule.onNodeWithText(formatPickerDateTime(apr8.atMinute(9 * 60))).assertIsDisplayed()
    }

    @Test
    fun cancel_callsOnDismiss() {
        var dismissed = false
        show(onDismiss = { dismissed = true })
        composeRule.onNodeWithText("Cancel").performClick()
        assertTrue(dismissed)
    }
}
