package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.AccountSessionDao
import com.example.data.local.dao.PlatformDao
import com.example.data.local.entity.AccountCookieEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountStorageEntity
import com.example.data.local.entity.PlatformEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        PlatformEntity::class,
        AccountEntity::class,
        AccountCookieEntity::class,
        AccountStorageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NaveHubDatabase : RoomDatabase() {

    abstract fun platformDao(): PlatformDao
    abstract fun accountDao(): AccountDao
    abstract fun accountSessionDao(): AccountSessionDao

    companion object {
        @Volatile
        private var INSTANCE: NaveHubDatabase? = null

        val INITIAL_PLATFORMS = listOf(
            PlatformEntity(
                id = "8u",
                name = "8U",
                defaultUrl = "https://8u.com",
                accentColorHex = "#00E676",
                isCustom = false
            ),
            PlatformEntity(
                id = "777",
                name = "777",
                defaultUrl = "https://777vipv0.com/#/home",
                accentColorHex = "#FFAB00",
                isCustom = false
            ),
            PlatformEntity(
                id = "365gg",
                name = "365GG",
                defaultUrl = "https://365gg2.com/#/home",
                accentColorHex = "#00B0FF",
                isCustom = false
            ),
            PlatformEntity(
                id = "93h",
                name = "93H",
                defaultUrl = "https://x83yy7.com/main/inicio",
                accentColorHex = "#FF4081",
                isCustom = false
            )
        )

        fun getVipLevel(name: String): Int {
            val match = Regex("VIP(\\d+)", RegexOption.IGNORE_CASE).find(name)
            return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }

        // Ordered by VIP level descending (highest VIP on the left)
        val INITIAL_ACCOUNTS_MAP = mapOf(
            "8u" to listOf(
                "3444VIP8",
                "4202VIP7",
                "5787VIP7",
                "4957VIP7",
                "0062VIP4",
                "4203VIP4",
                "4200VIP4",
                "1919VIP4",
                "1918VIP3",
                "4222VIP3",
                "4201VIP3",
                "4527VIP3",
                "4527VIP2",
                "0001VIP2",
                "3882VIP1",
                "3440VIP1",
                "3351VIP1",
                "3888VIP1"
            ),
            "777" to listOf(
                "4202VIP4",
                "3444VIP4",
                "0062VIP3",
                "1918VIP0"
            ),
            "365gg" to listOf(
                "aleekaosVIP2",
                "4202VIP1",
                "1918VIP1",
                "3444VIP1",
                "0062VIP1",
                "aleesatiroVIP1",
                "HeraclesVIP1",
                "4106VIP1"
            ),
            "93h" to listOf(
                "4202VIP7",
                "0062VIP4",
                "1918VIP3",
                "3444VIP2"
            )
        )

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): NaveHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NaveHubDatabase::class.java,
                    "navehub_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: NaveHubDatabase) {
            val platformDao = database.platformDao()
            val accountDao = database.accountDao()

            if (platformDao.getAllPlatformsSync().isEmpty()) {
                platformDao.insertPlatforms(INITIAL_PLATFORMS)
            }

            INITIAL_PLATFORMS.forEach { platform ->
                val expectedNames = INITIAL_ACCOUNTS_MAP[platform.id] ?: emptyList()
                val currentAccounts = accountDao.getAccountsForPlatformSync(platform.id)
                val currentNames = currentAccounts.map { it.name }.toSet()

                // If accounts don't match the new configured list, update or populate
                val needsSync = currentAccounts.isEmpty() || !expectedNames.all { currentNames.contains(it) }

                if (needsSync) {
                    accountDao.deleteAccountsForPlatform(platform.id)
                    var index = 0
                    expectedNames.forEach { accName ->
                        val account = AccountEntity(
                            id = "${platform.id}_${accName.lowercase()}",
                            platformId = platform.id,
                            name = accName,
                            currentUrl = platform.defaultUrl,
                            createdAt = System.currentTimeMillis() + (index++)
                        )
                        accountDao.insertAccount(account)
                    }
                }
            }
        }
    }
}
