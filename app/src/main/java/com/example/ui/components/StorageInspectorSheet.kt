package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Account
import com.example.domain.model.CookieItem
import com.example.domain.model.StorageItem
import com.example.domain.model.StorageType
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageInspectorSheet(
    account: Account,
    cookies: List<CookieItem>,
    localStorageItems: List<StorageItem>,
    sessionStorageItems: List<StorageItem>,
    onAddStorageItem: (StorageType, String, String) -> Unit,
    onDeleteStorageItem: (StorageType, String) -> Unit,
    onClearStorage: (StorageType) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0: LocalStorage, 1: Cookies, 2: SessionStorage
    var newKeyInput by remember { mutableStateOf("") }
    var newValueInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CyberSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("storage_inspector_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Armazenamento",
                        tint = CyanNeon,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Inspetor de Sessão Isolada",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Conta: ${account.name} (${account.id.take(8)}...)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.testTag("close_inspector_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0F172A),
                contentColor = CyanNeon,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyanNeon
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Local Storage (${localStorageItems.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Cookies (${cookies.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Session (${sessionStorageItems.size})", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Local Storage Tab
                    StorageTypeSection(
                        type = StorageType.LOCAL,
                        items = localStorageItems,
                        newKey = newKeyInput,
                        newValue = newValueInput,
                        onKeyChange = { newKeyInput = it },
                        onValueChange = { newValueInput = it },
                        onAdd = {
                            if (newKeyInput.isNotBlank()) {
                                onAddStorageItem(StorageType.LOCAL, newKeyInput.trim(), newValueInput.trim())
                                newKeyInput = ""
                                newValueInput = ""
                            }
                        },
                        onDelete = { key -> onDeleteStorageItem(StorageType.LOCAL, key) },
                        onClear = { onClearStorage(StorageType.LOCAL) }
                    )
                }
                1 -> {
                    // Cookies Tab
                    CookiesSection(cookies = cookies)
                }
                2 -> {
                    // Session Storage Tab
                    StorageTypeSection(
                        type = StorageType.SESSION,
                        items = sessionStorageItems,
                        newKey = newKeyInput,
                        newValue = newValueInput,
                        onKeyChange = { newKeyInput = it },
                        onValueChange = { newValueInput = it },
                        onAdd = {
                            if (newKeyInput.isNotBlank()) {
                                onAddStorageItem(StorageType.SESSION, newKeyInput.trim(), newValueInput.trim())
                                newKeyInput = ""
                                newValueInput = ""
                            }
                        },
                        onDelete = { key -> onDeleteStorageItem(StorageType.SESSION, key) },
                        onClear = { onClearStorage(StorageType.SESSION) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageTypeSection(
    type: StorageType,
    items: List<StorageItem>,
    newKey: String,
    newValue: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Add new item row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = newKey,
                onValueChange = onKeyChange,
                label = { Text("Chave", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("inspector_input_key"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFCBD5E1),
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CyberBorder
                )
            )

            OutlinedTextField(
                value = newValue,
                onValueChange = onValueChange,
                label = { Text("Valor", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("inspector_input_val"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFCBD5E1),
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CyberBorder
                )
            )

            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("inspector_add_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Itens Persistidos (${items.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )

            if (items.isNotEmpty()) {
                Text(
                    text = "Limpar Tudo",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed,
                    modifier = Modifier
                        .clickable(onClick = onClear)
                        .padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum dado salvo no storage desta conta.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.key,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon
                                )
                                Text(
                                    text = item.value,
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }

                            IconButton(
                                onClick = { onDelete(item.key) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CookiesSection(cookies: List<CookieItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Cookies da Conta (${cookies.size}):",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (cookies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum cookie registrado para esta conta.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cookies) { cookie ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cookie.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon
                                )
                                Text(
                                    text = cookie.domain,
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Text(
                                text = cookie.value,
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
