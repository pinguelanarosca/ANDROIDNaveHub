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
import com.example.domain.model.StorageItem
import com.example.domain.model.StorageType
import com.example.manager.IsolationAuditor
import com.example.manager.SessionIsolationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NaveHubUiState(
    val platforms: List<Platform> = emptyList(),
    val selectedPlatformId: String = "8u",
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: String = "",
    val isSandboxMode: Boolean = true,
    val isAuditRunning: Boolean = false,
    val auditReport: IsolationAuditReport? = null,
    val activeCookies: List<CookieItem> = emptyList(),
    val activeLocalStorage: List<StorageItem> = emptyList(),
    val activeSessionStorage: List<StorageItem> = emptyList(),
    val accountCounts: Map<String, Int> = emptyMap()
)

class NaveHubViewModel(application: Application) : AndroidViewModel(application) {

    val database: NaveHubDatabase = NaveHubDatabase.getDatabase(application, viewModelScope)
    val repository: NaveHubRepository = NaveHubRepository(database)
    val isolationManager: SessionIsolationManager = SessionIsolationManager(application, repository, viewModelScope)
    val auditor: IsolationAuditor = IsolationAuditor(repository, isolationManager)

    private val _selectedPlatformId = MutableStateFlow("8u")
    val selectedPlatformId: StateFlow<String> = _selectedPlatformId.asStateFlow()

    private val _selectedAccountId = MutableStateFlow("")
    val selectedAccountId: StateFlow<String> = _selectedAccountId.asStateFlow()

    private val _isSandboxMode = MutableStateFlow(true)
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

    // Remember last selected account per platform
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

            // Observe platforms and setup initial selection
            val initialPlatforms = repository.getPlatformsSync()
            if (initialPlatforms.isNotEmpty()) {
                val initialPlatId = initialPlatforms.first().id
                _selectedPlatformId.value = initialPlatId
                val accounts = repository.getAccountsForPlatformSync(initialPlatId)
                if (accounts.isNotEmpty()) {
                    _selectedAccountId.value = accounts.first().id
                    platformActiveAccountMap[initialPlatId] = accounts.first().id
                    refreshActiveAccountData(accounts.first().id)
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
                // Auto create account 1 if somehow empty
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

    fun createAccount(platformId: String, customName: String? = null) {
        viewModelScope.launch {
            val newAcc = repository.createAccount(platformId, customName)
            _selectedAccountId.value = newAcc.id
            platformActiveAccountMap[platformId] = newAcc.id
            refreshActiveAccountData(newAcc.id)
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
            repository.deleteAccount(accountId)

            val remaining = repository.getAccountsForPlatformSync(currentPlatId)
            if (remaining.isNotEmpty()) {
                val nextAccount = remaining.first()
                _selectedAccountId.value = nextAccount.id
                platformActiveAccountMap[currentPlatId] = nextAccount.id
                refreshActiveAccountData(nextAccount.id)
            } else {
                // If all deleted, recreate default
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
}
