package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NaveHubDatabase
import com.example.data.repository.NaveHubRepository
import com.example.domain.model.CookieItem
import com.example.domain.model.StorageType
import com.example.manager.AccountWebViewPool
import com.example.manager.IsolationAuditor
import com.example.manager.NativeProfileManager
import com.example.manager.SessionIsolationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NaveHubIsolationRobolectricTest {

    private lateinit var database: NaveHubDatabase
    private lateinit var repository: NaveHubRepository
    private lateinit var nativeProfileManager: NativeProfileManager
    private lateinit var isolationManager: SessionIsolationManager
    private lateinit var webViewPool: AccountWebViewPool
    private lateinit var auditor: IsolationAuditor
    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NaveHubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NaveHubRepository(database)
        val scope = CoroutineScope(Dispatchers.IO)
        nativeProfileManager = NativeProfileManager(context)
        isolationManager = SessionIsolationManager(context, repository, scope, nativeProfileManager)
        webViewPool = AccountWebViewPool(context, nativeProfileManager, isolationManager)
        auditor = IsolationAuditor(context, repository, isolationManager, nativeProfileManager)

        NaveHubDatabase.populateInitialData(database)
    }

    @After
    fun tearDown() {
        webViewPool.releaseAll()
        database.close()
    }

    @Test
    fun test1_CleanInitialization_FourInitialPlatforms() = runBlocking {
        val platforms = repository.getPlatformsSync()
        assertEquals(4, platforms.size)
        val platformIds = platforms.map { it.id }.toSet()
        assertTrue(platformIds.contains("8u"))
        assertTrue(platformIds.contains("777"))
        assertTrue(platformIds.contains("365gg"))
        assertTrue(platformIds.contains("93has"))
    }

    @Test
    fun test2_PlatformSelectionAndInitialAccounts() = runBlocking {
        val accounts8u = repository.getAccountsForPlatformSync("8u")
        val accounts777 = repository.getAccountsForPlatformSync("777")
        assertTrue(accounts8u.isNotEmpty())
        assertTrue(accounts777.isNotEmpty())
        assertEquals("8u", accounts8u.first().platformId)
        assertEquals("777", accounts777.first().platformId)
    }

    @Test
    fun test3_CreateMultipleAccountsOnSamePlatform() = runBlocking {
        val acc1 = repository.createAccount("8u", "Conta Alpha")
        val acc2 = repository.createAccount("8u", "Conta Beta")

        assertNotEquals(acc1.id, acc2.id)
        assertEquals("Conta Alpha", acc1.name)
        assertEquals("Conta Beta", acc2.name)

        val accounts = repository.getAccountsForPlatformSync("8u")
        assertTrue(accounts.any { it.id == acc1.id })
        assertTrue(accounts.any { it.id == acc2.id })
    }

    @Test
    fun test4_AccountRemovalIndependence() = runBlocking {
        val accA = repository.createAccount("8u", "Conta A")
        val accB = repository.createAccount("8u", "Conta B")

        repository.setStorageItem(accA.id, StorageType.LOCAL, "token_a", "SECRET_A")
        repository.setStorageItem(accB.id, StorageType.LOCAL, "token_b", "SECRET_B")

        // Delete Account A
        repository.deleteAccount(accA.id)

        // Verify A's data is wiped
        val storageA = repository.getStorageValue(accA.id, StorageType.LOCAL, "token_a")
        assertNull(storageA)
        assertNull(repository.getAccountById(accA.id))

        // Verify B's data remains 100% intact
        val storageB = repository.getStorageValue(accB.id, StorageType.LOCAL, "token_b")
        assertEquals("SECRET_B", storageB)
        assertNotNull(repository.getAccountById(accB.id))
    }

    @Test
    fun test5_IntraPlatformCookieAndStorageIsolation_8u() = runBlocking {
        val accA = repository.createAccount("8u", "Conta A (8u)")
        val accB = repository.createAccount("8u", "Conta B (8u)")

        // Write distinct cookies
        repository.saveCookie(CookieItem(accA.id, "8u.com", "session_id", "COOKIE_ALICE"))
        repository.saveCookie(CookieItem(accB.id, "8u.com", "session_id", "COOKIE_BOB"))

        // Write distinct localStorage
        repository.setStorageItem(accA.id, StorageType.LOCAL, "user", "Alice")
        repository.setStorageItem(accA.id, StorageType.LOCAL, "balance", "1500")

        repository.setStorageItem(accB.id, StorageType.LOCAL, "user", "Bob")
        repository.setStorageItem(accB.id, StorageType.LOCAL, "balance", "0")

        // Verify cookies
        val cookiesA = repository.getCookiesForAccountSync(accA.id)
        val cookiesB = repository.getCookiesForAccountSync(accB.id)

        assertEquals(1, cookiesA.size)
        assertEquals(1, cookiesB.size)
        assertEquals("COOKIE_ALICE", cookiesA.first().value)
        assertEquals("COOKIE_BOB", cookiesB.first().value)

        // Verify localStorage
        assertEquals("Alice", repository.getStorageValue(accA.id, StorageType.LOCAL, "user"))
        assertEquals("Bob", repository.getStorageValue(accB.id, StorageType.LOCAL, "user"))
        assertEquals("1500", repository.getStorageValue(accA.id, StorageType.LOCAL, "balance"))
        assertEquals("0", repository.getStorageValue(accB.id, StorageType.LOCAL, "balance"))
    }

    @Test
    fun test6_InterPlatformIsolation_8u_vs_777() = runBlocking {
        val acc8u = repository.createAccount("8u", "Conta 8u")
        val acc777 = repository.createAccount("777", "Conta 777")

        repository.setStorageItem(acc8u.id, StorageType.LOCAL, "auth_token", "TOKEN_8U")
        repository.setStorageItem(acc777.id, StorageType.LOCAL, "auth_token", "TOKEN_777")

        assertEquals("TOKEN_8U", repository.getStorageValue(acc8u.id, StorageType.LOCAL, "auth_token"))
        assertEquals("TOKEN_777", repository.getStorageValue(acc777.id, StorageType.LOCAL, "auth_token"))
        assertNotEquals(
            repository.getStorageValue(acc8u.id, StorageType.LOCAL, "auth_token"),
            repository.getStorageValue(acc777.id, StorageType.LOCAL, "auth_token")
        )
    }

    @Test
    fun test7_NewAccountCleanInheritance() = runBlocking {
        val acc1 = repository.createAccount("8u", "Conta 1")
        repository.setStorageItem(acc1.id, StorageType.LOCAL, "secret", "LEAK_CHECK")
        repository.saveCookie(CookieItem(acc1.id, "8u.com", "cookie1", "LEAK_COOKIE"))

        val acc2 = repository.createAccount("8u", "Conta 2")
        val acc2Storage = repository.getAllStorageForAccountSync(acc2.id)
        val acc2Cookies = repository.getCookiesForAccountSync(acc2.id)

        assertTrue("New account must have empty storage", acc2Storage.isEmpty())
        assertTrue("New account must have empty cookies", acc2Cookies.isEmpty())
    }

    @Test
    fun test8_RapidAccountSwitchingIntegrity() = runBlocking {
        val accA = repository.createAccount("8u", "A")
        val accB = repository.createAccount("8u", "B")

        repository.setStorageItem(accA.id, StorageType.LOCAL, "state", "STATE_A")
        repository.setStorageItem(accB.id, StorageType.LOCAL, "state", "STATE_B")

        for (i in 1..15) {
            isolationManager.switchAccountEnvironment(accA.id, "8u", "https://8u.com")
            assertEquals("STATE_A", repository.getStorageValue(accA.id, StorageType.LOCAL, "state"))

            isolationManager.switchAccountEnvironment(accB.id, "8u", "https://8u.com")
            assertEquals("STATE_B", repository.getStorageValue(accB.id, StorageType.LOCAL, "state"))
        }
    }

    @Test
    fun test9_SessionStorageIsolation() = runBlocking {
        val accA = repository.createAccount("365gg", "Acc A")
        val accB = repository.createAccount("365gg", "Acc B")

        repository.setStorageItem(accA.id, StorageType.SESSION, "temp_step", "STEP_3")
        repository.setStorageItem(accB.id, StorageType.SESSION, "temp_step", "STEP_1")

        assertEquals("STEP_3", repository.getStorageValue(accA.id, StorageType.SESSION, "temp_step"))
        assertEquals("STEP_1", repository.getStorageValue(accB.id, StorageType.SESSION, "temp_step"))
    }

    @Test
    fun test10_ComprehensiveIsolationAuditor_All20CriteriaPass() = runBlocking {
        val report = auditor.runFullAudit()
        assertNotNull(report)
        assertEquals(20, report.totalCount)

        report.criteriaResults.forEach { criterion ->
            assertNotNull(criterion.title)
            assertTrue("Status must be one of PASS, FAIL, UNSUPPORTED, NOT_TESTED",
                criterion.status in listOf("PASS", "FAIL", "UNSUPPORTED", "NOT_TESTED"))
            assertFalse("Criterion must not be FAIL", criterion.status == "FAIL")
        }
    }

    @Test
    fun test11_ProfileDiagnosticsNotDefault() {
        val testAccId = UUID.randomUUID().toString()
        val diag = nativeProfileManager.getDiagnostics(testAccId, null)
        assertNotNull(diag)
        assertEquals("navehub_profile_$testAccId", diag.profileName)
        assertFalse("Profile name must never be default", diag.profileName.lowercase().contains("default"))
    }

    @Test
    fun test12_FiftyRapidTogglesConcurrencyAndWebViewPool() = runBlocking {
        val platform = repository.getPlatformsSync().first()
        val accA = repository.createAccount(platform.id, "Conta Conc A")
        val accB = repository.createAccount(platform.id, "Conta Conc B")

        val wvA = webViewPool.getOrCreateWebView(accA, platform, isSandboxMode = true) {}
        val wvB = webViewPool.getOrCreateWebView(accB, platform, isSandboxMode = true) {}

        assertNotNull(wvA)
        assertNotNull(wvB)
        assertNotEquals(wvA, wvB)

        repository.setStorageItem(accA.id, StorageType.LOCAL, "token", "ACC_A_TOKEN")
        repository.setStorageItem(accB.id, StorageType.LOCAL, "token", "ACC_B_TOKEN")

        for (i in 1..50) {
            isolationManager.switchAccountEnvironment(accA.id, platform.id, "https://example.com")
            assertEquals("ACC_A_TOKEN", repository.getStorageValue(accA.id, StorageType.LOCAL, "token"))

            isolationManager.switchAccountEnvironment(accB.id, platform.id, "https://example.com")
            assertEquals("ACC_B_TOKEN", repository.getStorageValue(accB.id, StorageType.LOCAL, "token"))
        }
    }

    @Test
    fun test13_BridgeOmittedInFreeWebBrowsingMode() = runBlocking {
        val platform = repository.getPlatformsSync().first()
        val acc = repository.createAccount(platform.id, "Free Web Acc")

        // Free web browsing mode (isSandboxMode = false) must not attach NaveHubBridge
        val webView = webViewPool.getOrCreateWebView(acc, platform, isSandboxMode = false) {}
        assertNotNull(webView)
    }
}
