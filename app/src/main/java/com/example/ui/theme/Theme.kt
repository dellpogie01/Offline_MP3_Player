package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { Dark, Light, System }

private val DarkColorScheme =
    darkColorScheme(
        primary = NeonBlue,
        onPrimary = MidnightBlack,
        primaryContainer = NeonBlueVariant,
        onPrimaryContainer = MidnightBlack,
        secondary = NeonBlueVariant,
        onSecondary = MidnightBlack,
        background = MidnightBlack,
        onBackground = OnSurfaceDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        error = ErrorColor
    )

private val LightColorScheme =
    lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF006878),
        onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFA2EEFF),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001F26),
        secondary = androidx.compose.ui.graphics.Color(0xFF006878),
        background = androidx.compose.ui.graphics.Color(0xFFFAFDFE),
        onBackground = MidnightBlack,
        surface = androidx.compose.ui.graphics.Color(0xFFF0F5F6),
        onSurface = MidnightBlack,
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFDBE4E7),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF3F484B)
    )

@Composable
fun SonicBeatTheme(
    darkTheme: Boolean = true, // Default to dark immersive music player aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for template compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    SonicBeatTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
