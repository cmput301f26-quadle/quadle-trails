// Intent tests for New notification and the "Notification sent" dialog (US 02.10.01).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write Compose UI tests for sending a waitlist notification.
package com.example.trails.ui.ownernotify

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.SentRecipient
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI tests for [ComposeNotificationContent], driven with fixed states. */
@RunWith(AndroidJUnit4::class)
class ComposeNotificationContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ready = ComposeUiState.Ready(
        Lottery(id = "june", name = "June 2027"),
        "Grande Cache Ridge",
        listOf(SentRecipient("maya", "Maya Singh", 2), SentRecipient("daniel", "Daniel Kim", 4), SentRecipient("lena", "Lena Fischer", 3))
    )

    private var sends = 0
    private var typedSubject = ""
    private var history = 0
    private var done = 0

    private fun show(
        state: ComposeUiState = ready,
        form: NotifyForm = NotifyForm(),
        errors: NotifyFormErrors = NotifyFormErrors(),
        sendState: SendState = SendState.Idle
    ) {
        composeRule.setContent {
            TrailsTheme {
                ComposeNotificationContent(
                    state = state, form = form, errors = errors, sendState = sendState,
                    onBack = {}, onHistory = { history++ }, onRetry = {},
                    onSubjectChange = { typedSubject = it }, onMessageChange = {},
                    onSend = { sends++ }, onDone = { done++ }
                )
            }
        }
    }

    @Test
    fun ready_showsRecipientsAndWaitlist() {
        show()
        composeRule.onNodeWithText("New notification").assertIsDisplayed()
        composeRule.onNodeWithTag(ComposeTags.TO).assertTextContains("To: 3 entrants", substring = true)
        composeRule.onNodeWithText("Maya Singh, Daniel Kim, Lena Fischer").assertIsDisplayed()
        composeRule.onNodeWithText("Grande Cache Ridge · June 2027 waitlist").assertIsDisplayed()
    }

    @Test
    fun typingSubject_reportsIt() {
        show()
        composeRule.onNodeWithTag(ComposeTags.SUBJECT).performTextInput("Lottery draw date update")
        assertEquals("Lottery draw date update", typedSubject)
    }

    @Test
    fun counter_showsLengthOutOf500() {
        show(form = NotifyForm(message = "x".repeat(130)))
        composeRule.onNodeWithTag(ComposeTags.COUNTER).performScrollTo().assertTextContains("130 / 500")
    }

    @Test
    fun reviewAndSend_callsSend() {
        show(form = NotifyForm("Hello", "Hi"))
        composeRule.onNodeWithTag(ComposeTags.SEND).performScrollTo().performClick()
        assertEquals(1, sends)
    }

    @Test
    fun errors_areShownUnderTheFields() {
        show(errors = NotifyFormErrors(NotifyFieldError.SUBJECT_REQUIRED, NotifyFieldError.MESSAGE_REQUIRED))
        composeRule.onNodeWithText("Subject is required").assertExists()
        composeRule.onNodeWithText("Message is required").assertExists()
    }

    @Test
    fun emptyWaitlist_sendIsDisabled() {
        show(state = ready.copy(recipients = emptyList()))
        composeRule.onNodeWithTag(ComposeTags.NO_ENTRANTS).assertIsDisplayed()
        composeRule.onNodeWithTag(ComposeTags.SEND).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun sending_disablesTheButton() {
        show(sendState = SendState.Sending)
        composeRule.onNodeWithTag(ComposeTags.SEND).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun failure_isShown() {
        show(sendState = SendState.Failed("offline"))
        composeRule.onNodeWithTag(ComposeTags.ERROR).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sent_showsDialog_doneAndViewSent() {
        show(sendState = SendState.Sent("Lottery draw date update", 3, "Grande Cache Ridge · June 2027 waitlist"))
        composeRule.onNodeWithText("Notification sent").assertIsDisplayed()
        composeRule.onNodeWithText("was sent to 3 entrants", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(ComposeTags.VIEW_SENT).performClick()
        composeRule.onNodeWithTag(ComposeTags.DONE).performClick()
        assertEquals(1, history)
        assertEquals(1, done)
    }

    @Test
    fun historyAction_opensSentNotifications() {
        show()
        composeRule.onNodeWithTag(ComposeTags.HISTORY).performClick()
        assertEquals(1, history)
    }
}
