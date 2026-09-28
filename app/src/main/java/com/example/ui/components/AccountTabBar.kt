package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Account
import com.example.domain.model.Platform
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyanNeon

@Composable
fun AccountTabBar(
    platform: Platform?,
    accounts: List<Account>,
    selectedAccountId: String,
    onSelectAccount: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onOpenManagerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = try {
        if (platform != null) Color(android.graphics.Color.parseColor(platform.accentColorHex)) else CyanNeon
    } catch (e: Exception) {
        CyanNeon
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        color = CyberSurface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Platform Pill & Manage/Edit All Accounts Button
            if (platform != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("platform_indicator_${platform.id}")
                ) {
                    Text(
                        text = platform.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onOpenManagerClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("open_accounts_manager_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Gerenciar e Editar Contas da Plataforma",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Scrollable tabs of accounts
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                accounts.forEach { account ->
                    val isSelected = account.id == selectedAccountId
                    AccountTabItem(
                        account = account,
                        isSelected = isSelected,
                        accentColor = accentColor,
                        onClick = { onSelectAccount(account.id) }
                    )
                }

                // [+] Add Account button
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onAddAccountClick)
                        .padding(horizontal = 10.dp)
                        .testTag("add_account_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nova Conta",
                            tint = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Conta",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTabItem(
    account: Account,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF131B2E)
    val borderColor = if (isSelected) accentColor else Color(0xFF1E293B)

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(shape)
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag("account_tab_${account.id}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = account.name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Removes leading numeric counters/identifiers such as "163440VIP1" -> "VIP1",
 * "13444VIP8" -> "VIP8", "1 - Conta" -> "Conta", etc.
 */
internal fun formatAccountTabTitle(rawName: String): String {
    val trimmed = rawName.trim()
    val stripped = trimmed.replaceFirst(Regex("^\\d+[\\s._-]*"), "")
    return if (stripped.isNotBlank()) stripped else trimmed
}
