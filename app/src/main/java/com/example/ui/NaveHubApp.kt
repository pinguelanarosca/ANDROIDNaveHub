package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Account
import com.example.domain.model.Platform
import com.example.manager.UpdateInfo
import com.example.ui.components.AccountTabBar
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddPlatformDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.BackupDialog
import com.example.ui.components.DeleteAccountDialog
import com.example.ui.components.EditAccountDialog
import com.example.ui.components.EditPlatformDialog
import com.example.ui.components.NaveWebViewContainer
import com.example.ui.components.PlatformAccountsManagerDialog
import com.example.ui.components.PlatformSidebar
import com.example.ui.components.RestoreDialog
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.NaveHubTheme
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
    val currentDayOfYear by viewModel.currentDayOfYear.collectAsStateWithLifecycle()

    val selectedPlatform = platforms.find { it.id == selectedPlatformId }
        ?: platforms.firstOrNull()

    // Accounts for selected platform sorted by VIP level descending (highest VIP on the left)
    val platformAccounts = remember(allAccounts, selectedPlatform?.id) {
        val currentPlatId = selectedPlatform?.id ?: selectedPlatformId
        allAccounts
            .filter { it.platformId == currentPlatId }
            .sortedWith(
                compareByDescending<Account> { viewModel.getVipLevel(it.name) }
                    .thenBy { it.name }
            )
    }

    // Counts per platform
    val accountCounts = remember(allAccounts) {
        allAccounts.groupBy { it.platformId }.mapValues { it.value.size }
    }

    val selectedAccount = platformAccounts.find { it.id == selectedAccountId }
        ?: platformAccounts.firstOrNull()

    // Dialog & Fullscreen States
    var isFullscreen by remember { mutableStateOf(false) }
    var showAddPlatformDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAccountsManager by remember { mutableStateOf(false) }
    var platformToEdit by remember { mutableStateOf<Platform?>(null) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var backupJsonContent by remember { mutableStateOf("") }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }

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
                    LoadingScreen()
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
                // 1. LEFT REGION: Slim Platform Sidebar with Update, Backup & Restore in bottom-left
                PlatformSidebar(
                    platforms = platforms,
                    selectedPlatformId = selectedPlatform?.id ?: selectedPlatformId,
                    accountCounts = accountCounts,
                    allAccounts = allAccounts,
                    onSelectPlatform = { id -> viewModel.selectPlatform(id) },
                    onEditPlatform = { platform -> platformToEdit = platform },
                    onAddPlatformClick = { showAddPlatformDialog = true },
                    onUpdateClick = {
                        showUpdateDialog = true
                        isCheckingUpdate = true
                        scope.launch {
                            updateInfoState = viewModel.checkForAppUpdates()
                            isCheckingUpdate = false
                        }
                    },
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
                    // 2. TOP REGION: Account Tabs (Only accounts on the top bar)
                    AccountTabBar(
                        platform = selectedPlatform,
                        accounts = platformAccounts,
                        selectedAccountId = selectedAccount?.id ?: "",
                        onSelectAccount = { id -> viewModel.selectAccount(id) },
                        onEditAccount = { account -> accountToEdit = account },
                        onAddAccountClick = { showAddAccountDialog = true }
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
                            LoadingScreen()
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showUpdateDialog) {
        AppUpdateDialog(
            isLoading = isCheckingUpdate,
            updateInfo = updateInfoState,
            onDownloadApk = { url ->
                viewModel.openUrl(url)
            },
            onOpenGitHub = { url ->
                viewModel.openUrl(url)
            },
            onDismiss = { showUpdateDialog = false }
        )
    }

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

    platformToEdit?.let { platform ->
        EditPlatformDialog(
            platform = platform,
            onConfirm = { newName, newUrl, newColor ->
                viewModel.updatePlatform(platform.id, newName, newUrl, newColor)
                platformToEdit = null
            },
            onOpenAccountsManager = {
                platformToEdit = null
                showAccountsManager = true
            },
            onDismiss = { platformToEdit = null }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            platformName = selectedPlatform?.name ?: "Plataforma",
            onConfirm = { customName, customUrl ->
                viewModel.createAccount(
                    platformId = selectedPlatform?.id ?: selectedPlatformId,
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

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = CyanNeon,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Carregando NaveHub...",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun NaveHubPreview() {
    val samplePlatforms = listOf(
        Platform("8u", "8U", "https://8u.com", "#00E676"),
        Platform("777", "777", "https://777vipv0.com/#/home", "#FFAB00"),
        Platform("365gg", "365GG", "https://365gg2.com/#/home", "#00B0FF"),
        Platform("93h", "93H", "https://x83yy7.com/main/inicio", "#FF4081")
    )
    val sampleAccounts = listOf(
        Account("8u_3444vip8", "8u", "3444VIP8", "https://8u.com"),
        Account("8u_4202vip7", "8u", "4202VIP7", "https://8u.com"),
        Account("8u_5787vip7", "8u", "5787VIP7", "https://8u.com")
    )
    NaveHubTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = CyberBg
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PlatformSidebar(
                    platforms = samplePlatforms,
                    selectedPlatformId = "8u",
                    accountCounts = mapOf("8u" to 3, "777" to 4, "365gg" to 8, "93h" to 4),
                    allAccounts = sampleAccounts,
                    onSelectPlatform = {},
                    onEditPlatform = {},
                    onAddPlatformClick = {},
                    onUpdateClick = {},
                    onBackupClick = {},
                    onRestoreClick = {}
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    AccountTabBar(
                        platform = samplePlatforms.first(),
                        accounts = sampleAccounts,
                        selectedAccountId = sampleAccounts.first().id,
                        onSelectAccount = {},
                        onEditAccount = {},
                        onAddAccountClick = {}
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF090D16)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Área de Navegação Web", color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}
