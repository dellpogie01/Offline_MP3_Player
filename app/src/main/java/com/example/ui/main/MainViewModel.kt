package com.example.ui.main

import android.app.Application
import com.example.ui.theme.ThemePreferences
import com.example.ui.theme.ThemeMode
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.EqualizerEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val themePreferences = ThemePreferences(application)
    val themeMode = themePreferences.mode
    fun setThemeMode(mode: ThemeMode) = themePreferences.setMode(mode)

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()
    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage = _scanMessage.asStateFlow()

    private val musicRepository = MusicRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val playlistDao = database.playlistDao()
    private val favoriteDao = database.favoriteDao()
    private val equalizerDao = database.equalizerDao()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = combine(_songs, _searchQuery) { songList, query ->
        if (query.isBlank()) songList
        else songList.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true) ||
                    it.folderName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Long>> = favoriteDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _equalizerSettings = MutableStateFlow<EqualizerEntity?>(null)
    val equalizerSettings = _equalizerSettings.asStateFlow()
    private val equalizerWrites = Channel<EqualizerEntity>(Channel.CONFLATED)
    private var equalizerEdited = false

    init {
        viewModelScope.launch {
            equalizerDao.getEqualizerSettings().collect {
                if (!equalizerEdited) _equalizerSettings.value = it ?: EqualizerEntity()
            }
        }
        viewModelScope.launch {
            // One writer preserves the last slider value, even during rapid dragging.
            for (settings in equalizerWrites) equalizerDao.saveEqualizerSettings(settings)
        }
        loadSongs()
    }

    fun loadSongs() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanMessage.value = null
        viewModelScope.launch {
            try {
                _songs.value = musicRepository.fetchLocalSongs()
                _scanMessage.value = "Library updated: ${_songs.value.size} songs"
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _scanMessage.value = "Unable to scan audio. Please try again."
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            val currentFavorites = favorites.value
            if (currentFavorites.contains(songId)) {
                favoriteDao.removeFavorite(songId)
            } else {
                favoriteDao.insertFavorite(FavoriteEntity(songId))
            }
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.deletePlaylistWithSongs(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistDao.addSongToPlaylist(PlaylistSongEntity(playlistId, songId))
        }
    }

    fun playlistSongIds(playlistId: Long) = playlistDao.getSongIdsForPlaylist(playlistId)

    fun addSongsToPlaylist(playlistId: Long, songIds: Set<Long>) {
        viewModelScope.launch {
            playlistDao.addSongsToPlaylist(songIds.map { PlaylistSongEntity(playlistId, it) })
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistDao.removeSongFromPlaylist(playlistId, songId) }
    }

    fun updateEqualizerSettings(update: (EqualizerEntity) -> EqualizerEntity) {
        val current = _equalizerSettings.value ?: return
        equalizerEdited = true
        val settings = update(current)
        _equalizerSettings.value = settings
        equalizerWrites.trySend(settings)
    }
}
