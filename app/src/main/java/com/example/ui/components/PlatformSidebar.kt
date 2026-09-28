package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Account
import com.example.domain.model.Platform
import com.example.domain.model.isAccessedToday
import com.example.domain.model.isPlatformAllAccessedToday
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyanNeon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlatformSidebar(
    platforms: List<Platform>,
    selectedPlatformId: String,
    accountCounts: Map<String, Int>,
    allAccounts: List<Account>,
    onSelectPlatform: (String) -> Unit,
    onEditPlatform: (Platform) -> Unit,
    onAddPlatformClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(70.dp)
            .fillMaxHeight()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color(0xF20B1325),
                        Color(0xF5070B16),
                        Color(0xFA04070E)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color(0x4038BDF8),
                        Color(0x1A475569),
                        Color(0x308B5CF6)
                    )
                ),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
    ) {
        // Floating translucent particles and subtle ambient texture
        AmbientSidebarBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp, horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo (Transparent, free-floating, no visible box or border)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .testTag("navehub_logo_badge"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "NaveHub Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable platform list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                platforms.forEach { platform ->
                    val isSelected = platform.id == selectedPlatformId
                    val accentColor = try {
                        Color(android.graphics.Color.parseColor(platform.accentColorHex))
                    } catch (e: Exception) {
                        CyanNeon
                    }
                    val platAccounts = allAccounts.filter { it.platformId == platform.id }
                    // Linha verde até todas as contas terem sido acessadas no dia de hoje;
                    // Linha cinza quando todas as contas dentro da plataforma já tiverem sido acessadas hoje.
                    val isAllAccessedToday = isPlatformAllAccessedToday(platAccounts)

                    PlatformItem(
                        platform = platform,
                        isSelected = isSelected,
                        accentColor = accentColor,
                        isAllAccessedToday = isAllAccessedToday,
                        onClick = { onSelectPlatform(platform.id) },
                        onLongClick = { onEditPlatform(platform) }
                    )
                }

                // Add Platform Button (Glassmorphic)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color(0x301E293B), Color(0x180F172A))
                            )
                        )
                        .border(1.dp, Color(0x33475569), RoundedCornerShape(12.dp))
                        .clickable(onClick = onAddPlatformClick)
                        .testTag("add_platform_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Plataforma",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom-Left Actions: Update, Backup & Restore Buttons (Translucent neon glass)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Update App Button (Verificar atualização do APK no GitHub)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color(0x3500E676), Color(0x10064E3B))
                            )
                        )
                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(11.dp))
                        .clickable(onClick = onUpdateClick)
                        .testTag("sidebar_update_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Verificar Atualização do APK",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Backup Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color(0x3500F0FF), Color(0x10082F49))
                            )
                        )
                        .border(1.dp, CyanNeon.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                        .clickable(onClick = onBackupClick)
                        .testTag("sidebar_backup_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Fazer Backup Completo",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Restore Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color(0x35FFB703), Color(0x10451A03))
                            )
                        )
                        .border(1.dp, Color(0xFFFFB703).copy(alpha = 0.5f), RoundedCornerShape(11.dp))
                        .clickable(onClick = onRestoreClick)
                        .testTag("sidebar_restore_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = "Restaurar Backup (Sobrescrever)",
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlatformItem(
    platform: Platform,
    isSelected: Boolean,
    accentColor: Color,
    isAllAccessedToday: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    // Verde caso alguma conta dentro dela ainda não tenha sido acessada hoje;
    // Cinza caso todas as contas dentro dela já tenham sido acessadas no dia de hoje.
    val statusColor = if (!isAllAccessedToday) Color(0xFF00E676) else Color(0xFF64748B)

    val itemBrush = if (isSelected) {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(
                accentColor.copy(alpha = 0.32f),
                accentColor.copy(alpha = 0.12f)
            )
        )
    } else {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(
                Color(0x281E293B),
                Color(0x120F172A)
            )
        )
    }

    val borderColor = if (isSelected) accentColor else Color(0x33475569)

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .background(itemBrush)
            .border(if (isSelected) 1.8.dp else 1.dp, borderColor, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("platform_item_${platform.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Text(
                text = platform.name,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                fontSize = if (platform.name.length > 4) 11.sp else 13.sp,
                color = if (isSelected) accentColor else Color(0xFFF1F5F9),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Linha indicadora diretamente abaixo do texto nome da plataforma
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(statusColor)
                    .testTag("platform_status_line_${platform.id}")
            )
        }
    }
}
