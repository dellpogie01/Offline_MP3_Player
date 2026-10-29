package com.example

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.main.LaunchSplash
import com.example.ui.theme.SonicBeatTheme
import org.junit.Rule
import org.junit.Test

class LaunchSplashTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun brandedSplashStaysForThreeSecondsThenRevealsContent() {
        compose.mainClock.autoAdvance = false
        compose.setContent { SonicBeatTheme { LaunchSplash { Text("Library ready") } } }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("MP3 Player").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(2_900)
        compose.onNodeWithText("MP3 Player").assertIsDisplayed()
        compose.onNodeWithText("Library ready").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("Library ready").assertIsDisplayed()
        compose.onNodeWithText("MP3 Player").assertDoesNotExist()
    }
}
