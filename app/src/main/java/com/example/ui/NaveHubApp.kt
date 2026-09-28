package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.key
import com.example.domain.model.Account
import com.example.ui.components.AccountTabBar
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddPlatformDialog
import com.example.ui.components.DeleteAccountDialog
import com.example.ui.components.EditAccountDialog
import com.example.ui.components.IsolationAuditDialog
import com.example.ui.components.NaveWebViewContainer
import com.example.ui.components.PlatformSidebar
import com.example.ui.components.StorageInspectorSheet
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyanNeon

@Composable
fun NaveHubApp(
    viewModel: NaveHubViewModel
) {
    val platforms by viewModel.platforms.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val selectedPlatformId by viewModel.selectedPlatformId.collectAsStateWithLifecycle()
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()
    val isSandboxMode by viewModel.isSandboxMode.collectAsStateWithLifecycle()
    val isAuditRunning by viewModel.isAuditRunning.collectAsStateWithLifecycle()
    val auditReport by viewModel.auditReport.collectAsStateWithLifecycle()
    val profileDiagnostics by viewModel.profileDiagnostics.collectAsStateWithLifecycle()

    val activeCookies by viewModel.activeCookies.collectAsStateWithLifecycle()
    val activeLocalStorage by viewModel.activeLocalStorage.collectAsStateWithLifecycle()
    val activeSessionStorage by viewModel.activeSessionStorage.collectAsStateWithLifecycle()

    // Filter accounts belonging to current selected platform
    val platformAccounts by remember(allAccounts, selectedPlatformId) {
        derivedStateOf {
            allAccounts.filter { it.platformId == selectedPlatformId }
        }
    }

    // Counts per platform
    val accountCounts by remember(allAccounts) {
        derivedStateOf {
            allAccounts.groupBy { it.platformId }.mapValues { it.value.size }
        }
    }

    val selectedPlatform = platforms.find { it.id == selectedPlatformId }
    val selectedAccount = platformAccounts.find { it.id == selectedAccountId }
        ?: platformAccounts.firstOrNull()

    // Dialog & Sheet States
    var showAddPlatformDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var showAuditDialog by remember { mutableStateOf(false) }
    var showStorageInspector by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = CyberBg
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("navehub_main_screen")
        ) {
            // 1. LEFT REGION: Slim Platform Sidebar
            PlatformSidebar(
                platforms = platforms,
                selectedPlatformId = selectedPlatformId,
                accountCounts = accountCounts,
                onSelectPlatform = { id -> viewModel.selectPlatform(id) },
                onAddPlatformClick = { showAddPlatformDialog = true },
                onOpenAuditClick = {
                    viewModel.refreshActiveAccountData()
                    showAuditDialog = true
                }
            )

            // 2 & 3. RIGHT REGION: Top Bar (Accounts) + Central Area (Web/Content)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // 2. TOP REGION: Account Tabs
                AccountTabBar(
                    platform = selectedPlatform,
                    accounts = platformAccounts,
                    selectedAccountId = selectedAccount?.id ?: "",
                    onSelectAccount = { id -> viewModel.selectAccount(id) },
                    onAddAccountClick = { showAddAccountDialog = true },
                    onEditAccountClick = { account -> accountToEdit = account },
                    onDeleteAccountClick = { account -> accountToDelete = account },
                    onOpenInspectorClick = {
                        viewModel.refreshActiveAccountData()
                        showStorageInspector = true
                    }
                )

                // 3. CENTRAL REGION: Embedded WebView / Navigation Environment with Dedicated WebViews
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF090D16))
                ) {
                    if (selectedAccount != null && selectedPlatform != null) {
                        NaveWebViewContainer(
                            account = selectedAccount,
                            platform = selectedPlatform,
                            webViewPool = viewModel.webViewPool,
                            isSandboxMode = false,
                            onUrlChange = { newUrl -> viewModel.updateAccountUrl(newUrl) },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CyanNeon)
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs & Sheets ---

    if (showAddPlatformDialog) {
        AddPlatformDialog(
            onConfirm = { name, url, color ->
                viewModel.addPlatform(name, url, color)
                showAddPlatformDialog = false
            },
            onDismiss = { showAddPlatformDialog = false }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            platformName = selectedPlatform?.name ?: "Plataforma",
            onConfirm = { customName, customUrl ->
                viewModel.createAccount(
                    platformId = selectedPlatformId,
                    customName = customName.takeIf { it.isNotBlank() },
                    customUrl = customUrl.takeIf { it.isNotBlank() }
                )
                showAddAccountDialog = false
            },
            onDismiss = { showAddAccountDialog = false }
        )
    }

    accountToEdit?.let { account ->
        EditAccountDialog(
            account = account,
            onConfirm = { newName, newUrl ->
                viewModel.updateAccountDetails(account.id, newName, newUrl)
                accountToEdit = null
            },
            onDismiss = { accountToEdit = null }
        )
    }

    accountToDelete?.let { account ->
        DeleteAccountDialog(
            account = account,
            onConfirm = {
                viewModel.deleteAccount(account.id)
                accountToDelete = null
            },
            onDismiss = { accountToDelete = null }
        )
    }

    if (showAuditDialog) {
        IsolationAuditDialog(
            report = auditReport,
            diagnostics = profileDiagnostics,
            isRunning = isAuditRunning,
            onRunAuditClick = { viewModel.runIsolationAudit() },
            onDismissRequest = { showAuditDialog = false }
        )
    }

    if (showStorageInspector && selectedAccount != null) {
        StorageInspectorSheet(
            account = selectedAccount,
            cookies = activeCookies,
            localStorageItems = activeLocalStorage,
            sessionStorageItems = activeSessionStorage,
            onAddStorageItem = { type, key, value ->
                viewModel.setStorageItem(type, key, value)
            },
            onDeleteStorageItem = { type, key ->
                viewModel.deleteStorageItem(type, key)
            },
            onClearStorage = { type ->
                viewModel.clearStorage(type)
            },
            onDismissRequest = { showStorageInspector = false }
        )
    }
}
