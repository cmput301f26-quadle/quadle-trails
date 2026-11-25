package com.example.trails.ui.testing

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.fake.TestUsers
import com.example.trails.ui.theme.TrailsTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TestingRoleGateTest {
    @get:Rule val compose = createComposeRule()
    @Before fun setUp() { CurrentUser.reset() }
    @After fun tearDown() { CurrentUser.reset() }

    @Test fun testingBuild_blocksAppUntilRoleChosen() {
        compose.setContent {
            TrailsTheme { TestingRoleGate(enabled = true) { Text("App opened") } }
        }
        compose.onNodeWithText("Choose your testing role").assertIsDisplayed()
        compose.onNodeWithText("App opened").assertDoesNotExist()
        compose.onNodeWithTag("testing_role_OWNER").performClick()
        compose.onNodeWithText("App opened").assertIsDisplayed()
        compose.onNodeWithText("Choose your testing role").assertDoesNotExist()
        compose.runOnIdle { assertEquals(TestUsers.OWNER_ID, CurrentUser.id()) }
    }

    @Test fun releaseBehavior_opensAppWithoutTestingDialog() {
        compose.setContent {
            TrailsTheme { TestingRoleGate(enabled = false) { Text("App opened") } }
        }
        compose.onNodeWithText("App opened").assertIsDisplayed()
        compose.onNodeWithText("Choose your testing role").assertDoesNotExist()
    }
}
