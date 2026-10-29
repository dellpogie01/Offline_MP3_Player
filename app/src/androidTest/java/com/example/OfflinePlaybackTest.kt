package com.example

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.lifecycle.Lifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.service.MusicPlaybackService
import com.example.data.local.AppDatabase
import com.example.data.local.EqualizerEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/** Run on an emulator in airplane mode; creates and removes only its own audio fixture. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class OfflinePlaybackTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun opensLibraryAndPlaysLocalAudioWithBackgroundMediaControlsOffline() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertEquals("Enable airplane mode before running this test", 1,
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0))
        assertEquals(PackageManager.PERMISSION_DENIED,
            context.checkSelfPermission(Manifest.permission.INTERNET))
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "SonicBeat Offline Test.wav")
            put(MediaStore.Audio.Media.TITLE, "SonicBeat Offline Test")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/SonicBeatTests")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values))
        var controller: MediaController? = null
        var scenario: ActivityScenario<MainActivity>? = null
        val equalizerDao = AppDatabase.getDatabase(context).equalizerDao()
        val originalSettings = runBlocking { equalizerDao.getEqualizerSettings().first() } ?: EqualizerEntity()
        try {
            resolver.openOutputStream(uri)!!.use { it.write(testTone()) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            scenario = ActivityScenario.launch(MainActivity::class.java)
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText("Music Library").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Music Library").assertIsDisplayed()
            compose.onNodeWithText("Login").assertDoesNotExist()
            compose.onNodeWithText("Register").assertDoesNotExist()
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("SonicBeat Offline Test").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("SonicBeat Offline Test").performClick()
            lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
            instrumentation.runOnMainSync {
                future = MediaController.Builder(context,
                    SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))).buildAsync()
            }
            val connected = future.get(20, TimeUnit.SECONDS)
            controller = connected
            fun playing(): Boolean {
                var result = false
                instrumentation.runOnMainSync { result = connected.isPlaying }
                return result
            }
            compose.waitUntil(30_000) { playing() }
            scenario.moveToState(Lifecycle.State.CREATED)
            var before = 0L
            instrumentation.runOnMainSync { before = connected.currentPosition }
            compose.waitUntil(15_000) {
                var advanced = false
                instrumentation.runOnMainSync { advanced = connected.isPlaying && connected.currentPosition > before + 500 }
                advanced
            }
            @Suppress("DEPRECATION")
            val services = (context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager).getRunningServices(Int.MAX_VALUE)
            assertTrue("Background playback must run in a foreground service", services.any {
                it.service.className == MusicPlaybackService::class.java.name && it.foreground
            })
            instrumentation.runOnMainSync { connected.pause() }
            compose.waitUntil(10_000) { !playing() }
            instrumentation.runOnMainSync { connected.seekTo(2_000); connected.play() }
            compose.waitUntil(10_000) { playing() }
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithText("Music Library").assertIsDisplayed()
            compose.onNodeWithText("Settings").performClick()
            compose.onNodeWithText("Account").assertDoesNotExist()
            compose.onNodeWithText("Logout").assertDoesNotExist()
            compose.onNodeWithText("Graphic Equalizer & Bass Boost").assertIsDisplayed()
            compose.onNodeWithText("Graphic Equalizer & Bass Boost").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Presets").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Bass").performClick()
            compose.onNodeWithContentDescription("60 Hz gain").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(600f) }
            if (!runBlocking { equalizerDao.getEqualizerSettings().first() }!!.bassBoostEnabled) {
                compose.onNodeWithContentDescription("Enable Bass Boost").performScrollTo().performClick()
            }
            compose.onNodeWithContentDescription("Bass strength").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(800f) }
            compose.waitUntil(10_000) {
                val saved = runBlocking { equalizerDao.getEqualizerSettings().first() }
                saved?.enabled == true && saved.band1 == 600.toShort() && saved.presetName == "Custom" &&
                    saved.bassBoostEnabled && saved.bassBoostStrength == 800.toShort()
            }
            assertTrue("Adjusting equalizer and bass must not interrupt playback", playing())
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Graphic Equalizer & Bass Boost").performClick()
            compose.onNodeWithContentDescription("60 Hz gain").performScrollTo()
            assertEquals(600f, compose.onNodeWithContentDescription("60 Hz gain").fetchSemanticsNode()
                .config[SemanticsProperties.ProgressBarRangeInfo].current, 0.01f)
        } finally {
            instrumentation.runOnMainSync { controller?.pause(); controller?.release() }
            scenario?.close()
            runBlocking { equalizerDao.saveEqualizerSettings(originalSettings) }
            resolver.delete(uri, null, null)
        }
    }

    private fun testTone(): ByteArray {
        val rate = 8_000
        val samples = rate * 120
        return ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + samples * 2); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(rate); putInt(rate * 2)
            putShort(2); putShort(16); put("data".toByteArray()); putInt(samples * 2)
            repeat(samples) { putShort((kotlin.math.sin(it * 2 * Math.PI * 440 / rate) * 1_000).toInt().toShort()) }
        }.array()
    }
}
