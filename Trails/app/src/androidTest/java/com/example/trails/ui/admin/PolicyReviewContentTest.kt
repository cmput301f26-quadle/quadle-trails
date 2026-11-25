// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: UI (intent) tests for the admin's Policy review page, its Remove owner dialog and
//   Dismiss reports.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.trails.data.model.Report
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.ui.theme.TrailsTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class PolicyReviewContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun dateOf(year: Int, month: Int, day: Int) =
        Timestamp(Calendar.getInstance().apply { set(year, month, day, 12, 0, 0) }.time)

    private val rule = "Accurate listings · photos and details must match the campsite"

    private val maya = User(
        deviceId = "device-maya", name = "Maya Singh", roles = listOf(Role.OWNER),
        createdAt = dateOf(2026, Calendar.NOVEMBER, 2)
    )

    private val reports = listOf(
        Report(
            id = "r1", ownerId = maya.deviceId, campsiteId = "pine", reason = "Misleading photos",
            ruleBroken = rule, reporterCount = 2, createdAt = dateOf(2026, Calendar.NOVEMBER, 21)
        ),
        Report(
            id = "r2", ownerId = maya.deviceId, campsiteId = "lake", reason = "Not as described",
            ruleBroken = rule, reporterCount = 1, createdAt = dateOf(2026, Calendar.NOVEMBER, 12)
        ),
        Report(
            id = "r3", ownerId = maya.deviceId, campsiteId = "ridge", reason = "Confirmed booking cancelled",
            ruleBroken = "Honour bookings", reporterCount = 1, createdAt = dateOf(2026, Calendar.NOVEMBER, 3)
        )
    )

    private fun mayaWith(action: PolicyActionState = PolicyActionState.Idle, isSelf: Boolean = false) =
        PolicyReviewUiState.Loaded(
            user = maya,
            campsiteCount = 3,
            reports = reports,
            ruleBroken = rule,
            impact = ProfileImpact("Maya Singh", 3, 2),
            action = action,
            isSelf = isSelf,
            campsiteNames = mapOf("pine" to "Pine Ridge", "lake" to "Lakeview", "ridge" to "Ridge Camp")
        )

    private fun show(
        state: PolicyReviewUiState,
        onRemoveClick: () -> Unit = {},
        onDismissDialog: () -> Unit = {},
        onConfirmRemove: () -> Unit = {},
        onDismissReports: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            PolicyReviewContent(
                state, onBack = {},
                onRemoveClick = onRemoveClick, onDismissDialog = onDismissDialog,
                onConfirmRemove = onConfirmRemove, onDismissReports = onDismissReports
            )
        }
    }

    // This checks the header: initials, name, owner line, and the Owner and report count chips.
    @Test
    fun header_showsNameOwnerLineAndChips() {
        show(mayaWith())
        composeRule.onNodeWithText("Policy review").assertIsDisplayed()
        composeRule.onNodeWithText("MS").assertIsDisplayed()
        composeRule.onNodeWithTag(PolicyReviewTags.NAME).assertTextEquals("Maya Singh")
        composeRule.onNodeWithTag(PolicyReviewTags.OWNER_LINE)
            .assertTextEquals("Owner · 3 campsites · Joined Nov 2026")
        composeRule.onNodeWithTag(PolicyReviewTags.CHIPS).assertIsDisplayed()
        composeRule.onNodeWithText("Owner").assertIsDisplayed()
        composeRule.onNodeWithText("3 reports").assertIsDisplayed()
    }

    // This checks that the rule broken card shows its label and the rule.
    @Test
    fun ruleCard_showsRule() {
        show(mayaWith())
        composeRule.onNodeWithTag(PolicyReviewTags.RULE_CARD).assertIsDisplayed()
        composeRule.onNodeWithText("RULE BROKEN").assertIsDisplayed()
        composeRule.onNodeWithText(rule).assertIsDisplayed()
    }

    // This checks that each report has a card with its reason and campsite · campers · date line.
    @Test
    fun reportCards_showReasonAndSubtitle() {
        show(mayaWith())
        composeRule.onNodeWithText("Reports · last 30 days").assertIsDisplayed()
        composeRule.onNodeWithText("Misleading photos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Pine Ridge · 2 campers · Nov 21").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Not as described").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Lakeview · 1 camper · Nov 12").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Confirmed booking cancelled").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ridge Camp · 1 camper · Nov 3").performScrollTo().assertIsDisplayed()
    }

    // This checks the muted line saying what removing the owner takes with them.
    @Test
    fun impactLine_isShown() {
        show(mayaWith())
        composeRule.onNodeWithTag(PolicyReviewTags.IMPACT).performScrollTo().assertTextEquals(
            "Removing also takes down their 3 campsites and cancels 2 lottery entries."
        )
    }

    // This checks that tapping Remove owner opens the "Remove owner?" dialog with what is removed.
    @Test
    fun tapRemove_showsDialog() {
        var state by mutableStateOf<PolicyReviewUiState>(mayaWith())
        composeRule.setContent {
            TrailsTheme {
                PolicyReviewContent(
                    state, onBack = {},
                    onRemoveClick = { state = mayaWith(PolicyActionState.Confirming) }
                )
            }
        }
        composeRule.onNodeWithTag(PolicyReviewTags.DIALOG).assertDoesNotExist()
        composeRule.onNodeWithTag(PolicyReviewTags.REMOVE_BUTTON).performClick()
        composeRule.onNodeWithTag(PolicyReviewTags.DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText("Remove owner?").assertIsDisplayed()
        composeRule.onNodeWithTag(PolicyReviewTags.DIALOG_MESSAGE).assertTextEquals(
            "Maya Singh loses access to Quadle. Their 3 campsites and 2 lottery entries are removed. " +
                "This can't be undone."
        )
    }

    // This checks that Cancel in the dialog asks to close it.
    @Test
    fun dialogCancel_reportsDismissDialog() {
        var closed = false
        show(mayaWith(PolicyActionState.Confirming), onDismissDialog = { closed = true })
        composeRule.onNodeWithTag(PolicyReviewTags.DIALOG_CANCEL).performClick()
        assertTrue(closed)
    }

    // This checks that Remove in the dialog asks to remove the owner.
    @Test
    fun dialogRemove_reportsConfirmRemove() {
        var confirmed = false
        show(mayaWith(PolicyActionState.Confirming), onConfirmRemove = { confirmed = true })
        composeRule.onNodeWithTag(PolicyReviewTags.DIALOG_REMOVE).performClick()
        assertTrue(confirmed)
    }

    // This checks that tapping Dismiss reports asks to dismiss them.
    @Test
    fun tapDismiss_reportsDismissReports() {
        var dismissed = false
        show(mayaWith(), onDismissReports = { dismissed = true })
        composeRule.onNodeWithTag(PolicyReviewTags.DISMISS_BUTTON).performClick()
        assertTrue(dismissed)
    }

    // This checks that the admin can't act on their own reports, so both buttons are hidden.
    @Test
    fun self_hidesButtons() {
        show(mayaWith(isSelf = true))
        composeRule.onNodeWithTag(PolicyReviewTags.REMOVE_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PolicyReviewTags.DISMISS_BUTTON).assertDoesNotExist()
    }

    // This checks that while an action runs both buttons are disabled.
    @Test
    fun working_buttonsDisabled() {
        show(mayaWith(PolicyActionState.Working))
        composeRule.onNodeWithTag(PolicyReviewTags.REMOVE_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(PolicyReviewTags.DISMISS_BUTTON).assertIsNotEnabled()
    }

    // This checks that a failed action shows the error text.
    @Test
    fun error_isShown() {
        show(mayaWith(PolicyActionState.Error))
        composeRule.onNodeWithTag(PolicyReviewTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("Couldn't finish this review. Please try again.").assertIsDisplayed()
        composeRule.onNodeWithTag(PolicyReviewTags.DIALOG).assertDoesNotExist()
    }

    // This checks that an owner who no longer exists shows a message.
    @Test
    fun notFound_showsMessage() {
        show(PolicyReviewUiState.NotFound)
        composeRule.onNodeWithText("This owner no longer exists").assertIsDisplayed()
    }
}
