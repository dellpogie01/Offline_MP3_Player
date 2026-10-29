package com.example.ui.main

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.player.MiniPlayer
import com.example.ui.player.NowPlayingScreen
import com.example.ui.player.PlayerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel,
    mainViewModel: MainViewModel,
    onNavigateToEqualizer: () -> Unit
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission = permission)

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        } else {
            mainViewModel.loadSongs()
        }
    }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            mainViewModel.loadSongs()
        }
    }

    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    var focusLibrarySearch by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }

    if (showFullPlayer) {
        NowPlayingScreen(
            playerViewModel = playerViewModel,
            mainViewModel = mainViewModel,
            onNavigateToEqualizer = {
                showFullPlayer = false
                onNavigateToEqualizer()
            },
            onClose = { showFullPlayer = false }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                MiniPlayer(playerViewModel = playerViewModel) {
                    showFullPlayer = true
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Library") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(imageVector = Icons.Default.PlaylistPlay, contentDescription = "Playlists") },
                        label = { Text("Playlists") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!permissionState.status.isGranted) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Storage permission is required to scan and play local music files.", color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Grant Permission", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> HomeTab(
                        mainViewModel = mainViewModel,
                        playerViewModel = playerViewModel,
                        onSearchInLibrary = {
                            focusLibrarySearch = true
                            selectedTab = 1
                        },
                        onNavigateToLibrary = { selectedTab = 1 }
                    )
                    1 -> LibraryTab(
                        mainViewModel = mainViewModel,
                        playerViewModel = playerViewModel,
                        focusSearch = focusLibrarySearch,
                        onSearchFocused = { focusLibrarySearch = false }
                    )
                    2 -> PlaylistsTab(mainViewModel = mainViewModel, playerViewModel = playerViewModel)
                    3 -> SettingsTab(
                        mainViewModel = mainViewModel,
                        onNavigateToEqualizer = onNavigateToEqualizer
                    )
                }
            }
        }
    }
}
