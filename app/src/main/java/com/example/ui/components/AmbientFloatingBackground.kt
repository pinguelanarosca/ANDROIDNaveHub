package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Ultra-lightweight static ambient texture and translucent glowing elements.
 * Designed for high performance and zero CPU overhead on emulator environments.
 */
@Composable
fun AmbientSidebarBackground(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00F0FF)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Subtle top glowing ambient orb
        val orb1Center = Offset(width * 0.5f, height * 0.2f)
        val orb1Radius = width * 1.2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.12f),
                    accentColor.copy(alpha = 0.03f),
                    Color.Transparent
                ),
                center = orb1Center,
                radius = orb1Radius
            ),
            radius = orb1Radius,
            center = orb1Center
        )

        // 2. Subtle bottom glowing ambient orb (Violet)
        val orb2Center = Offset(width * 0.5f, height * 0.8f)
        val orb2Radius = width * 1.3f
        val secondaryColor = Color(0xFF8B5CF6)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.10f),
                    secondaryColor.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = orb2Center,
                radius = orb2Radius
            ),
            radius = orb2Radius,
            center = orb2Center
        )

        // 3. Subtle floating translucent particles
        drawCircle(
            color = Color(0x3538BDF8),
            radius = 1.8.dp.toPx(),
            center = Offset(width * 0.35f, height * 0.35f)
        )
        drawCircle(
            color = Color(0x4000E676),
            radius = 1.5.dp.toPx(),
            center = Offset(width * 0.65f, height * 0.55f)
        )
        drawCircle(
            color = Color(0x35A855F7),
            radius = 2.0.dp.toPx(),
            center = Offset(width * 0.40f, height * 0.75f)
        )
    }
}

/**
 * Ultra-lightweight static ambient texture for horizontal account tab bar.
 */
@Composable
fun AmbientTopBarBackground(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00F0FF)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Translucent glowing gradient beam across the bar
        val beam1Center = Offset(width * 0.3f, height * 0.5f)
        val beam1Radius = height * 2.5f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.12f),
                    accentColor.copy(alpha = 0.03f),
                    Color.Transparent
                ),
                center = beam1Center,
                radius = beam1Radius
            ),
            radius = beam1Radius,
            center = beam1Center
        )

        val beam2Center = Offset(width * 0.75f, height * 0.5f)
        val secondaryGlow = Color(0xFF8B5CF6)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryGlow.copy(alpha = 0.10f),
                    secondaryGlow.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = beam2Center,
                radius = beam1Radius
            ),
            radius = beam1Radius,
            center = beam2Center
        )

        // 2. Subtle translucent particles
        drawCircle(
            color = Color(0x3538BDF8),
            radius = 1.5.dp.toPx(),
            center = Offset(width * 0.2f, height * 0.35f)
        )
        drawCircle(
            color = Color(0x30A855F7),
            radius = 1.5.dp.toPx(),
            center = Offset(width * 0.6f, height * 0.65f)
        )
    }
}
