package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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
import com.example.manager.AccountWebViewPool
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyanNeon

@Composable
fun NaveWebViewContainer(
    account: Account,
    platform: Platform,
    webViewPool: AccountWebViewPool,
    isSandboxMode: Boolean,
    onUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentUrlInput by remember(account.id, isSandboxMode) {
        mutableStateOf(if (isSandboxMode) "navehub://sandbox/${platform.id}/${account.name}" else account.currentUrl)
    }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableFloatStateOf(0f) }

    // Retrieve or create the dedicated WebView instance from pool
    val webView = remember(account.id) {
        webViewPool.getOrCreateWebView(
            account = account,
            platform = platform,
            isSandboxMode = isSandboxMode,
            onUrlChanged = { newUrl ->
                currentUrlInput = newUrl
                onUrlChange(newUrl)
            }
        )
    }

    // Update navigation states
    LaunchedEffect(webView) {
        canGoBack = webView.canGoBack()
        canGoForward = webView.canGoForward()
    }

    // Hardware back navigation
    BackHandler(enabled = canGoBack) {
        webView.goBack()
        canGoBack = webView.canGoBack()
        canGoForward = webView.canGoForward()
    }

    // Effect on mode change
    LaunchedEffect(isSandboxMode) {
        if (isSandboxMode) {
            val html = SandboxHtmlGenerator.generateHtml(
                platform = platform,
                account = account,
                isNativeProfile = true,
                profileName = "navehub_profile_${account.id}"
            )
            webView.loadDataWithBaseURL("https://${platform.id}.navehub.local/", html, "text/html", "UTF-8", null)
        } else {
            if (!webView.url.orEmpty().startsWith("http")) {
                webView.loadUrl(account.currentUrl)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Control Bar
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
                IconButton(
                    onClick = {
                        webView.goBack()
                        canGoBack = webView.canGoBack()
                        canGoForward = webView.canGoForward()
                    },
                    enabled = canGoBack,
                    modifier = Modifier.size(36.dp).testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = if (canGoBack) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        webView.goForward()
                        canGoBack = webView.canGoBack()
                        canGoForward = webView.canGoForward()
                    },
                    enabled = canGoForward,
                    modifier = Modifier.size(36.dp).testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Avançar",
                        tint = if (canGoForward) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (isSandboxMode) {
                            val html = SandboxHtmlGenerator.generateHtml(platform, account)
                            webView.loadDataWithBaseURL("https://${platform.id}.navehub.local/", html, "text/html", "UTF-8", null)
                        } else {
                            webView.reload()
                        }
                    },
                    modifier = Modifier.size(36.dp).testTag("nav_reload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recarregar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

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
                                text = "navehub://${platform.name}/${account.name} (Sandbox Isolado)",
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
                                        webView.loadUrl(urlToLoad)
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

        // Central WebView area hosting the dedicated account WebView
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF090D16))
                .testTag("central_webview_container")
        ) {
            AndroidView(
                factory = { _ ->
                    // Remove from previous parent if attached
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView
                },
                update = { view ->
                    if (view != webView) {
                        (view.parent as? ViewGroup)?.removeView(view)
                        (webView.parent as? ViewGroup)?.removeView(webView)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
