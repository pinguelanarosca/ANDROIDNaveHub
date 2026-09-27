package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@Composable
fun PlatformSidebar(
    platforms: List<Platform>,
    selectedPlatformId: String,
    accountCounts: Map<String, Int>,
    onSelectPlatform: (String) -> Unit,
    onAddPlatformClick: () -> Unit,
    onOpenAuditClick: () -> Unit,
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

            Spacer(modifier = Modifier.height(12.dp))

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
                        onClick = { onSelectPlatform(platform.id) }
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

            Spacer(modifier = Modifier.height(8.dp))

            // Audit & Info actions at bottom
            IconButton(
                onClick = onOpenAuditClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("open_isolation_audit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Auditoria de Isolamento",
                    tint = CyanNeon,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PlatformItem(
    platform: Platform,
    isSelected: Boolean,
    accentColor: Color,
    accountCount: Int,
    onClick: () -> Unit
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
            .clickable(onClick = onClick)
            .testTag("platform_item_${platform.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
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

            // Account count pill
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
