package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE platformId = :platformId ORDER BY createdAt ASC")
    fun getAccountsForPlatform(platformId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE platformId = :platformId ORDER BY createdAt ASC")
    suspend fun getAccountsForPlatformSync(platformId: String): List<AccountEntity>

    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    suspend fun getAllAccountsSync(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET currentUrl = :url, lastActiveTimestamp = :timestamp WHERE id = :id")
    suspend fun updateAccountUrl(id: String, url: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET name = :name WHERE id = :id")
    suspend fun updateAccountName(id: String, name: String)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: String)

    @Query("DELETE FROM accounts WHERE platformId = :platformId")
    suspend fun deleteAccountsForPlatform(platformId: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Query("SELECT COUNT(*) FROM accounts WHERE platformId = :platformId")
    suspend fun getAccountCountForPlatform(platformId: String): Int
}
