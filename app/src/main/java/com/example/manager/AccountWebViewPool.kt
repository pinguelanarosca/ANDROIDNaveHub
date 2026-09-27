package com.example.manager

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.domain.model.Account
import com.example.domain.model.Platform
import com.example.domain.model.ProfileDiagnostics
import com.example.ui.components.SandboxHtmlGenerator
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages dedicated WebView instances per account.
 * Ensures that each account maintains its own WebView instance
 * permanently bound to its own dedicated native AndroidX Profile.
 * Completely eliminates indiscriminate single-WebView reuse.
 */
class AccountWebViewPool(
    private val context: Context,
    private val nativeProfileManager: NativeProfileManager,
    private val sessionIsolationManager: SessionIsolationManager
) {
    // Map: accountId -> Dedicated WebView
    private val webViewMap = ConcurrentHashMap<String, WebView>()

    fun getOrCreateWebView(
        account: Account,
        platform: Platform,
        isSandboxMode: Boolean,
        onUrlChanged: (String) -> Unit
    ): WebView {
        return webViewMap.getOrPut(account.id) {
            createDedicatedWebView(account, platform, isSandboxMode, onUrlChanged)
        }
    }

    fun getWebView(accountId: String): WebView? {
        return webViewMap[accountId]
    }

    private fun createDedicatedWebView(
        account: Account,
        platform: Platform,
        isSandboxMode: Boolean,
        onUrlChanged: (String) -> Unit
    ): WebView {
        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Allow system automatic layer selection (hardware/software rendering as available)
            setLayerType(android.view.View.LAYER_TYPE_NONE, null)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                // BLOQUEIO 19: Revised mixed content - NEVER use ALWAYS_ALLOW
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                userAgentString = "$userAgentString NaveHub/1.0"
            }
        }

        // BLOQUEIO 1 & 2: Bind native profile BEFORE any page navigation or call!
        if (nativeProfileManager.isMultiProfileSupported) {
            val bindSuccess = nativeProfileManager.bindWebViewToAccountProfile(webView, account.id)
            if (!bindSuccess) {
                // REQUIREMENT 1: If binding fails, DO NOT load URL or sandbox content. Show explicit FAIL state.
                val failHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head><title>BINDING_FAIL</title></head>
                    <body style="background-color: #111; color: #ff5555; font-family: monospace; padding: 24px;">
                        <h2>[FAIL] NATIVE_PROFILE_BINDING_FAILED</h2>
                        <p><strong>Account:</strong> ${account.id}</p>
                        <p><strong>Expected Profile:</strong> navehub_profile_${account.id}</p>
                        <p><strong>Status:</strong> ERROR - Failed to bind or validate native profile on WebView.</p>
                        <p>Navigation and sandbox execution aborted to prevent Default profile data leak.</p>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL(null, failHtml, "text/html", "UTF-8", null)
                return webView
            }
        }

        // REQUIREMENT 18: Restrict NaveHubBridge strictly to Sandbox Mode where content is controlled by the app.
        // For free web browsing (external URLs), NaveHubBridge is omitted to eliminate cross-origin iframe security risks.
        if (isSandboxMode) {
            webView.addJavascriptInterface(
                sessionIsolationManager.createRestrictedBridge(account.id),
                "NaveHubBridge"
            )
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // Intercept navigation to prevent external window/intent leakage
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view?.loadUrl(url)
                    onUrlChanged(url)
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { onUrlChanged(it) }

                // If native multi-profile is NOT supported, inject fallback polyfill
                if (!nativeProfileManager.isMultiProfileSupported && view != null) {
                    sessionIsolationManager.injectIsolationPolyfill(view, account.id, platform.id)
                }
            }
        }

        webView.webChromeClient = WebChromeClient()

        // Initial content load
        if (isSandboxMode) {
            val html = SandboxHtmlGenerator.generateHtml(
                platform = platform,
                account = account,
                isNativeProfile = nativeProfileManager.isMultiProfileSupported,
                profileName = nativeProfileManager.getProfileNameForAccount(account.id)
            )
            webView.loadDataWithBaseURL("https://${platform.id}.navehub.local/", html, "text/html", "UTF-8", null)
        } else {
            webView.loadUrl(account.currentUrl)
        }

        return webView
    }

    fun releaseAccountWebView(accountId: String) {
        val wv = webViewMap.remove(accountId) ?: return
        try {
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.clearHistory()
            wv.destroy()
        } catch (e: Throwable) {
            // Ignore destruction errors
        }
    }

    fun releaseAll() {
        for (accountId in webViewMap.keys()) {
            releaseAccountWebView(accountId)
        }
    }

    fun getDiagnostics(accountId: String): ProfileDiagnostics {
        val wv = webViewMap[accountId]
        return nativeProfileManager.getDiagnostics(accountId, wv)
    }
}
