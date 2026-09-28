package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NaveHubDatabase
import com.example.data.repository.NaveHubRepository
import com.example.domain.model.Account
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

    private val platformActiveAccountMap = mutableMapOf<String, String>()

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
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureInitialized()

            val initialPlatforms = repository.getPlatformsSync()
            if (initialPlatforms.isNotEmpty()) {
                val initialPlatId = initialPlatforms.first().id
                _selectedPlatformId.value = initialPlatId
                val accounts = repository.getAccountsForPlatformSync(initialPlatId)
                if (accounts.isNotEmpty()) {
                    val firstAccId = accounts.first().id
                    _selectedAccountId.value = firstAccId
                    platformActiveAccountMap[initialPlatId] = firstAccId
                    refreshActiveAccountData(firstAccId)
                }
            }
        }
    }

    fun selectPlatform(platformId: String) {
        viewModelScope.launch {
            _selectedPlatformId.value = platformId
            val accounts = repository.getAccountsForPlatformSync(platformId)
            val rememberedAccountId = platformActiveAccountMap[platformId]
            val targetAccount = accounts.find { it.id == rememberedAccountId } ?: accounts.firstOrNull()

            if (targetAccount != null) {
                _selectedAccountId.value = targetAccount.id
                platformActiveAccountMap[platformId] = targetAccount.id
                refreshActiveAccountData(targetAccount.id)
            } else if (accounts.isEmpty()) {
                val newAcc = repository.createAccount(platformId, "Conta 1")
                _selectedAccountId.value = newAcc.id
                platformActiveAccountMap[platformId] = newAcc.id
                refreshActiveAccountData(newAcc.id)
            }
        }
    }

    fun selectAccount(accountId: String) {
        viewModelScope.launch {
            _selectedAccountId.value = accountId
            platformActiveAccountMap[_selectedPlatformId.value] = accountId
            refreshActiveAccountData(accountId)
        }
    }

    fun createAccount(platformId: String, customName: String? = null, customUrl: String? = null) {
        viewModelScope.launch {
            val newAcc = repository.createAccount(platformId, customName, customUrl)
            _selectedAccountId.value = newAcc.id
            platformActiveAccountMap[platformId] = newAcc.id
            refreshActiveAccountData(newAcc.id)
        }
    }

    fun updateAccountDetails(accountId: String, newName: String, newUrl: String) {
        viewModelScope.launch {
            repository.updateAccountDetails(accountId, newName, newUrl)
            val webView = webViewPool.getWebView(accountId)
            if (webView != null && newUrl.isNotBlank()) {
                val formattedUrl = if (newUrl.startsWith("http://") || newUrl.startsWith("https://")) newUrl else "https://$newUrl"
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
            if (remaining.isNotEmpty()) {
                val nextAccount = remaining.first()
                _selectedAccountId.value = nextAccount.id
                platformActiveAccountMap[currentPlatId] = nextAccount.id
                refreshActiveAccountData(nextAccount.id)
            } else {
                val newAcc = repository.createAccount(currentPlatId, "Conta 1")
                _selectedAccountId.value = newAcc.id
                platformActiveAccountMap[currentPlatId] = newAcc.id
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

    fun toggleSandboxMode(enabled: Boolean) {
        _isSandboxMode.value = enabled
    }

    fun updateAccountUrl(url: String) {
        val accId = _selectedAccountId.value
        if (accId.isNotBlank()) {
            viewModelScope.launch {
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

    fun setStorageItem(type: StorageType, key: String, value: String) {
        val accId = _selectedAccountId.value
        if (accId.isBlank()) return
        viewModelScope.launch {
            repository.setStorageItem(accId, type, key, value)
            refreshActiveAccountData(accId)
        }
    }

    fun deleteStorageItem(type: StorageType, key: String) {
        val accId = _selectedAccountId.value
        if (accId.isBlank()) return
        viewModelScope.launch {
            repository.deleteStorageItem(accId, type, key)
            refreshActiveAccountData(accId)
        }
    }

    fun clearStorage(type: StorageType) {
        val accId = _selectedAccountId.value
        if (accId.isBlank()) return
        viewModelScope.launch {
            repository.clearStorage(accId, type)
            refreshActiveAccountData(accId)
        }
    }

    fun runIsolationAudit() {
        if (_isAuditRunning.value) return
        viewModelScope.launch {
            _isAuditRunning.value = true
            try {
                val report = auditor.runFullAudit()
                _auditReport.value = report
            } finally {
                _isAuditRunning.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webViewPool.releaseAll()
    }
}
