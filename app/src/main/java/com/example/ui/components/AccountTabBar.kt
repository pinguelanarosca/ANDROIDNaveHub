package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Account
import com.example.domain.model.Platform
import com.example.domain.model.isAccessedToday
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyanNeon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountTabBar(
    platform: Platform?,
    accounts: List<Account>,
    selectedAccountId: String,
    onSelectAccount: (String) -> Unit,
    onEditAccount: (Account) -> Unit,
    onAddAccountClick: () -> Unit,
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
            .height(54.dp),
        color = CyberSurface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable tabs of accounts (Platform indicator removed per request: only accounts remain)
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
                        onClick = { onSelectAccount(account.id) },
                        onLongClick = { onEditAccount(account) }
                    )
                }

                // [+] Add Account button
                Box(
                    modifier = Modifier
                        .height(40.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountTabItem(
    account: Account,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isOpenedToday = isAccessedToday(account.lastActiveTimestamp)
    // Verde se esta conta não foi aberta no dia de hoje; CINZA se já foi acessada hoje
    val statusLineColor = if (isOpenedToday) Color(0xFF64748B) else Color(0xFF00E676)

    val shape = RoundedCornerShape(8.dp)
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF131B2E)
    val borderColor = if (isSelected) accentColor else Color(0xFF1E293B)

    Box(
        modifier = Modifier
            .height(42.dp)
            .clip(shape)
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("account_tab_${account.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = account.name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Linha indicadora abaixo do título da conta
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(statusLineColor)
                    .testTag("account_status_line_${account.id}")
            )
        }
    }
}
