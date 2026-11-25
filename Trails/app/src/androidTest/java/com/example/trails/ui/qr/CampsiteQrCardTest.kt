// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — QR codes
// USER STORIES: 02.02.01
// PURPOSE: UI tests for the "Campsite QR code" card on the owner's campsite page.
// Outstanding issues: none.
package com.example.trails.ui.qr

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.trails.data.fake.TestCampsites
import com.example.trails.ui.campsite.OwnerCampsiteContent
import com.example.trails.ui.campsite.OwnerCampsiteTags
import com.example.trails.ui.campsite.OwnerCampsiteUiState
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Checks that [CampsiteQrCardContent] and [CampsiteQrCard] show the code, the text from
 * Figma and both links, and that the card sits on the owner's campsite page.
 */
class CampsiteQrCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val hinton = TestCampsites.all.first { it.id == TestCampsites.HINTON_ID }

    @Test
    fun card_showsCodeTitleHelpAndLinks() {
        composeRule.setContent {
            TrailsTheme { CampsiteQrCard(campsiteId = hinton.id, campsiteName = hinton.name) }
        }
        composeRule.onNodeWithTag(CampsiteQrTags.IMAGE).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("QR code for this campsite").assertIsDisplayed()
        composeRule.onNodeWithText("Campsite QR code").assertIsDisplayed()
        composeRule.onNodeWithText("Entrants scan this to open the listing.").assertIsDisplayed()
        composeRule.onNodeWithText("Download").assertIsDisplayed()
        composeRule.onNodeWithText("Share").assertIsDisplayed()
    }

    @Test
    fun tapLinks_callDownloadAndShare() {
        var downloads = 0
        var shares = 0
        composeRule.setContent {
            TrailsTheme {
                CampsiteQrCardContent(
                    image = qrBitmap(campsiteQrText(hinton.id), 256).asImageBitmap(),
                    onDownload = { downloads++ },
                    onShare = { shares++ }
                )
            }
        }
        composeRule.onNodeWithTag(CampsiteQrTags.DOWNLOAD).performClick()
        composeRule.onNodeWithTag(CampsiteQrTags.SHARE).performClick()
        assertEquals(1, downloads)
        assertEquals(1, shares)
    }

    @Test
    fun blankId_showsNoCard() {
        composeRule.setContent {
            TrailsTheme { CampsiteQrCard(campsiteId = "", campsiteName = "") }
        }
        composeRule.onNodeWithTag(CampsiteQrTags.CARD).assertDoesNotExist()
    }

    @Test
    fun ownerCampsitePage_showsTheCardAboveEditCampsite() {
        composeRule.setContent {
            TrailsTheme {
                OwnerCampsiteContent(
                    state = OwnerCampsiteUiState.Loaded(hinton, lotteryName = null),
                    showPublishedDialog = false,
                    onDismissPublished = {},
                    onBack = {},
                    onEdit = {},
                    onCreateLottery = {}
                )
            }
        }
        val card = composeRule.onNodeWithTag(CampsiteQrTags.CARD).performScrollTo()
        card.assertIsDisplayed()
        val edit = composeRule.onNodeWithTag(OwnerCampsiteTags.EDIT).performScrollTo()
        val cardBottom = card.fetchSemanticsNode().boundsInRoot.bottom
        val editTop = edit.fetchSemanticsNode().boundsInRoot.top
        assertTrue("QR card should be above Edit Campsite", cardBottom <= editTop)
    }
}
