package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.Song
import com.example.ui.main.PlaylistSongPicker
import com.example.ui.theme.SonicBeatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w320dp-h640dp")
class PlaylistSongPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun selectedSongsSurviveSearchAndAreAddedTogether() {
        val songs = listOf(
            Song(1, "First song", "Artist One", "Album One", 1000, "content://one", null, "Music", 0),
            Song(2, "Second song", "Artist Two", "Album Two", 1000, "content://two", null, "Music", 0)
        )
        var added = emptySet<Long>()
        compose.setContent {
            SonicBeatTheme {
                PlaylistSongPicker("Road trip", songs, onDismiss = {}, onAdd = { added = it })
            }
        }
        compose.onNodeWithText("Add (0)").assertIsNotEnabled()
        compose.onNodeWithText("First song").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Album Two")
        compose.onNodeWithText("First song").assertDoesNotExist()
        compose.onNodeWithText("Second song").performClick()
        compose.onNodeWithText("Add (2)").performClick()
        compose.runOnIdle { assertEquals(setOf(1L, 2L), added) }
    }
}
