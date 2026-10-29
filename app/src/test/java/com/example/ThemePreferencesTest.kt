package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThemePreferencesTest {
    @Test
    fun appearanceUpdatesImmediatelyAndSurvivesReopening() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            val appearance = ThemePreferences(context)
            assertEquals(ThemeMode.Dark, appearance.mode.value)
            for (mode in listOf(ThemeMode.Light, ThemeMode.System, ThemeMode.Dark)) {
                appearance.setMode(mode)
                assertEquals(mode, appearance.mode.value)
                assertEquals(mode, ThemePreferences(context).mode.value)
            }
            preferences.edit().putString("theme", "unknown").commit()
            assertEquals(ThemeMode.Dark, ThemePreferences(context).mode.value)
        } finally { preferences.edit().clear().commit() }
    }
}
