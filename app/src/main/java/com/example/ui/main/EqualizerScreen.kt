package com.example.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(mainViewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val settings by mainViewModel.equalizerSettings.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Graphic Equalizer & Bass Boost") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val current = settings
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Changes apply to the playing track. Volume is balanced automatically to reduce distortion.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                EffectSwitch("Enable Equalizer", current.enabled) { enabled ->
                    mainViewModel.updateEqualizerSettings { it.copy(enabled = enabled) }
                }
                Text("Presets", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Normal", "Bass", "Rock", "Pop").forEach { preset ->
                        FilterChip(selected = current.presetName == preset, onClick = {
                            val bands = when (preset) {
                                "Bass" -> shortArrayOf(800, 500, 0, -200, -400)
                                "Rock" -> shortArrayOf(500, 300, -200, 400, 600)
                                "Pop" -> shortArrayOf(-200, 200, 600, 200, -200)
                                else -> shortArrayOf(0, 0, 0, 0, 0)
                            }
                            mainViewModel.updateEqualizerSettings {
                                it.copy(enabled = true, presetName = preset, band1 = bands[0], band2 = bands[1],
                                    band3 = bands[2], band4 = bands[3], band5 = bands[4])
                            }
                        }, label = { Text(preset) })
                    }
                }
                Text("Frequency Bands (dB)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                BandSlider("60 Hz", current.band1, current.enabled) { value ->
                    mainViewModel.updateEqualizerSettings { it.copy(band1 = value, presetName = "Custom") }
                }
                BandSlider("230 Hz", current.band2, current.enabled) { value ->
                    mainViewModel.updateEqualizerSettings { it.copy(band2 = value, presetName = "Custom") }
                }
                BandSlider("910 Hz", current.band3, current.enabled) { value ->
                    mainViewModel.updateEqualizerSettings { it.copy(band3 = value, presetName = "Custom") }
                }
                BandSlider("3.6 kHz", current.band4, current.enabled) { value ->
                    mainViewModel.updateEqualizerSettings { it.copy(band4 = value, presetName = "Custom") }
                }
                BandSlider("14 kHz", current.band5, current.enabled) { value ->
                    mainViewModel.updateEqualizerSettings { it.copy(band5 = value, presetName = "Custom") }
                }
                HorizontalDivider()
                EffectSwitch("Enable Bass Boost", current.bassBoostEnabled) { enabled ->
                    mainViewModel.updateEqualizerSettings { it.copy(bassBoostEnabled = enabled) }
                }
                Text("Strength: ${current.bassBoostStrength.toInt() / 10}%")
                Slider(value = current.bassBoostStrength.toFloat(), enabled = current.bassBoostEnabled,
                    onValueChange = { value ->
                        mainViewModel.updateEqualizerSettings { it.copy(bassBoostStrength = value.toInt().toShort()) }
                    }, valueRange = 0f..1000f,
                    modifier = Modifier.semantics { contentDescription = "Bass strength" },
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
private fun EffectSwitch(label: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Bold)
        Switch(enabled, onChange, modifier = Modifier.semantics { contentDescription = label },
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun BandSlider(label: String, value: Short, enabled: Boolean, onValueChange: (Short) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(String.format(Locale.ROOT, "%.1f dB", value / 100.0))
        }
        Slider(value = value.toFloat(), enabled = enabled,
            onValueChange = { onValueChange(it.toInt().toShort()) }, valueRange = -1500f..1500f,
            modifier = Modifier.semantics { contentDescription = "$label gain" },
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
    }
}
