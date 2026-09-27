package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.Account
import com.example.domain.model.Platform
import com.example.manager.SessionIsolationManager
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyanNeon

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NaveWebViewContainer(
    account: Account,
    platform: Platform,
    isolationManager: SessionIsolationManager,
    isSandboxMode: Boolean,
    onUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrlInput by remember(account.id, isSandboxMode) {
        mutableStateOf(if (isSandboxMode) "navehub://sandbox/${platform.id}/${account.name}" else account.currentUrl)
    }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableFloatStateOf(0f) }

    // Intercept hardware/system back button
    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    // Effect whenever account or mode changes: switch isolated session
    LaunchedEffect(account.id, isSandboxMode) {
        val targetUrl = if (isSandboxMode) "sandbox" else account.currentUrl
        isolationManager.switchAccountEnvironment(account.id, platform.id, targetUrl)

        webViewInstance?.let { wv ->
            if (isSandboxMode) {
                val html = SandboxHtmlGenerator.generateHtml(platform, account)
                wv.loadDataWithBaseURL("https://${platform.id}.navehub.local/", html, "text/html", "UTF-8", null)
            } else {
                wv.loadUrl(account.currentUrl)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Navigation & URL Control Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            color = CyberSurface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = { webViewInstance?.goBack() },
                    enabled = canGoBack,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = if (canGoBack) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Forward Button
                IconButton(
                    onClick = { webViewInstance?.goForward() },
                    enabled = canGoForward,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Avançar",
                        tint = if (canGoForward) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Refresh Button
                IconButton(
                    onClick = {
                        if (isSandboxMode) {
                            webViewInstance?.let { wv ->
                                val html = SandboxHtmlGenerator.generateHtml(platform, account)
                                wv.loadDataWithBaseURL("https://${platform.id}.navehub.local/", html, "text/html", "UTF-8", null)
                            }
                        } else {
                            webViewInstance?.reload()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("nav_reload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recarregar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // URL / Address field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Seguro",
                            tint = CyanNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        if (isSandboxMode) {
                            Text(
                                text = "navehub://${platform.name}/${account.name} (Ambiente Sandbox)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        } else {
                            OutlinedTextField(
                                value = currentUrlInput,
                                onValueChange = { currentUrlInput = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        val urlToLoad = if (currentUrlInput.startsWith("http://") || currentUrlInput.startsWith("https://")) {
                                            currentUrlInput
                                        } else {
                                            "https://$currentUrlInput"
                                        }
                                        currentUrlInput = urlToLoad
                                        onUrlChange(urlToLoad)
                                        webViewInstance?.loadUrl(urlToLoad)
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color(0xFFE2E8F0),
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("url_input_field")
                            )
                        }
                    }
                }
            }
        }

        // Progress bar when loading
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = CyanNeon,
                trackColor = Color(0xFF1E293B)
            )
        }

        // Central WebView area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF090D16))
                .testTag("central_webview_container")
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "$userAgentString NaveHub/1.0"
                        }

                        // Add JS Bridge for isolated session storage
                        addJavascriptInterface(isolationManager.createJavascriptBridge(), "NaveHubBridge")

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                // CRITICAL: Keep all navigation inside NaveHub!
                                // Never open external browser or intent
                                val url = request?.url?.toString() ?: return false
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    view?.loadUrl(url)
                                    currentUrlInput = url
                                    onUrlChange(url)
                                }
                                return true
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let {
                                    currentUrlInput = it
                                    onUrlChange(it)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false

                                // Inject isolation polyfill on finished
                                view?.let {
                                    isolationManager.injectIsolationPolyfill(it, account.id, platform.id)
                                }
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                isLoading = false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress / 100f
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }
                        }

                        // Initial load
                        if (isSandboxMode) {
                            val html = SandboxHtmlGenerator.generateHtml(platform, account)
                            loadDataWithBaseURL("https://${platform.id}.navehub.local/", html, "text/html", "UTF-8", null)
                        } else {
                            loadUrl(account.currentUrl)
                        }

                        webViewInstance = this
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
