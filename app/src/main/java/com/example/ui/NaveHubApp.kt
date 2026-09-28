package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Account
import com.example.ui.components.AccountTabBar
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddPlatformDialog
import com.example.ui.components.BackupDialog
import com.example.ui.components.DeleteAccountDialog
import com.example.ui.components.EditAccountDialog
import com.example.ui.components.NaveWebViewContainer
import com.example.ui.components.PlatformAccountsManagerDialog
import com.example.ui.components.PlatformSidebar
import com.example.ui.components.RestoreDialog
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyanNeon
import kotlinx.coroutines.launch

@Composable
fun NaveHubApp(
    viewModel: NaveHubViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val platforms by viewModel.platforms.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val selectedPlatformId by viewModel.selectedPlatformId.collectAsStateWithLifecycle()
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()
    val isSandboxMode by viewModel.isSandboxMode.collectAsStateWithLifecycle()

    // Accounts for selected platform sorted by VIP level descending (highest VIP on the left)
    val platformAccounts = remember(allAccounts, selectedPlatformId) {
        allAccounts
            .filter { it.platformId == selectedPlatformId }
            .sortedWith(
                compareByDescending<Account> { viewModel.getVipLevel(it.name) }
                    .thenBy { it.name }
            )
    }

    // Counts per platform
    val accountCounts = remember(allAccounts) {
        allAccounts.groupBy { it.platformId }.mapValues { it.value.size }
    }

    val selectedPlatform = platforms.find { it.id == selectedPlatformId }
    val selectedAccount = platformAccounts.find { it.id == selectedAccountId }
        ?: platformAccounts.firstOrNull()

    // Dialog & Fullscreen States
    var isFullscreen by remember { mutableStateOf(false) }
    var showAddPlatformDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAccountsManager by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var backupJsonContent by remember { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = CyberBg
    ) { paddingValues ->
        if (isFullscreen) {
            // Fullscreen mode
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF090D16))
                    .testTag("fullscreen_webview_container")
            ) {
                if (selectedAccount != null && selectedPlatform != null) {
                    NaveWebViewContainer(
                        account = selectedAccount,
                        platform = selectedPlatform,
                        webViewPool = viewModel.webViewPool,
                        isSandboxMode = isSandboxMode,
                        isFullscreen = true,
                        onToggleFullscreen = { isFullscreen = it },
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
        } else {
            // Normal mode
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("navehub_main_screen")
            ) {
                // 1. LEFT REGION: Slim Platform Sidebar with Backup & Restore in bottom-left
                PlatformSidebar(
                    platforms = platforms,
                    selectedPlatformId = selectedPlatformId,
                    accountCounts = accountCounts,
                    onSelectPlatform = { id -> viewModel.selectPlatform(id) },
                    onAddPlatformClick = { showAddPlatformDialog = true },
                    onBackupClick = {
                        scope.launch {
                            backupJsonContent = viewModel.generateBackupJson()
                            showBackupDialog = true
                        }
                    },
                    onRestoreClick = {
                        showRestoreDialog = true
                    }
                )

                // 2 & 3. RIGHT REGION: Top Bar (Accounts) + Central Area (Web/Content)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // 2. TOP REGION: Account Tabs (Sorted by VIP level)
                    AccountTabBar(
                        platform = selectedPlatform,
                        accounts = platformAccounts,
                        selectedAccountId = selectedAccount?.id ?: "",
                        onSelectAccount = { id -> viewModel.selectAccount(id) },
                        onAddAccountClick = { showAddAccountDialog = true },
                        onOpenManagerClick = { showAccountsManager = true }
                    )

                    // 3. CENTRAL REGION: Embedded WebView Container
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
                                isSandboxMode = isSandboxMode,
                                isFullscreen = false,
                                onToggleFullscreen = { isFullscreen = it },
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
    }

    // --- Dialogs ---

    if (showBackupDialog) {
        BackupDialog(
            backupJson = backupJsonContent,
            platformCount = platforms.size,
            accountCount = allAccounts.size,
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showRestoreDialog) {
        RestoreDialog(
            onConfirmRestore = { json ->
                scope.launch {
                    val result = viewModel.restoreFromBackup(json)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Backup restaurado com sucesso! Dados sobrescritos.", Toast.LENGTH_LONG).show()
                        showRestoreDialog = false
                    } else {
                        Toast.makeText(context, "Erro na restauração: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { showRestoreDialog = false }
        )
    }

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

    if (showAccountsManager && selectedPlatform != null) {
        PlatformAccountsManagerDialog(
            platform = selectedPlatform,
            accounts = platformAccounts,
            onAddAccount = {
                showAccountsManager = false
                showAddAccountDialog = true
            },
            onEditAccount = { account ->
                accountToEdit = account
            },
            onDeleteAccount = { account ->
                accountToDelete = account
            },
            onDismiss = { showAccountsManager = false }
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
}
