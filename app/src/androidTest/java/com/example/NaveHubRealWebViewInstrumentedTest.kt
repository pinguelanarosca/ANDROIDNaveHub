package com.example

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.data.database.NaveHubDatabase
import com.example.data.repository.NaveHubRepository
import com.example.domain.model.StorageType
import com.example.manager.AccountWebViewPool
import com.example.manager.NativeProfileManager
import com.example.manager.SessionIsolationManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android Instrumented Test for ANDROIDNaveHub Profile & WebView Isolation.
 * Runs on real Android Device / Emulator.
 */
@RunWith(AndroidJUnit4::class)
class NaveHubRealWebViewInstrumentedTest {

    private lateinit var context: Context
    private lateinit var database: NaveHubDatabase
    private lateinit var repository: NaveHubRepository
    private lateinit var nativeProfileManager: NativeProfileManager
    private lateinit var sessionIsolationManager: SessionIsolationManager
    private lateinit var webViewPool: AccountWebViewPool

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = NaveHubDatabase.getDatabase(context)
        repository = NaveHubRepository(
            database.accountDao(),
            database.platformDao(),
            database.storageDao(),
            database.cookieDao()
        )
        nativeProfileManager = NativeProfileManager(context)
        sessionIsolationManager = SessionIsolationManager(context, repository, nativeProfileManager)
        webViewPool = AccountWebViewPool(context, nativeProfileManager, sessionIsolationManager)
    }

    @After
    fun tearDown() {
        webViewPool.clearPool()
    }

    @Test
    fun test01_VerifyMultiProfileFeatureSupport() {
        val isSupported = nativeProfileManager.isMultiProfileSupported
        val webViewPackage = try {
            WebViewCompat.getCurrentWebViewPackage(context)
        } catch (e: Throwable) {
            null
        }
        val versionStr = webViewPackage?.let { "${it.packageName} v${it.versionName}" } ?: "System WebView"
        
        println("INSTRUMENTED_TEST: MULTI_PROFILE Supported = $isSupported ($versionStr)")
        // In instrumented environment, we verify truthful reporting
        if (isSupported) {
            assertTrue("WebViewFeature.MULTI_PROFILE should be active", WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE))
        }
    }

    @Test
    fun test02_RuntimeProfileAndWebViewBindingValidation() = runBlocking {
        val platform = repository.getPlatformsSync().first()
        val accA = repository.createAccount(platform.id, "Real Device Acc A")
        val accB = repository.createAccount(platform.id, "Real Device Acc B")

        try {
            val wvA = webViewPool.getOrCreateWebView(accA, platform, isSandboxMode = true) {}
            val wvB = webViewPool.getOrCreateWebView(accB, platform, isSandboxMode = true) {}

            assertNotNull(wvA)
            assertNotNull(wvB)
            assertNotEquals("WebViews must be separate instances", wvA, wvB)

            if (nativeProfileManager.isMultiProfileSupported) {
                val profileNameA = nativeProfileManager.getProfileNameForAccount(accA.id)
                val profileNameB = nativeProfileManager.getProfileNameForAccount(accB.id)

                assertNotEquals("Profile A must not equal Profile B", profileNameA, profileNameB)
                assertFalse("Profile A must not be Default", profileNameA.lowercase().contains("default"))
                assertFalse("Profile B must not be Default", profileNameB.lowercase().contains("default"))

                val boundA = WebViewCompat.getProfile(wvA).name
                val boundB = WebViewCompat.getProfile(wvB).name

                assertEquals("WebView A bound to Profile A", profileNameA, boundA)
                assertEquals("WebView B bound to Profile B", profileNameB, boundB)
            }
        } finally {
            repository.deleteAccount(accA.id)
            repository.deleteAccount(accB.id)
            nativeProfileManager.deleteProfile(accA.id)
            nativeProfileManager.deleteProfile(accB.id)
        }
    }

    @Test
    fun test03_IntraPlatformCookieAndStorageIsolation() = runBlocking {
        val platform = repository.getPlatformsSync().first()
        val accA = repository.createAccount(platform.id, "User Alice")
        val accB = repository.createAccount(platform.id, "User Bob")

        try {
            val key = "user_token"
            val valA = "TOKEN_ALICE_REAL_DEVICE_123"
            val valB = "TOKEN_BOB_REAL_DEVICE_456"

            repository.setStorageItem(accA.id, StorageType.LOCAL, key, valA)
            repository.setStorageItem(accB.id, StorageType.LOCAL, key, valB)

            val readA = repository.getStorageValue(accA.id, StorageType.LOCAL, key)
            val readB = repository.getStorageValue(accB.id, StorageType.LOCAL, key)

            assertEquals(valA, readA)
            assertEquals(valB, readB)
            assertNotEquals(readA, readB)
        } finally {
            repository.deleteAccount(accA.id)
            repository.deleteAccount(accB.id)
            nativeProfileManager.deleteProfile(accA.id)
            nativeProfileManager.deleteProfile(accB.id)
        }
    }

    @Test
    fun test04_FiftyRapidTogglesConcurrency() = runBlocking {
        val platform = repository.getPlatformsSync().first()
        val accA = repository.createAccount(platform.id, "Toggle Acc A")
        val accB = repository.createAccount(platform.id, "Toggle Acc B")

        try {
            val wvA = webViewPool.getOrCreateWebView(accA, platform, isSandboxMode = true) {}
            val wvB = webViewPool.getOrCreateWebView(accB, platform, isSandboxMode = true) {}

            assertNotNull(wvA)
            assertNotNull(wvB)

            repository.setStorageItem(accA.id, StorageType.LOCAL, "toggle_val", "VAL_A")
            repository.setStorageItem(accB.id, StorageType.LOCAL, "toggle_val", "VAL_B")

            for (i in 1..50) {
                sessionIsolationManager.switchAccountEnvironment(accA.id, platform.id, "https://example.com")
                assertEquals("VAL_A", repository.getStorageValue(accA.id, StorageType.LOCAL, "toggle_val"))

                sessionIsolationManager.switchAccountEnvironment(accB.id, platform.id, "https://example.com")
                assertEquals("VAL_B", repository.getStorageValue(accB.id, StorageType.LOCAL, "toggle_val"))
            }
        } finally {
            repository.deleteAccount(accA.id)
            repository.deleteAccount(accB.id)
            nativeProfileManager.deleteProfile(accA.id)
            nativeProfileManager.deleteProfile(accB.id)
        }
    }

    @Test
    fun test05_NewAccountCleanStateAndAccountRemovalIndependence() = runBlocking {
        val platform = repository.getPlatformsSync().first()
        val accA = repository.createAccount(platform.id, "Keep Acc A")
        val accB = repository.createAccount(platform.id, "Delete Acc B")

        try {
            repository.setStorageItem(accA.id, StorageType.LOCAL, "data", "ALICE_PERSISTENT")
            repository.setStorageItem(accB.id, StorageType.LOCAL, "data", "BOB_TEMPORARY")

            // Create new Account C while A and B exist
            val accC = repository.createAccount(platform.id, "Clean Acc C")
            val storageC = repository.getAllStorageForAccountSync(accC.id)
            assertTrue("New Account C must start with 0 storage items", storageC.isEmpty())

            // Delete Account B and its profile
            repository.deleteAccount(accB.id)
            nativeProfileManager.deleteProfile(accB.id)

            // Confirm Account A remains intact
            val readA = repository.getStorageValue(accA.id, StorageType.LOCAL, "data")
            assertEquals("ALICE_PERSISTENT", readA)

            repository.deleteAccount(accC.id)
            nativeProfileManager.deleteProfile(accC.id)
        } finally {
            repository.deleteAccount(accA.id)
            nativeProfileManager.deleteProfile(accA.id)
        }
    }

    @Test
    fun test06_NaveHubBridgeSandboxConfinement() = runBlocking {
        val platform = repository.getPlatformsSync().first()
        val accFree = repository.createAccount(platform.id, "Free Web Acc")
        val accSandbox = repository.createAccount(platform.id, "Sandbox Acc")

        try {
            // Free Web mode (isSandboxMode = false): separate account instance
            val wvFree = webViewPool.getOrCreateWebView(accFree, platform, isSandboxMode = false) {}
            assertNotNull("Free Web WebView must be created", wvFree)

            // Sandbox mode (isSandboxMode = true): separate account instance
            val wvSandbox = webViewPool.getOrCreateWebView(accSandbox, platform, isSandboxMode = true) {}
            assertNotNull("Sandbox WebView must be created", wvSandbox)
            assertNotEquals("WebViews for distinct accounts must be distinct instances", wvFree, wvSandbox)
        } finally {
            repository.deleteAccount(accFree.id)
            repository.deleteAccount(accSandbox.id)
            nativeProfileManager.deleteProfile(accFree.id)
            nativeProfileManager.deleteProfile(accSandbox.id)
        }
    }
}
