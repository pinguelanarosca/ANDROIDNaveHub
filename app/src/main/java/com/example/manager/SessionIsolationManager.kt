package com.example.manager

import android.content.Context
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebStorage
import android.webkit.WebView
import com.example.data.repository.NaveHubRepository
import com.example.domain.model.CookieItem
import com.example.domain.model.StorageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI

class SessionIsolationManager(
    private val context: Context,
    private val repository: NaveHubRepository,
    private val coroutineScope: CoroutineScope,
    val nativeProfileManager: NativeProfileManager = NativeProfileManager(context)
) {
    private val defaultCookieManager: CookieManager? = try {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
        }
    } catch (e: Throwable) {
        null
    }

    @Volatile
    var activeAccountId: String? = null
        private set

    @Volatile
    var activePlatformId: String? = null
        private set

    @Volatile
    var activeUrl: String = ""
        private set

    /**
     * Resolves the appropriate CookieManager for an account.
     * Uses the native Profile's CookieManager if MULTI_PROFILE is supported,
     * otherwise falls back to the system default singleton.
     */
    fun getEffectiveCookieManager(accountId: String): CookieManager? {
        if (nativeProfileManager.isMultiProfileSupported) {
            val profileCm = nativeProfileManager.getCookieManagerForAccount(accountId)
            if (profileCm != null) return profileCm
        }
        return defaultCookieManager
    }

    /**
     * Resolves the appropriate WebStorage for an account.
     */
    fun getEffectiveWebStorage(accountId: String): WebStorage? {
        if (nativeProfileManager.isMultiProfileSupported) {
            val profileWs = nativeProfileManager.getWebStorageForAccount(accountId)
            if (profileWs != null) return profileWs
        }
        return try {
            WebStorage.getInstance()
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Prepares the isolated environment for a target account.
     * In Native Multi-Profile mode, each account's WebView is bound to its own Profile,
     * so cross-talk is prevented at the Chromium engine level.
     * In legacy fallback mode, purges and restores cookies.
     */
    suspend fun switchAccountEnvironment(
        targetAccountId: String,
        targetPlatformId: String,
        targetUrl: String
    ) = withContext(Dispatchers.IO) {
        val outgoingAccountId = activeAccountId

        if (!nativeProfileManager.isMultiProfileSupported) {
            // Legacy fallback switching
            if (outgoingAccountId != null && outgoingAccountId != targetAccountId) {
                persistCurrentCookiesForAccount(outgoingAccountId, activeUrl)
            }

            try {
                defaultCookieManager?.removeAllCookies(null)
                defaultCookieManager?.flush()
                WebStorage.getInstance().deleteAllData()
            } catch (e: Throwable) {
                // Ignore in headless test contexts
            }

            val targetCookies = repository.getCookiesForAccountSync(targetAccountId)
            try {
                targetCookies.forEach { cookie ->
                    val cookieStr = buildCookieString(cookie)
                    val domainUrl = if (cookie.domain.startsWith("http")) cookie.domain else "https://${cookie.domain}"
                    defaultCookieManager?.setCookie(domainUrl, cookieStr)
                }
                defaultCookieManager?.flush()
            } catch (e: Throwable) {
                // Ignore
            }
        }

        activeAccountId = targetAccountId
        activePlatformId = targetPlatformId
        activeUrl = targetUrl
    }

    suspend fun persistCurrentCookiesForAccount(accountId: String, url: String) = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext
        val domain = extractDomain(url) ?: return@withContext

        val cm = getEffectiveCookieManager(accountId)
        val cookieHeader = try {
            cm?.getCookie(url)
        } catch (e: Throwable) {
            null
        }

        if (!cookieHeader.isNullOrBlank()) {
            val pairs = cookieHeader.split(";").map { it.trim() }
            for (pair in pairs) {
                val eqIdx = pair.indexOf('=')
                if (eqIdx > 0) {
                    val name = pair.substring(0, eqIdx).trim()
                    val value = pair.substring(eqIdx + 1).trim()
                    repository.saveCookie(
                        CookieItem(
                            accountId = accountId,
                            domain = domain,
                            name = name,
                            value = value
                        )
                    )
                }
            }
        }
    }

    private fun extractDomain(url: String): String? {
        return try {
            val uri = URI(if (url.startsWith("http")) url else "https://$url")
            uri.host ?: url
        } catch (e: Exception) {
            null
        }
    }

    private fun buildCookieString(cookie: CookieItem): String {
        val sb = StringBuilder()
        sb.append("${cookie.name}=${cookie.value}; path=${cookie.path}")
        if (cookie.isSecure) sb.append("; Secure")
        if (cookie.isHttpOnly) sb.append("; HttpOnly")
        return sb.toString()
    }

    /**
     * Injects fallback JavaScript isolation polyfill only when Native Multi-Profile is not supported.
     */
    fun injectIsolationPolyfill(webView: WebView, accountId: String, platformId: String) {
        if (nativeProfileManager.isMultiProfileSupported) return // Native profile provides real storage!
        val polyfillJs = generateIsolationPolyfillJs(accountId, platformId)
        webView.evaluateJavascript(polyfillJs, null)
    }

    fun generateIsolationPolyfillJs(accountId: String, platformId: String): String {
        return """
        (function() {
            if (window.__navehub_initialized__) return;
            window.__navehub_initialized__ = true;
            window.__NAVEHUB_ACCOUNT_ID__ = "$accountId";
            window.__NAVEHUB_PLATFORM_ID__ = "$platformId";

            function createIsolatedStorage(type) {
                return {
                    getItem: function(key) {
                        if (!window.NaveHubBridge) return null;
                        var val = window.NaveHubBridge.getStorageItem(type, key);
                        return (val === "___NULL_VALUE___" || val === null) ? null : val;
                    },
                    setItem: function(key, value) {
                        if (!window.NaveHubBridge) return;
                        window.NaveHubBridge.setStorageItem(type, key, String(value));
                    },
                    removeItem: function(key) {
                        if (!window.NaveHubBridge) return;
                        window.NaveHubBridge.removeStorageItem(type, key);
                    },
                    clear: function() {
                        if (!window.NaveHubBridge) return;
                        window.NaveHubBridge.clearStorage(type);
                    },
                    getAll: function() {
                        if (!window.NaveHubBridge) return {};
                        var json = window.NaveHubBridge.getAllStorageJson(type);
                        try { return JSON.parse(json); } catch(e) { return {}; }
                    },
                    get length() {
                        return Object.keys(this.getAll()).length;
                    },
                    key: function(index) {
                        var keys = Object.keys(this.getAll());
                        return keys[index] || null;
                    }
                };
            }

            try {
                var isolatedLocal = createIsolatedStorage("LOCAL");
                var isolatedSession = createIsolatedStorage("SESSION");

                Object.defineProperty(window, 'localStorage', {
                    get: function() { return isolatedLocal; },
                    configurable: true
                });

                Object.defineProperty(window, 'sessionStorage', {
                    get: function() { return isolatedSession; },
                    configurable: true
                });
            } catch(e) {
                console.warn("[NaveHub] Polyfill notice: " + e.message);
            }
        })();
        """.trimIndent()
    }

    /**
     * Creates a restricted auxiliary JavascriptInterface bound strictly to its accountId.
     * BLOQUEIO 3 & 10: Bridge is non-essential and restricted to avoid arbitrary external origin abuse.
     */
    fun createRestrictedBridge(boundAccountId: String): RestrictedNaveHubBridge {
        return RestrictedNaveHubBridge(boundAccountId, repository, this)
    }

    class RestrictedNaveHubBridge(
        private val boundAccountId: String,
        private val repository: NaveHubRepository,
        private val isolationManager: SessionIsolationManager
    ) {
        @JavascriptInterface
        fun getActiveAccountId(): String {
            // Strict guard: verify calling context matches bound account
            if (isolationManager.activeAccountId != boundAccountId) return ""
            return boundAccountId
        }

        @JavascriptInterface
        fun getStorageItem(typeStr: String, key: String): String {
            if (isolationManager.activeAccountId != boundAccountId) return "___NULL_VALUE___"
            val type = if (typeStr == "SESSION") StorageType.SESSION else StorageType.LOCAL
            return runBlocking(Dispatchers.IO) {
                val value = repository.getStorageValue(boundAccountId, type, key)
                value ?: "___NULL_VALUE___"
            }
        }

        @JavascriptInterface
        fun setStorageItem(typeStr: String, key: String, value: String) {
            if (isolationManager.activeAccountId != boundAccountId) return
            val type = if (typeStr == "SESSION") StorageType.SESSION else StorageType.LOCAL
            runBlocking(Dispatchers.IO) {
                repository.setStorageItem(boundAccountId, type, key, value)
            }
        }

        @JavascriptInterface
        fun removeStorageItem(typeStr: String, key: String) {
            if (isolationManager.activeAccountId != boundAccountId) return
            val type = if (typeStr == "SESSION") StorageType.SESSION else StorageType.LOCAL
            runBlocking(Dispatchers.IO) {
                repository.deleteStorageItem(boundAccountId, type, key)
            }
        }

        @JavascriptInterface
        fun clearStorage(typeStr: String) {
            if (isolationManager.activeAccountId != boundAccountId) return
            val type = if (typeStr == "SESSION") StorageType.SESSION else StorageType.LOCAL
            runBlocking(Dispatchers.IO) {
                repository.clearStorage(boundAccountId, type)
            }
        }

        @JavascriptInterface
        fun getAllStorageJson(typeStr: String): String {
            if (isolationManager.activeAccountId != boundAccountId) return "{}"
            val type = if (typeStr == "SESSION") StorageType.SESSION else StorageType.LOCAL
            return runBlocking(Dispatchers.IO) {
                val items = repository.getStorageForAccountSync(boundAccountId, type)
                val json = JSONObject()
                for (item in items) {
                    json.put(item.key, item.value)
                }
                json.toString()
            }
        }

        @JavascriptInterface
        fun saveCookie(domain: String, name: String, value: String) {
            if (isolationManager.activeAccountId != boundAccountId) return
            runBlocking(Dispatchers.IO) {
                repository.saveCookie(
                    CookieItem(
                        accountId = boundAccountId,
                        domain = domain,
                        name = name,
                        value = value
                    )
                )
            }
        }
    }
}
