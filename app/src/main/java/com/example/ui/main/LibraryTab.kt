package com.example.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.player.PlayerViewModel

@Composable
fun LibraryTab(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    focusSearch: Boolean = false,
    onSearchFocused: () -> Unit = {}
) {
    val songs by mainViewModel.filteredSongs.collectAsState()
    val searchQuery by mainViewModel.searchQuery.collectAsState()

    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchFieldValue by remember {
        mutableStateOf(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
    }

    LaunchedEffect(searchQuery) {
        if (searchFieldValue.text != searchQuery) {
            searchFieldValue = TextFieldValue(searchQuery, TextRange(searchQuery.length))
        }
    }
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            searchFieldValue = TextFieldValue(searchQuery, TextRange(searchQuery.length))
            searchFocusRequester.requestFocus()
            keyboardController?.show()
            onSearchFocused()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Music Library",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { mainViewModel.loadSongs() }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchFieldValue,
            onValueChange = {
                searchFieldValue = it
                mainViewModel.setSearchQuery(it.text)
            },
            placeholder = { Text("Search title, artist, album...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching audio files found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(songs) { song ->
                    SongListItem(song = song) {
                        playerViewModel.playQueue(songs, songs.indexOf(song))
                    }
                }
            }
        }
    }
}
