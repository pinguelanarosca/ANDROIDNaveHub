package com.example.data.repository

import com.example.data.local.NaveHubDatabase
import com.example.data.local.entity.AccountCookieEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountStorageEntity
import com.example.data.local.entity.PlatformEntity
import com.example.domain.model.Account
import com.example.domain.model.CookieItem
import com.example.domain.model.Platform
import com.example.domain.model.StorageItem
import com.example.domain.model.StorageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class NaveHubRepository(private val database: NaveHubDatabase) {

    private val platformDao = database.platformDao()
    private val accountDao = database.accountDao()
    private val sessionDao = database.accountSessionDao()

    suspend fun ensureInitialized() {
        NaveHubDatabase.populateInitialData(database)
    }

    // --- Platforms ---
    val allPlatforms: Flow<List<Platform>> = platformDao.getAllPlatforms().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getPlatformsSync(): List<Platform> =
        platformDao.getAllPlatformsSync().map { it.toDomain() }

    suspend fun getPlatformById(id: String): Platform? =
        platformDao.getPlatformById(id)?.toDomain()

    suspend fun addPlatform(name: String, url: String, colorHex: String): Platform {
        val id = name.trim().lowercase().replace("\\s+".toRegex(), "_") + "_" + UUID.randomUUID().toString().take(4)
        val platform = Platform(
            id = id,
            name = name.trim(),
            defaultUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url",
            accentColorHex = colorHex,
            isCustom = true
        )
        platformDao.insertPlatform(PlatformEntity.fromDomain(platform))

        // Create default account for new platform
        val firstAccount = Account(
            id = UUID.randomUUID().toString(),
            platformId = platform.id,
            name = "Conta 1",
            currentUrl = platform.defaultUrl
        )
        accountDao.insertAccount(AccountEntity.fromDomain(firstAccount))

        return platform
    }

    suspend fun deletePlatform(platformId: String) {
        platformDao.deletePlatformById(platformId)
    }

    suspend fun updatePlatform(id: String, name: String, url: String, colorHex: String) {
        val existing = platformDao.getPlatformById(id) ?: return
        val updated = existing.copy(
            name = name.trim(),
            defaultUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url",
            accentColorHex = colorHex
        )
        platformDao.updatePlatform(updated)
    }

    // --- Accounts ---
    fun getAccountsForPlatform(platformId: String): Flow<List<Account>> =
        accountDao.getAccountsForPlatform(platformId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getAccountsForPlatformSync(platformId: String): List<Account> =
        accountDao.getAccountsForPlatformSync(platformId).map { it.toDomain() }

    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun allAccountsSync(): List<Account> =
        accountDao.getAllAccountsSync().map { it.toDomain() }

    suspend fun getAccountById(id: String): Account? =
        accountDao.getAccountById(id)?.toDomain()

    suspend fun createAccount(platformId: String, customName: String? = null, customUrl: String? = null): Account {
        val existingCount = accountDao.getAccountCountForPlatform(platformId)
        val accountName = customName?.takeIf { it.isNotBlank() } ?: "Conta ${existingCount + 1}"
        val platform = getPlatformById(platformId)
        val startUrl = customUrl?.takeIf { it.isNotBlank() }
            ?.let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
            ?: platform?.defaultUrl ?: "https://8u.com"

        val account = Account(
            id = UUID.randomUUID().toString(),
            platformId = platformId,
            name = accountName,
            currentUrl = startUrl
        )
        accountDao.insertAccount(AccountEntity.fromDomain(account))
        return account
    }

    suspend fun updateAccountDetails(accountId: String, name: String, url: String) {
        val formattedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        accountDao.updateAccountName(accountId, name)
        accountDao.updateAccountUrl(accountId, formattedUrl)
    }

    suspend fun updateAccountUrl(accountId: String, url: String) {
        accountDao.updateAccountUrl(accountId, url)
    }

    suspend fun updateAccountName(accountId: String, name: String) {
        accountDao.updateAccountName(accountId, name)
    }

    suspend fun markAccountAccessed(accountId: String, timestamp: Long = System.currentTimeMillis()) {
        accountDao.updateLastActiveTimestamp(accountId, timestamp)
    }

    suspend fun resetAllAccountsLastActive() {
        accountDao.resetAllLastActiveTimestamps()
    }

    suspend fun deleteAccount(accountId: String) {
        // Cascade removes cookies and storage in Room due to foreign keys,
        // but explicit purge ensures thorough cleanup:
        sessionDao.clearCookiesForAccount(accountId)
        sessionDao.clearAllStorageForAccount(accountId)
        accountDao.deleteAccountById(accountId)
    }

    // --- Cookies ---
    fun getCookiesForAccount(accountId: String): Flow<List<CookieItem>> =
        sessionDao.getCookiesForAccount(accountId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getCookiesForAccountSync(accountId: String): List<CookieItem> =
        sessionDao.getCookiesForAccountSync(accountId).map { it.toDomain() }

    suspend fun saveCookie(cookie: CookieItem) {
        sessionDao.insertCookie(AccountCookieEntity.fromDomain(cookie))
    }

    suspend fun saveCookies(cookies: List<CookieItem>) {
        sessionDao.insertCookies(cookies.map { AccountCookieEntity.fromDomain(it) })
    }

    suspend fun clearCookiesForAccount(accountId: String) {
        sessionDao.clearCookiesForAccount(accountId)
    }

    // --- Storage (Local & Session) ---
    fun getStorageForAccount(accountId: String, storageType: StorageType): Flow<List<StorageItem>> =
        sessionDao.getStorageForAccount(accountId, storageType.name).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getStorageForAccountSync(accountId: String, storageType: StorageType): List<StorageItem> =
        sessionDao.getStorageForAccountSync(accountId, storageType.name).map { it.toDomain() }

    suspend fun getAllStorageForAccountSync(accountId: String): List<StorageItem> =
        sessionDao.getAllStorageForAccountSync(accountId).map { it.toDomain() }

    suspend fun getStorageValue(accountId: String, storageType: StorageType, key: String): String? =
        sessionDao.getStorageValue(accountId, storageType.name, key)

    suspend fun setStorageItem(accountId: String, storageType: StorageType, key: String, value: String) {
        sessionDao.insertStorageItem(
            AccountStorageEntity(
                accountId = accountId,
                storageType = storageType.name,
                key = key,
                value = value,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteStorageItem(accountId: String, storageType: StorageType, key: String) {
        sessionDao.deleteStorageItem(accountId, storageType.name, key)
    }

    suspend fun clearStorage(accountId: String, storageType: StorageType) {
        sessionDao.clearStorageForAccount(accountId, storageType.name)
    }

    suspend fun overwriteAllData(platforms: List<Platform>, accounts: List<Account>) {
        accountDao.deleteAllAccounts()
        platformDao.deleteAllPlatforms()

        platformDao.insertPlatforms(platforms.map { PlatformEntity.fromDomain(it) })
        accountDao.insertAccounts(accounts.map { AccountEntity.fromDomain(it) })
    }
}
