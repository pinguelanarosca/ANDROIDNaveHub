package com.example.ui.components

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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
    var currentUrlInput by remember(account.id) {
        mutableStateOf(account.currentUrl)
    }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Retrieve or create the dedicated WebView instance from pool
    val webView = remember(account.id) {
        webViewPool.getOrCreateWebView(
            account = account,
            platform = platform,
            isSandboxMode = false,
            onUrlChanged = { newUrl ->
                currentUrlInput = newUrl
                onUrlChange(newUrl)
            }
        )
    }

    // Sync input and load account URL if account changes or currentUrl changes
    LaunchedEffect(account.id, account.currentUrl) {
        currentUrlInput = account.currentUrl
        if (webView.url.isNull_or_empty_or_different_from(account.currentUrl)) {
            webView.loadUrl(account.currentUrl)
        }
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
                    onClick = { webView.reload() },
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

                        BasicTextField(
                            value = currentUrlInput,
                            onValueChange = { currentUrlInput = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(CyanNeon),
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
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                    if (currentUrlInput.isEmpty()) {
                                        Text("Digite uma URL...", color = Color(0xFF64748B), fontSize = 12.sp)
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("url_input_field")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                val urlToLoad = if (currentUrlInput.startsWith("http://") || currentUrlInput.startsWith("https://")) {
                                    currentUrlInput
                                } else {
                                    "https://$currentUrlInput"
                                }
                                currentUrlInput = urlToLoad
                                onUrlChange(urlToLoad)
                                webView.loadUrl(urlToLoad)
                            },
                            modifier = Modifier.size(24.dp).testTag("nav_go_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Navegar",
                                tint = CyanNeon,
                                modifier = Modifier.size(14.dp)
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
                factory = { ctx ->
                    android.widget.FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { container ->
                    val currentChild = container.getChildAt(0)
                    if (currentChild != webView) {
                        container.removeAllViews()
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        container.addView(webView)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun String?.isNull_or_empty_or_different_from(targetUrl: String): Boolean {
    if (this.isNullOrBlank()) return true
    return this != targetUrl && !this.startsWith(targetUrl)
}
