package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Platform
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyanNeon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlatformSidebar(
    platforms: List<Platform>,
    selectedPlatformId: String,
    accountCounts: Map<String, Int>,
    onSelectPlatform: (String) -> Unit,
    onEditPlatform: (Platform) -> Unit,
    onAddPlatformClick: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(68.dp)
            .fillMaxHeight(),
        color = CyberSurface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, CyanNeon.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .testTag("navehub_logo_badge"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NH",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = CyanNeon
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
                    val count = accountCounts[platform.id] ?: 0

                    PlatformItem(
                        platform = platform,
                        isSelected = isSelected,
                        accentColor = accentColor,
                        accountCount = count,
                        onClick = { onSelectPlatform(platform.id) },
                        onLongClick = { onEditPlatform(platform) }
                    )
                }

                // Add Platform Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
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

            // Bottom-Left Actions: Backup & Restore Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Backup Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onRestoreClick)
                        .testTag("sidebar_restore_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = "Restaurar Backup (Sobrescrever)",
                        tint = Color(0xFFFFB300),
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
    accountCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.22f) else Color(0xFF131B2E)
    val borderColor = if (isSelected) accentColor else Color(0xFF1E293B)

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(shape)
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
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
                .padding(horizontal = 2.dp)
        ) {
            Text(
                text = platform.name,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                fontSize = if (platform.name.length > 4) 10.sp else 12.sp,
                color = if (isSelected) accentColor else Color(0xFFE2E8F0),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Account count badge
            if (accountCount > 0) {
                Text(
                    text = "$accountCount",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) accentColor else Color(0xFF64748B)
                )
            }
        }
    }
}
