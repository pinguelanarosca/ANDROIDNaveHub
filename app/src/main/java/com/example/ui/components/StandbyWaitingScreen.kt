package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Account
import com.example.domain.model.Platform
import com.example.ui.theme.CyanNeon
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tela de espera / standby animada, fluida e de altíssima performance.
 * Animações lidas exclusivamente na fase de desenho (Draw Phase) para ZERO recomposição do Compose.
 */
@Composable
fun StandbyWaitingScreen(
    account: Account,
    platform: Platform,
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = remember(platform.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(platform.accentColorHex))
        } catch (e: Exception) {
            CyanNeon
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "standby_anim")

    // State holders - NÃO ler com "by" no corpo do Composable para evitar recomposição a cada frame
    val rotationState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val reverseRotationState = infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rev_rotation"
    )

    val pulseScaleState = infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val auraAlphaState = infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura"
    )

    val bgBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF0F172C),
                Color(0xFF090E1B),
                Color(0xFF050812)
            ),
            radius = 1200f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .testTag("standby_waiting_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Animação central (Lida apenas no drawScope do Canvas)
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rot = rotationState.value
                    val revRot = reverseRotationState.value
                    val pulse = pulseScaleState.value
                    val aura = auraAlphaState.value

                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = (size.minDimension / 2f) - 14f

                    // 1. Aura de brilho difuso
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = aura * 0.30f),
                                CyanNeon.copy(alpha = aura * 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseRadius * 1.2f
                        ),
                        center = center,
                        radius = baseRadius * 1.2f
                    )

                    // 2. Anel externo com partículas orbitantes
                    val outerRadius = baseRadius * pulse
                    drawCircle(
                        color = CyanNeon.copy(alpha = 0.30f),
                        center = center,
                        radius = outerRadius,
                        style = Stroke(width = 1.5f)
                    )

                    val radOuter = Math.toRadians(rot.toDouble())
                    val p1X = center.x + (outerRadius * cos(radOuter)).toFloat()
                    val p1Y = center.y + (outerRadius * sin(radOuter)).toFloat()
                    drawCircle(
                        color = CyanNeon,
                        radius = 4f,
                        center = Offset(p1X, p1Y)
                    )

                    val p2X = center.x + (outerRadius * cos(radOuter + Math.PI)).toFloat()
                    val p2Y = center.y + (outerRadius * sin(radOuter + Math.PI)).toFloat()
                    drawCircle(
                        color = accentColor,
                        radius = 3.5f,
                        center = Offset(p2X, p2Y)
                    )

                    // 3. Anel intermediário
                    val midRadius = baseRadius * 0.76f
                    drawCircle(
                        color = accentColor.copy(alpha = 0.35f),
                        center = center,
                        radius = midRadius,
                        style = Stroke(width = 1.8f)
                    )

                    val radMid = Math.toRadians(revRot.toDouble())
                    val p3X = center.x + (midRadius * cos(radMid)).toFloat()
                    val p3Y = center.y + (midRadius * sin(radMid)).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(p3X, p3Y)
                    )
                }

                // Núcleo central holográfico
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x701E293B),
                                    Color(0x900F172A)
                                )
                            )
                        )
                        .border(1.5.dp, accentColor.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Standby NaveHub",
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Cartão de identificação da conta
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x351E293B),
                                Color(0x180F172A)
                            )
                        )
                    )
                    .border(1.dp, Color(0x3538BDF8), RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Text(
                            text = "${platform.name} • ${account.name}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Ambiente Isolado",
                            tint = CyanNeon,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Ambiente e Sessão Isolados • Pronto",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botão Iniciar Navegação (Clique com feedback instantâneo)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                CyanNeon,
                                Color(0xFF00B0FF)
                            )
                        )
                    )
                    .clickable(onClick = onStartNavigation)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("standby_start_navigation_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Iniciar Navegação",
                        tint = Color(0xFF001E28),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Iniciar Navegação",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF001E28)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF001E28),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Ou toque na barra de endereços no topo para digitar qualquer URL",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}
