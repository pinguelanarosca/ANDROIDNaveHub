package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NaveHubColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color(0xFF001E28),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = PurpleAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFDDD6FE),
    background = CyberBg,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun NaveHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NaveHubColorScheme,
        typography = Typography,
        content = content
    )
}
