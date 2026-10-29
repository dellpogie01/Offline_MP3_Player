package com.example.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlaylistEntity
import com.example.data.model.Song
import com.example.ui.player.PlayerViewModel

@Composable
fun PlaylistsTab(mainViewModel: MainViewModel, playerViewModel: PlayerViewModel) {
    val playlists by mainViewModel.playlists.collectAsState()
    val favorites by mainViewModel.favorites.collectAsState()
    val songs by mainViewModel.songs.collectAsState()
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<PlaylistEntity?>(null) }
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }

    if (selectedPlaylist != null) {
        PlaylistContents(selectedPlaylist, songs, mainViewModel, playerViewModel,
            onBack = { selectedPlaylistId = null })
    } else {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Playlists & Favorites", modifier = Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (selectedTab == 0) {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Create Playlist", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Playlists") })
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Favorites (${favorites.size})") })
            }
            Spacer(Modifier.height(16.dp))
            if (selectedTab == 0) {
                if (playlists.isEmpty()) {
                    Text("No playlists yet. Tap + to create one, then open it to add songs.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            Surface(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.clickable { selectedPlaylistId = playlist.id }.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(playlist.name, fontWeight = FontWeight.Bold)
                                        Text("Open playlist", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { deleting = playlist }) {
                                        Icon(Icons.Default.Delete, "Delete ${playlist.name}", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val favoriteSongs = songs.filter { it.id in favorites }
                if (favoriteSongs.isEmpty()) Text("No favorite songs added yet")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(favoriteSongs, key = { it.id }) { song ->
                        SongListItem(song) { playerViewModel.playQueue(favoriteSongs, favoriteSongs.indexOf(song)) }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(onDismissRequest = { showCreateDialog = false }, title = { Text("Create Playlist") },
            text = {
                OutlinedTextField(newPlaylistName, { newPlaylistName = it },
                    label = { Text("Playlist Name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(enabled = newPlaylistName.isNotBlank(), onClick = {
                    mainViewModel.createPlaylist(newPlaylistName)
                    newPlaylistName = ""
                    showCreateDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } })
    }
    deleting?.let { playlist ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete ${playlist.name}?") },
            text = { Text("The audio files will stay on your device.") },
            confirmButton = { TextButton(onClick = {
                mainViewModel.deletePlaylist(playlist.id)
                deleting = null
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaylistContents(
    playlist: PlaylistEntity,
    songs: List<Song>,
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val idsFlow = remember(playlist.id, mainViewModel) { mainViewModel.playlistSongIds(playlist.id) }
    val songIds by idsFlow.collectAsState(initial = emptyList())
    val playlistSongs = songs.filter { it.id in songIds }
    var adding by rememberSaveable(playlist.id) { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to playlists") }
            Column(Modifier.weight(1f)) {
                Text(playlist.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${playlistSongs.size} songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { adding = true }) { Icon(Icons.Default.Add, null); Text("Add songs") }
            OutlinedButton(enabled = playlistSongs.isNotEmpty(), onClick = { playerViewModel.playQueue(playlistSongs, 0) }) {
                Icon(Icons.Default.PlayArrow, null); Text("Play all")
            }
        }
        Spacer(Modifier.height(12.dp))
        if (playlistSongs.isEmpty()) Text("This playlist is empty. Tap Add songs to choose music from your library.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(playlistSongs, key = { it.id }) { song ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        SongListItem(song) { playerViewModel.playQueue(playlistSongs, playlistSongs.indexOf(song)) }
                    }
                    IconButton(onClick = { mainViewModel.removeSongFromPlaylist(playlist.id, song.id) }) {
                        Icon(Icons.Default.RemoveCircleOutline, "Remove ${song.title} from playlist")
                    }
                }
            }
        }
    }
    if (adding) {
        PlaylistSongPicker(playlist.name, songs.filter { it.id !in songIds },
            onDismiss = { adding = false }, onAdd = {
                mainViewModel.addSongsToPlaylist(playlist.id, it)
                adding = false
            })
    }
}

@Composable
internal fun PlaylistSongPicker(
    playlistName: String,
    availableSongs: List<Song>,
    onDismiss: () -> Unit,
    onAdd: (Set<Long>) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    val filtered = availableSongs.filter {
        it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add songs to $playlistName") }, text = {
        Column {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search title, artist, album...") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            if (availableSongs.isEmpty()) Text("No more songs to add. Add audio to your device and rescan the library.")
            else if (filtered.isEmpty()) Text("No matching songs")
            else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(filtered, key = { it.id }) { song ->
                    val checked = song.id in selected
                    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = {
                        selected = if (it) selected + song.id else selected - song.id
                    }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }, confirmButton = {
        TextButton(enabled = selected.isNotEmpty(), onClick = { onAdd(selected) }) { Text("Add (${selected.size})") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
