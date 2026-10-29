package com.example.ui.player

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.Song
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The service owns playback; this controller only connects the UI to it. */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val controllerFuture = MediaController.Builder(
        application, SessionToken(application, ComponentName(application, MusicPlaybackService::class.java))
    ).buildAsync()
    private var controller: MediaController? = null
    private var pendingQueue: Pair<List<Song>, Int>? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    private val _playbackQueue = MutableStateFlow<List<Song>>(emptyList())
    val playbackQueue = _playbackQueue.asStateFlow()
    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode = _shuffleMode.asStateFlow()
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()
    val currentPosition: Long get() = controller?.currentPosition ?: 0L

    init {
        controllerFuture.addListener({
            if (!controllerFuture.isCancelled) {
                try {
                    val player = controllerFuture.get()
                    controller = player
                    player.addListener(object : Player.Listener {
                        override fun onEvents(player: Player, events: Player.Events) = syncState(player)
                    })
                    syncState(player)
                    pendingQueue?.let { (songs, index) ->
                        pendingQueue = null
                        playQueue(songs, index)
                    }
                } catch (_: java.util.concurrent.ExecutionException) {
                    _isPlaying.value = false
                }
            }
        }, ContextCompat.getMainExecutor(application))
    }

    private fun syncState(player: Player) {
        _isPlaying.value = player.isPlaying
        _shuffleMode.value = player.shuffleModeEnabled
        _repeatMode.value = player.repeatMode
        _playbackQueue.value = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).toSong() }
        _currentSong.value = player.currentMediaItem?.toSong()
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val safeIndex = startIndex.coerceIn(songs.indices)
        val player = controller ?: run {
            pendingQueue = songs.toList() to safeIndex
            return
        }
        player.setMediaItems(songs.map { it.toMediaItem() }, safeIndex, 0)
        player.prepare()
        player.play()
        syncState(player)
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun skipToNext() { controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() } }
    fun skipToPrevious() { controller?.let { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() } }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0)) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun toggleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    override fun onCleared() {
        pendingQueue = null
        MediaController.releaseFuture(controllerFuture)
        controller = null
        super.onCleared()
    }
}

private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uriString)
    .setMediaMetadata(MediaMetadata.Builder()
        .setTitle(title).setArtist(artist).setAlbumTitle(album)
        .setArtworkUri(albumArtUriString?.let(Uri::parse))
        .setExtras(Bundle().apply {
            putLong("duration", duration)
            putString("uri", uriString)
            putString("folder", folderName)
            putLong("dateAdded", dateAdded)
        }).build())
    .build()

private fun MediaItem.toSong(): Song? {
    val id = mediaId.toLongOrNull() ?: return null
    val metadata = mediaMetadata
    val extras = metadata.extras
    return Song(id, metadata.title?.toString() ?: "Unknown Title",
        metadata.artist?.toString() ?: "Unknown Artist", metadata.albumTitle?.toString() ?: "Unknown Album",
        extras?.getLong("duration") ?: 0L, extras?.getString("uri") ?: "content://media/external/audio/media/$id",
        metadata.artworkUri?.toString(), extras?.getString("folder") ?: "", extras?.getLong("dateAdded") ?: 0L)
}
