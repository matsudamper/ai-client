package net.matsudamper.gptclient

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
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
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.POST_NOTIFICATIONS,
    )

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigateToCalendarChat() = runTest {
        composeTestRule.waitUntilExactlyOne(
            hasTestTag(NewChatTestTag.ProjectButton(0).testTag()),
            10.seconds,
        ).assertIsDisplayed()
            .performClick()

        composeTestRule.waitUntilExactlyOne(
            hasTestTag(ProjectScreenTestTag.Root.testTag()),
            10.seconds,
        ).assertIsDisplayed()
    }
}
