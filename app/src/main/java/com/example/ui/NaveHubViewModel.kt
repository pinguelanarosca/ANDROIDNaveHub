package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NaveHubDatabase
import com.example.data.repository.NaveHubRepository
import com.example.domain.model.Account
import com.example.domain.model.BackupPayload
import com.example.domain.model.CookieItem
import com.example.domain.model.IsolationAuditReport
import com.example.domain.model.Platform
import com.example.domain.model.ProfileDiagnostics
import com.example.domain.model.StorageItem
import com.example.domain.model.StorageType
import com.example.manager.AccountWebViewPool
import com.example.manager.IsolationAuditor
import com.example.manager.NativeProfileManager
import com.example.manager.SessionIsolationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NaveHubViewModel(application: Application) : AndroidViewModel(application) {

    val database: NaveHubDatabase = NaveHubDatabase.getDatabase(application, viewModelScope)
    val repository: NaveHubRepository = NaveHubRepository(database)
    val nativeProfileManager: NativeProfileManager = NativeProfileManager(application)
    val isolationManager: SessionIsolationManager = SessionIsolationManager(
        application,
        repository,
        viewModelScope,
        nativeProfileManager
    )
    val auditor: IsolationAuditor = IsolationAuditor(
        application,
        repository,
        isolationManager,
        nativeProfileManager
    )
    val webViewPool: AccountWebViewPool = AccountWebViewPool(
        application,
        nativeProfileManager,
        isolationManager
    )

    private val prefs = application.getSharedPreferences("navehub_platform_state", Context.MODE_PRIVATE)

    private val _selectedPlatformId = MutableStateFlow("8u")
    val selectedPlatformId: StateFlow<String> = _selectedPlatformId.asStateFlow()

    private val _selectedAccountId = MutableStateFlow("")
    val selectedAccountId: StateFlow<String> = _selectedAccountId.asStateFlow()

    private val _isSandboxMode = MutableStateFlow(false)
    val isSandboxMode: StateFlow<Boolean> = _isSandboxMode.asStateFlow()

    private val _isAuditRunning = MutableStateFlow(false)
    val isAuditRunning: StateFlow<Boolean> = _isAuditRunning.asStateFlow()

    private val _auditReport = MutableStateFlow<IsolationAuditReport?>(null)
    val auditReport: StateFlow<IsolationAuditReport?> = _auditReport.asStateFlow()

    private val _activeCookies = MutableStateFlow<List<CookieItem>>(emptyList())
    val activeCookies: StateFlow<List<CookieItem>> = _activeCookies.asStateFlow()

    private val _activeLocalStorage = MutableStateFlow<List<StorageItem>>(emptyList())
    val activeLocalStorage: StateFlow<List<StorageItem>> = _activeLocalStorage.asStateFlow()

    private val _activeSessionStorage = MutableStateFlow<List<StorageItem>>(emptyList())
    val activeSessionStorage: StateFlow<List<StorageItem>> = _activeSessionStorage.asStateFlow()

    private val _profileDiagnostics = MutableStateFlow<ProfileDiagnostics?>(null)
    val profileDiagnostics: StateFlow<ProfileDiagnostics?> = _profileDiagnostics.asStateFlow()

    private val _currentDayOfYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR))
    val currentDayOfYear: StateFlow<Int> = _currentDayOfYear.asStateFlow()

    val platforms: StateFlow<List<Platform>> = repository.allPlatforms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val allAccounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    init {
        // Periodic check to detect midnight transition and trigger recomposition
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10_000)
                val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
                if (today != _currentDayOfYear.value) {
                    _currentDayOfYear.value = today
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureInitialized()

            val resetDone = prefs.getBoolean("daily_access_tracker_init_v2", false)
            if (!resetDone) {
                repository.resetAllAccountsLastActive()
                prefs.edit().putBoolean("daily_access_tracker_init_v2", true).apply()
            }

            val initialPlatforms = repository.getPlatformsSync()
            if (initialPlatforms.isNotEmpty()) {
                val lastSelectedPlatform = prefs.getString("last_platform_id", initialPlatforms.first().id)
                    ?: initialPlatforms.first().id
                val validPlatId = if (initialPlatforms.any { it.id == lastSelectedPlatform }) {
                    lastSelectedPlatform
                } else {
                    initialPlatforms.first().id
                }

                _selectedPlatformId.value = validPlatId

                val accounts = repository.getAccountsForPlatformSync(validPlatId)
                    .sortedWith(compareByDescending<Account> { getVipLevel(it.name) }.thenBy { it.name })

                if (accounts.isNotEmpty()) {
                    val rememberedAccId = prefs.getString("last_account_$validPlatId", null)
                    val targetAcc = accounts.find { it.id == rememberedAccId } ?: accounts.first()
                    _selectedAccountId.value = targetAcc.id
                    refreshActiveAccountData(targetAcc.id)
                    markAccountAccessed(targetAcc.id)
                }
            }
        }
    }

    fun getVipLevel(name: String): Int {
        val match = Regex("VIP(\\d+)", RegexOption.IGNORE_CASE).find(name)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    fun selectPlatform(platformId: String) {
        _selectedPlatformId.value = platformId
        prefs.edit().putString("last_platform_id", platformId).apply()

        val platformAccounts = allAccounts.value
            .filter { it.platformId == platformId }
            .sortedWith(compareByDescending<Account> { getVipLevel(it.name) }.thenBy { it.name })

        val rememberedAccId = prefs.getString("last_account_$platformId", null)
        val immediateTarget = platformAccounts.find { it.id == rememberedAccId } ?: platformAccounts.firstOrNull()

        if (immediateTarget != null) {
            _selectedAccountId.value = immediateTarget.id
            refreshActiveAccountData(immediateTarget.id)
            markAccountAccessed(immediateTarget.id)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val dbAccounts = repository.getAccountsForPlatformSync(platformId)
                .sortedWith(compareByDescending<Account> { getVipLevel(it.name) }.thenBy { it.name })

            if (dbAccounts.isNotEmpty()) {
                val dbTarget = dbAccounts.find { it.id == rememberedAccId } ?: dbAccounts.first()
                if (_selectedAccountId.value != dbTarget.id && immediateTarget == null) {
                    _selectedAccountId.value = dbTarget.id
                    prefs.edit().putString("last_account_$platformId", dbTarget.id).apply()
                    refreshActiveAccountData(dbTarget.id)
                    markAccountAccessed(dbTarget.id)
                }
            } else {
                val newAcc = repository.createAccount(platformId, "VIP1")
                _selectedAccountId.value = newAcc.id
                prefs.edit().putString("last_account_$platformId", newAcc.id).apply()
                refreshActiveAccountData(newAcc.id)
                markAccountAccessed(newAcc.id)
            }
        }
    }

    fun selectAccount(accountId: String) {
        _selectedAccountId.value = accountId
        prefs.edit().putString("last_account_${_selectedPlatformId.value}", accountId).apply()
        refreshActiveAccountData(accountId)
        markAccountAccessed(accountId)
    }

    fun markAccountAccessed(accountId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAccountAccessed(accountId, System.currentTimeMillis())
        }
    }

    fun createAccount(platformId: String, customName: String? = null, customUrl: String? = null) {
        viewModelScope.launch {
            val newAcc = repository.createAccount(platformId, customName, customUrl)
            _selectedAccountId.value = newAcc.id
            prefs.edit().putString("last_account_$platformId", newAcc.id).apply()
            refreshActiveAccountData(newAcc.id)
            markAccountAccessed(newAcc.id)
        }
    }

    fun updateAccountDetails(accountId: String, newName: String, newUrl: String) {
        viewModelScope.launch {
            repository.updateAccountDetails(accountId, newName, newUrl)
            val webView = webViewPool.getWebView(accountId)
            if (webView != null && newUrl.isNotBlank()) {
                val formattedUrl = if (newUrl.startsWith("http://", ignoreCase = true) || newUrl.startsWith("https://", ignoreCase = true)) {
                    newUrl
                } else {
                    "https://$newUrl"
                }
                webView.loadUrl(formattedUrl)
            }
        }
    }

    fun renameAccount(accountId: String, newName: String) {
        viewModelScope.launch {
            repository.updateAccountName(accountId, newName)
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            val currentPlatId = _selectedPlatformId.value
            webViewPool.releaseAccountWebView(accountId)
            nativeProfileManager.deleteProfile(accountId)
            repository.deleteAccount(accountId)

            val remaining = repository.getAccountsForPlatformSync(currentPlatId)
                .sortedWith(compareByDescending<Account> { getVipLevel(it.name) }.thenBy { it.name })

            if (remaining.isNotEmpty()) {
                val nextAccount = remaining.first()
                _selectedAccountId.value = nextAccount.id
                prefs.edit().putString("last_account_$currentPlatId", nextAccount.id).apply()
                refreshActiveAccountData(nextAccount.id)
            } else {
                val newAcc = repository.createAccount(currentPlatId, "VIP1")
                _selectedAccountId.value = newAcc.id
                prefs.edit().putString("last_account_$currentPlatId", newAcc.id).apply()
                refreshActiveAccountData(newAcc.id)
            }
        }
    }

    fun addPlatform(name: String, url: String, colorHex: String) {
        viewModelScope.launch {
            val platform = repository.addPlatform(name, url, colorHex)
            selectPlatform(platform.id)
        }
    }

    fun updatePlatform(platformId: String, name: String, url: String, colorHex: String) {
        viewModelScope.launch {
            repository.updatePlatform(platformId, name, url, colorHex)
        }
    }

    fun toggleSandboxMode(enabled: Boolean) {
        _isSandboxMode.value = enabled
    }

    fun updateAccountUrl(url: String) {
        val accId = _selectedAccountId.value
        if (accId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateAccountUrl(accId, url)
            }
        }
    }

    fun refreshActiveAccountData(accountId: String = _selectedAccountId.value) {
        if (accountId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _activeCookies.value = repository.getCookiesForAccountSync(accountId)
            _activeLocalStorage.value = repository.getStorageForAccountSync(accountId, StorageType.LOCAL)
            _activeSessionStorage.value = repository.getStorageForAccountSync(accountId, StorageType.SESSION)
            _profileDiagnostics.value = webViewPool.getDiagnostics(accountId)
        }
    }

    // --- Backup & Restore Features ---

    suspend fun generateBackupJson(): String = withContext(Dispatchers.IO) {
        val currentPlatforms = repository.getPlatformsSync()
        val currentAccounts = repository.allAccountsSync()

        val allPrefs = prefs.all.mapValues { it.value.toString() }

        val payload = BackupPayload(
            platforms = currentPlatforms,
            accounts = currentAccounts,
            preferences = allPrefs
        )
        payload.toJsonString()
    }

    suspend fun restoreFromBackup(jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = BackupPayload.fromJsonString(jsonString)

            if (payload.platforms.isEmpty() || payload.accounts.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("O arquivo de backup está vazio ou é inválido."))
            }

            // Release all active webviews
            withContext(Dispatchers.Main) {
                webViewPool.releaseAll()
            }

            // Clean and overwrite database completely
            repository.overwriteAllData(payload.platforms, payload.accounts)

            // Overwrite preferences
            val editor = prefs.edit().clear()
            payload.preferences.forEach { (k, v) ->
                editor.putString(k, v)
            }
            editor.apply()

            // Re-select first platform and account
            val firstPlatform = payload.platforms.first()
            val firstAccounts = payload.accounts
                .filter { it.platformId == firstPlatform.id }
                .sortedWith(compareByDescending<Account> { getVipLevel(it.name) }.thenBy { it.name })

            val targetAccount = firstAccounts.firstOrNull() ?: payload.accounts.first()

            withContext(Dispatchers.Main) {
                _selectedPlatformId.value = firstPlatform.id
                _selectedAccountId.value = targetAccount.id
                refreshActiveAccountData(targetAccount.id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        webViewPool.releaseAll()
    }
}
