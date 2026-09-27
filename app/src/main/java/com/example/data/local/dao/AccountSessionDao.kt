package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AccountCookieEntity
import com.example.data.local.entity.AccountStorageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountSessionDao {
    // --- Cookies ---
    @Query("SELECT * FROM account_cookies WHERE accountId = :accountId")
    fun getCookiesForAccount(accountId: String): Flow<List<AccountCookieEntity>>

    @Query("SELECT * FROM account_cookies WHERE accountId = :accountId")
    suspend fun getCookiesForAccountSync(accountId: String): List<AccountCookieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookie(cookie: AccountCookieEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookies(cookies: List<AccountCookieEntity>)

    @Query("DELETE FROM account_cookies WHERE accountId = :accountId")
    suspend fun clearCookiesForAccount(accountId: String)

    @Query("DELETE FROM account_cookies WHERE accountId = :accountId AND domain = :domain AND name = :name")
    suspend fun deleteCookie(accountId: String, domain: String, name: String)

    // --- Storage (Local & Session) ---
    @Query("SELECT * FROM account_storage WHERE accountId = :accountId AND storageType = :storageType")
    fun getStorageForAccount(accountId: String, storageType: String): Flow<List<AccountStorageEntity>>

    @Query("SELECT * FROM account_storage WHERE accountId = :accountId AND storageType = :storageType")
    suspend fun getStorageForAccountSync(accountId: String, storageType: String): List<AccountStorageEntity>

    @Query("SELECT * FROM account_storage WHERE accountId = :accountId")
    suspend fun getAllStorageForAccountSync(accountId: String): List<AccountStorageEntity>

    @Query("SELECT value FROM account_storage WHERE accountId = :accountId AND storageType = :storageType AND key = :key LIMIT 1")
    suspend fun getStorageValue(accountId: String, storageType: String, key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorageItem(item: AccountStorageEntity)

    @Query("DELETE FROM account_storage WHERE accountId = :accountId AND storageType = :storageType AND key = :key")
    suspend fun deleteStorageItem(accountId: String, storageType: String, key: String)

    @Query("DELETE FROM account_storage WHERE accountId = :accountId AND storageType = :storageType")
    suspend fun clearStorageForAccount(accountId: String, storageType: String)

    @Query("DELETE FROM account_storage WHERE accountId = :accountId")
    suspend fun clearAllStorageForAccount(accountId: String)
}
