package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val HighContrastColorScheme = darkColorScheme(
    primary = Color(0xFF00C6FF),         // Neon bright cyan
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00354A),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF00E5FF),
    onSecondary = Color.Black,
    tertiary = Color(0xFFFFD700),          // Gold
    onTertiary = Color.Black,
    background = Color.Black,             // Pure Black background
    onBackground = Color.White,           // Pure White text
    surface = Color(0xFF121212),          // Very dark slate grey surface
    onSurface = Color.White,              // Pure White text on surface
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFE0E0E0), // Extremely bright grey for high contrast description
    error = Color(0xFFFF3B30),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force our high contrast black theme globally
    content: @Composable () -> Unit,
) {
    // We always use HighContrastColorScheme to ensure black background and white text legibility
    MaterialTheme(colorScheme = HighContrastColorScheme, typography = Typography, content = content)
}
