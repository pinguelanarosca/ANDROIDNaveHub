package com.example.manager

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import java.util.concurrent.ConcurrentHashMap

class AccountWebViewPool(
    private val context: Context,
    private val nativeProfileManager: NativeProfileManager,
    private val sessionIsolationManager: SessionIsolationManager
) {

    private val webViewMap = ConcurrentHashMap<String, WebView>()

    init {
        try {
            val cacheBase = java.io.File(context.cacheDir, "WebView")
            java.io.File(cacheBase, "Default/HTTP Cache/Code Cache/wasm").mkdirs()
            java.io.File(cacheBase, "Default/HTTP Cache/Code Cache/js").mkdirs()
        } catch (_: Throwable) {
        }
    }

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

            setLayerType(android.view.View.LAYER_TYPE_NONE, null)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)
                userAgentString = "$userAgentString NaveHub/1.0"
            }
        }

        // Bind native profile BEFORE any navigation
        if (nativeProfileManager.isMultiProfileSupported) {
            val bindSuccess = nativeProfileManager.bindWebViewToAccountProfile(webView, account.id)
            if (!bindSuccess) {
                val failHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head><title>BINDING_FAIL</title></head>
                    <body style="background-color: #111; color: #ff5555; font-family: monospace; padding: 24px;">
                        <h2>[FAIL] NATIVE_PROFILE_BINDING_FAILED</h2>
                        <p><strong>Account:</strong> ${account.id}</p>
                        <p><strong>Expected Profile:</strong> navehub_profile_${account.id}</p>
                        <p><strong>Status:</strong> ERROR - Failed to bind native profile on WebView.</p>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL(null, failHtml, "text/html", "UTF-8", null)
                return webView
            }
        }

        if (isSandboxMode) {
            webView.addJavascriptInterface(
                sessionIsolationManager.createRestrictedBridge(account.id),
                "NaveHubBridge"
            )
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                // If it is a custom/external scheme (tel, mailto, intent, whatsapp), open external intent
                if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                    return try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        true
                    } catch (e: Exception) {
                        Log.w("NaveHubPool", "Cannot open custom scheme: $url", e)
                        true // Suppress unhandled scheme crashes
                    }
                }

                // Return false so WebView loads standard http/https naturally without reload loops or breaking SPA routers
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { onUrlChanged(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { onUrlChanged(it) }

                if (view != null) {
                    val blockerScript = PlatformPopupBlockers.getBlockerScriptForPlatform(platform.id)
                    if (blockerScript.isNotBlank()) {
                        view.evaluateJavascript(blockerScript, null)
                    }
                    val titleScript = PlatformPopupBlockers.getTitleScript(account.name)
                    view.evaluateJavascript(titleScript, null)

                    if (!nativeProfileManager.isMultiProfileSupported) {
                        sessionIsolationManager.injectIsolationPolyfill(view, account.id, platform.id)
                    }
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val errorCode = error?.errorCode ?: 0
                    val description = error?.description?.toString() ?: "Network error"
                    Log.w("NaveHubPool", "Network issue on main frame: code=$errorCode, desc=$description, url=${request.url}")
                }
            }
        }

        webView.webChromeClient = WebChromeClient()

        // Initial content load
        if (isSandboxMode) {
            val html = """
                <!DOCTYPE html>
                <html>
                <head><title>NaveHub Sandbox - ${account.name}</title></head>
                <body style="background-color: #0b0f19; color: #00ffff; font-family: sans-serif; padding: 20px;">
                    <h2>NaveHub Sandbox: ${platform.name}</h2>
                    <p>Account: ${account.name} (${account.id})</p>
                    <p>Status: Isolated Environment Active</p>
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL("https://${platform.id}.navehub.local/", html, "text/html", "UTF-8", null)
        } else if (account.currentUrl.isNotBlank()) {
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
            Log.e("NaveHubPool", "Error destroying webview for $accountId", e)
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
