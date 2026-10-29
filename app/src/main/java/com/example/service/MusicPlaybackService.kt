package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.EqualizerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val effects = EqualizerAudioProcessor()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val renderers = object : DefaultRenderersFactory(this) {
            @Suppress("DEPRECATION")
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean): AudioSink {
                // PCM-only output prevents hardware passthrough/offload from bypassing the EQ.
                return DefaultAudioSink.Builder()
                    .setAudioCapabilities(AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES)
                    .setEnableFloatOutput(false)
                    .setAudioProcessors(arrayOf(effects))
                    .build()
            }
        }
        player = ExoPlayer.Builder(this, renderers).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        mediaSession = MediaSession.Builder(this, player).setSessionActivity(pendingIntent).build()
        serviceScope.launch {
            AppDatabase.getDatabase(this@MusicPlaybackService).equalizerDao().getEqualizerSettings().collect {
                effects.updateSettings(it ?: EqualizerEntity())
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run { player.release(); release() }
        mediaSession = null
        super.onDestroy()
    }
}
