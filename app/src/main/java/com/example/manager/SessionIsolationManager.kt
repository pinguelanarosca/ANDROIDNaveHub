package com.example.manager

import android.content.Context
import android.os.Handler
import android.os.Looper
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
    private val coroutineScope: CoroutineScope
) {
    private val cookieManager: CookieManager? = try {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
        }
    } catch (e: Throwable) {
        null
    }
    private val mainHandler = Handler(Looper.getMainLooper())

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
     * Prepares the isolated environment for a target account.
     * 1. Captures outgoing account's cookies & state (if an account was active).
     * 2. Purges the global CookieManager and WebStorage.
     * 3. Injects target account's saved cookies into CookieManager.
     * 4. Updates active pointers.
     */
    suspend fun switchAccountEnvironment(
        targetAccountId: String,
        targetPlatformId: String,
        targetUrl: String
    ) = withContext(Dispatchers.IO) {
        val outgoingAccountId = activeAccountId
        if (outgoingAccountId != null && outgoingAccountId != targetAccountId) {
            // Snapshot and save cookies from current session
            persistCurrentCookiesForAccount(outgoingAccountId, activeUrl)
        }

        // Wipe global volatile cookies & WebStorage
        try {
            cookieManager?.removeAllCookies(null)
            cookieManager?.flush()
            WebStorage.getInstance().deleteAllData()
        } catch (e: Throwable) {
            // In unit tests or headless contexts, webkit singleton may throw
        }

        // Restore target account's cookies
        val targetCookies = repository.getCookiesForAccountSync(targetAccountId)
        try {
            targetCookies.forEach { cookie ->
                val cookieStr = buildCookieString(cookie)
                val domainUrl = if (cookie.domain.startsWith("http")) cookie.domain else "https://${cookie.domain}"
                cookieManager?.setCookie(domainUrl, cookieStr)
            }
            cookieManager?.flush()
        } catch (e: Throwable) {
            // Ignore in headless test environments
        }

        activeAccountId = targetAccountId
        activePlatformId = targetPlatformId
        activeUrl = targetUrl
    }

    /**
     * Persists cookies from CookieManager for the specified account.
     */
    suspend fun persistCurrentCookiesForAccount(accountId: String, url: String) = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext
        val domain = extractDomain(url) ?: return@withContext

        val cookieHeader = try {
            cookieManager?.getCookie(url)
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
     * Injects the JavaScript isolation bridge and proxies into the WebView.
     */
    fun injectIsolationPolyfill(webView: WebView, accountId: String, platformId: String) {
        val polyfillJs = generateIsolationPolyfillJs(accountId, platformId)
        webView.evaluateJavascript(polyfillJs, null)
    }

    /**
     * JavaScript code that intercepts localStorage and sessionStorage
     * and maps them to our secure Android Room backend via NaveHubBridge.
     */
    fun generateIsolationPolyfillJs(accountId: String, platformId: String): String {
        return """
        (function() {
            if (window.__navehub_initialized__) return;
            window.__navehub_initialized__ = true;
            window.__NAVEHUB_ACCOUNT_ID__ = "$accountId";
            window.__NAVEHUB_PLATFORM_ID__ = "$platformId";

            // Storage implementation backed by Room via NaveHubBridge
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
                console.warn("[NaveHub] Storage override notice: " + e.message);
            }

            window.NaveHub = {
                accountId: "$accountId",
                platformId: "$platformId",
                setCookie: function(domain, name, value) {
                    if (window.NaveHubBridge) {
                        window.NaveHubBridge.saveCookie(domain, name, value);
                    }
                }
            };
        })();
        """.trimIndent()
    }

    /**
     * Creates the JavascriptInterface bridge instance to be added to WebView
     */
    fun createJavascriptBridge(): NaveHubBridge {
        return NaveHubBridge(repository, this)
    }

    /**
     * JavaScript Bridge class exposed as `window.NaveHubBridge`
     */
    class NaveHubBridge(
        private val repository: NaveHubRepository,
        private val isolationManager: SessionIsolationManager
    ) {
        @JavascriptInterface
        fun getActiveAccountId(): String {
            return isolationManager.activeAccountId ?: ""
        }

        @JavascriptInterface
        fun getActivePlatformId(): String {
            return isolationManager.activePlatformId ?: ""
        }

        @JavascriptInterface
        fun getStorageItem(typeStr: String, key: String): String {
            val accountId = isolationManager.activeAccountId ?: return "___NULL_VALUE___"
            val type = if (typeStr == "SESSION") StorageType.SESSION else StorageType.LOCAL
            return runBlocking(Dispatchers.IO) {
                val value = repository.getStorageValue(accountId, type, key)
                value ?: "___NULL_VALUE___"
            }
        }

        @JavascriptInterface
        fun setStorageItem(typeStr: String, key: String, value: String) {
            val accountId = isolationManager.activeAccountId ?: return
            val type = if (typeStr == "SESSION") StorageType.SESSION else StorageType.LOCAL
            runBlocking(Dispatchers.IO) {
                repository.setStorageItem(accountId, type, key, value)
            }
        }

        @JavascriptInterface
        fun removeStorageItem(typeStr: String, key: String) {
            val accountId = isolationManager.activeAccountId ?: return
            val type = if (typeStr == "SESSION") StorageType.SESSION else StorageType.LOCAL
            runBlocking(Dispatchers.IO) {
                repository.deleteStorageItem(accountId, type, key)
            }
        }

        @JavascriptInterface
        fun clearStorage(typeStr: String) {
            val accountId = isolationManager.activeAccountId ?: return
            val type = if (typeStr == "SESSION") StorageType.SESSION else StorageType.LOCAL
            runBlocking(Dispatchers.IO) {
                repository.clearStorage(accountId, type)
            }
        }

        @JavascriptInterface
        fun getAllStorageJson(typeStr: String): String {
            val accountId = isolationManager.activeAccountId ?: return "{}"
            val type = if (typeStr == "SESSION") StorageType.SESSION else StorageType.LOCAL
            return runBlocking(Dispatchers.IO) {
                val items = repository.getStorageForAccountSync(accountId, type)
                val json = JSONObject()
                for (item in items) {
                    json.put(item.key, item.value)
                }
                json.toString()
            }
        }

        @JavascriptInterface
        fun saveCookie(domain: String, name: String, value: String) {
            val accountId = isolationManager.activeAccountId ?: return
            runBlocking(Dispatchers.IO) {
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
