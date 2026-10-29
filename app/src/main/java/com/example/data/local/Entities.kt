package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long
)

@Entity(tableName = "equalizer_settings")
data class EqualizerEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    val presetName: String = "Normal",
    val band1: Short = 0,
    val band2: Short = 0,
    val band3: Short = 0,
    val band4: Short = 0,
    val band5: Short = 0,
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Short = 0
)
