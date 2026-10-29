package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongsToPlaylist(songs: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Transaction
    suspend fun deletePlaylistWithSongs(playlistId: Long) {
        clearPlaylistSongs(playlistId)
        deletePlaylist(playlistId)
    }

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId")
    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>>
}

@Dao
interface FavoriteDao {
    @Query("SELECT songId FROM favorites")
    fun getAllFavorites(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)
}

@Dao
interface EqualizerDao {
    @Query("SELECT * FROM equalizer_settings WHERE id = 1")
    fun getEqualizerSettings(): Flow<EqualizerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEqualizerSettings(settings: EqualizerEntity)
}
