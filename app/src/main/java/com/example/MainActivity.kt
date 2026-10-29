package com.example

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.theme.ThemeMode
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.main.EqualizerScreen
import com.example.ui.main.MainScreen
import com.example.ui.main.MainViewModel
import com.example.ui.player.PlayerViewModel
import com.example.ui.theme.SonicBeatTheme
import com.example.ui.main.LaunchSplash

class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Reveal the branded Compose splash as soon as its first frame is ready.
            splashScreen.setOnExitAnimationListener { it.remove() }
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            val themeMode by mainViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> isSystemInDarkTheme()
            }
            SideEffect {
                val barStyle = if (darkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            SonicBeatTheme(darkTheme = darkTheme) {
              LaunchSplash {
              // Use a new saved-state key so an upgrade cannot restore a removed account route.
              key("offline-navigation-v1") {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            playerViewModel = playerViewModel,
                            mainViewModel = mainViewModel,
                            onNavigateToEqualizer = { navController.navigate("equalizer") }
                        )
                    }
                    composable("equalizer") {
                        EqualizerScreen(
                            mainViewModel = mainViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
              }
              }
            }
        }
    }
}
