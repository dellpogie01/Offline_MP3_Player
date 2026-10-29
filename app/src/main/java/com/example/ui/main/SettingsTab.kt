package com.example.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(mainViewModel: MainViewModel, onNavigateToEqualizer: () -> Unit) {
    val themeMode by mainViewModel.themeMode.collectAsState()
    val scanning by mainViewModel.isScanning.collectAsState()
    val scanMessage by mainViewModel.scanMessage.collectAsState()
    var scanRequest by remember { mutableIntStateOf(0) }
    var animating by remember { mutableStateOf(false) }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(scanRequest) {
        if (scanRequest > 0) {
            animating = true
            try {
                do {
                    sweep.snapTo(0f)
                    sweep.animateTo(1f, tween(1200, easing = LinearEasing))
                } while (scanning)
            } finally { animating = false }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Audio & Playback", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        SettingsItem(Icons.Default.Equalizer, "Graphic Equalizer & Bass Boost",
            "Customize frequency bands and audio enhancements", onClick = onNavigateToEqualizer)
        SettingsItem(
            Icons.Default.Refresh,
            if (scanning) "Rescanning Audio Library..." else "Rescan Audio Library",
            if (scanRequest > 0 && !scanning && !animating) scanMessage ?: "Scan device storage for new audio files"
                else "Scan device storage for new audio files",
            enabled = !scanning && !animating,
            modifier = Modifier.drawWithContent {
                drawContent()
                if (animating) {
                    val width = size.width * 0.65f
                    val left = -width + (size.width + width) * sweep.value
                    val colors = listOf(Color.Transparent, Color.Red, Color(0xFFFF9800),
                        Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Transparent)
                    drawRect(Brush.horizontalGradient(colors, left, left + width),
                        topLeft = Offset(left, size.height - 4.dp.toPx()),
                        size = Size(width, 4.dp.toPx()))
                }
            }
        ) {
            scanRequest++
            mainViewModel.loadSongs()
        }
        HorizontalDivider()
        Text("Theme", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Choose your appearance", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(selected = themeMode == mode,
                    onClick = { mainViewModel.setThemeMode(mode) },
                    label = { Text(mode.name) })
            }
        }
        Text(if (themeMode == ThemeMode.System) "Follows your phone's appearance settings."
            else "${themeMode.name} theme is active.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}
