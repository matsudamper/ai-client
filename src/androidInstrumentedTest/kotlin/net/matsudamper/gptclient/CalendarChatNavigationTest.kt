package net.matsudamper.gptclient

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import net.matsudamper.gptclient.ui.NewChatTestTag
import net.matsudamper.gptclient.ui.ProjectScreenTestTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class CalendarChatNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigateToCalendarChat() = runTest {
        composeTestRule.waitUntilExactlyOne(
            hasText(NewChatTestTag.ProjectButton(0).testTag()),
            10.seconds,
        ).assertIsDisplayed()
            .performClick()

        composeTestRule.waitUntilExactlyOne(
            hasText(ProjectScreenTestTag.Root.testTag()),
            10.seconds,
        ).assertIsDisplayed()
    }
}
