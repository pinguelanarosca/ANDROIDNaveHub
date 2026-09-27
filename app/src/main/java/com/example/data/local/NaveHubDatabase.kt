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
    version = 1,
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
                name = "8u",
                defaultUrl = "https://8u.com",
                accentColorHex = "#00E676", // Neon Green
                isCustom = false
            ),
            PlatformEntity(
                id = "777",
                name = "777",
                defaultUrl = "https://777.com",
                accentColorHex = "#FFAB00", // Gold Amber
                isCustom = false
            ),
            PlatformEntity(
                id = "365gg",
                name = "365gg",
                defaultUrl = "https://365gg.com",
                accentColorHex = "#00B0FF", // Cyan Electric
                isCustom = false
            ),
            PlatformEntity(
                id = "93has",
                name = "93has",
                defaultUrl = "https://93has.com",
                accentColorHex = "#FF4081", // Magenta Rose
                isCustom = false
            )
        )

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): NaveHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NaveHubDatabase::class.java,
                    "navehub_database.db"
                )
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
        }

        suspend fun populateInitialData(database: NaveHubDatabase) {
            val platformDao = database.platformDao()
            val accountDao = database.accountDao()

            if (platformDao.getAllPlatformsSync().isEmpty()) {
                platformDao.insertPlatforms(INITIAL_PLATFORMS)

                // Initialize 1 default account for each platform
                INITIAL_PLATFORMS.forEach { platform ->
                    val defaultAccount = AccountEntity(
                        id = UUID.randomUUID().toString(),
                        platformId = platform.id,
                        name = "Conta 1",
                        currentUrl = platform.defaultUrl,
                        createdAt = System.currentTimeMillis()
                    )
                    accountDao.insertAccount(defaultAccount)
                }
            }
        }
    }
}
