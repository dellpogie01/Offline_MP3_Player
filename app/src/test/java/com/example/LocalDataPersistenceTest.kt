package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.EqualizerEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocalDataPersistenceTest {
    @Test
    fun playlistsFavoritesAndPreferencesSurviveReopeningWithoutAnAccount() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "offline-persistence-test"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val settings = EqualizerEntity(enabled = true, presetName = "Rock", band1 = 500)
        val id = open().let { database ->
            try {
                val playlistId = database.playlistDao().insertPlaylist(PlaylistEntity(name = "My music"))
                database.playlistDao().addSongToPlaylist(PlaylistSongEntity(playlistId, 42L))
                database.favoriteDao().insertFavorite(FavoriteEntity(42L))
                database.equalizerDao().saveEqualizerSettings(settings)
                playlistId
            } finally { database.close() }
        }
        val reopened = open()
        try {
            assertEquals("My music", reopened.playlistDao().getAllPlaylists().first().single().name)
            assertEquals(listOf(42L), reopened.playlistDao().getSongIdsForPlaylist(id).first())
            assertEquals(listOf(42L), reopened.favoriteDao().getAllFavorites().first())
            assertEquals(settings, reopened.equalizerDao().getEqualizerSettings().first())
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }
    @Test
    fun playlistMembershipIsIsolatedDeduplicatedAndClearedOnDeletion() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = database.playlistDao()
            val first = dao.insertPlaylist(PlaylistEntity(name = "First"))
            val second = dao.insertPlaylist(PlaylistEntity(name = "Second"))
            dao.addSongsToPlaylist(listOf(PlaylistSongEntity(first, 11), PlaylistSongEntity(first, 12)))
            dao.addSongsToPlaylist(listOf(PlaylistSongEntity(first, 11), PlaylistSongEntity(second, 11)))
            assertEquals(setOf(11L, 12L), dao.getSongIdsForPlaylist(first).first().toSet())
            assertEquals(listOf(11L), dao.getSongIdsForPlaylist(second).first())
            dao.removeSongFromPlaylist(first, 11)
            assertEquals(listOf(12L), dao.getSongIdsForPlaylist(first).first())
            assertEquals(listOf(11L), dao.getSongIdsForPlaylist(second).first())
            dao.deletePlaylistWithSongs(first)
            assertEquals(emptyList<Long>(), dao.getSongIdsForPlaylist(first).first())
            assertEquals(listOf(second), dao.getAllPlaylists().first().map { it.id })
            assertEquals(listOf(11L), dao.getSongIdsForPlaylist(second).first())
        } finally { database.close() }
    }

}
