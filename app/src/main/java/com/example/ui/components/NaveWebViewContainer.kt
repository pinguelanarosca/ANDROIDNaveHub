package com.example.ui.components

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.TextStyle
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
    isFullscreen: Boolean,
    onToggleFullscreen: (Boolean) -> Unit,
    onUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentUrlInput by remember(account.id) {
        mutableStateOf(account.currentUrl)
    }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Address bar is hidden by default across opening/switching accounts or platforms
    var isAddressBarExpanded by remember(account.id, platform.id) {
        mutableStateOf(false)
    }

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
        ).also { wv ->
            if (wv.url.isNullOrBlank() && account.currentUrl.isNotBlank()) {
                wv.loadUrl(account.currentUrl)
            }
        }
    }

    // Refresh navigation states
    LaunchedEffect(account.id) {
        val currentLoadedUrl = webView.url
        if (!currentLoadedUrl.isNullOrBlank()) {
            currentUrlInput = currentLoadedUrl
        } else {
            currentUrlInput = account.currentUrl
        }
        canGoBack = webView.canGoBack()
        canGoForward = webView.canGoForward()
    }

    // Track and direct all Back commands into the WebView
    BackHandler(enabled = true) {
        val now = System.currentTimeMillis()
        if (isFullscreen) {
            // Fullscreen mode: double press exits fullscreen, single press navigates back
            if (now - lastBackPressTime < 2000L) {
                onToggleFullscreen(false)
                lastBackPressTime = 0L
                Toast.makeText(context, "Modo ecrã inteiro desativado", Toast.LENGTH_SHORT).show()
            } else {
                lastBackPressTime = now
                if (webView.canGoBack()) {
                    webView.goBack()
                    canGoBack = webView.canGoBack()
                    canGoForward = webView.canGoForward()
                } else {
                    webView.evaluateJavascript("if (window.history.length > 1) { window.history.back(); }", null)
                    Toast.makeText(context, "Prima 'Voltar' novamente para sair do ecrã inteiro", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Normal mode: command is tracked and directed into the WebView
            if (webView.canGoBack()) {
                webView.goBack()
                canGoBack = webView.canGoBack()
                canGoForward = webView.canGoForward()
            } else {
                // If webView.canGoBack() is false, also check client-side SPA router history
                webView.evaluateJavascript(
                    "(function() { if (window.history.length > 1) { window.history.back(); return true; } return false; })()"
                ) { result ->
                    val handledBySpa = result == "true"
                    if (!handledBySpa) {
                        // At root page of WebView: require double-press to exit app so accidental clicks don't close it
                        if (now - lastBackPressTime < 2000L) {
                            (context as? Activity)?.finish()
                        } else {
                            lastBackPressTime = now
                            Toast.makeText(context, "Pressione 'Voltar' novamente para sair do aplicativo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Control Bar (Hidden by default, expandable via arrow; hidden in fullscreen)
        if (!isFullscreen) {
            AnimatedVisibility(
                visible = isAddressBarExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
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
                                if (webView.canGoBack()) {
                                    webView.goBack()
                                    canGoBack = webView.canGoBack()
                                    canGoForward = webView.canGoForward()
                                } else {
                                    webView.evaluateJavascript("window.history.back()", null)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("nav_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (webView.canGoForward()) {
                                    webView.goForward()
                                    canGoBack = webView.canGoBack()
                                    canGoForward = webView.canGoForward()
                                } else {
                                    webView.evaluateJavascript("window.history.forward()", null)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("nav_forward_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Avançar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                webView.reload()
                                canGoBack = webView.canGoBack()
                                canGoForward = webView.canGoForward()
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

                        // Address URL input box
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
                                    textStyle = TextStyle(
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
                                            val urlToLoad = formatUrlInput(currentUrlInput)
                                            currentUrlInput = urlToLoad
                                            onUrlChange(urlToLoad)
                                            webView.loadUrl(urlToLoad)
                                        }
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (currentUrlInput.isEmpty()) {
                                                Text(
                                                    "Digite uma URL...",
                                                    color = Color(0xFF64748B),
                                                    fontSize = 12.sp
                                                )
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
                                        val urlToLoad = formatUrlInput(currentUrlInput)
                                        currentUrlInput = urlToLoad
                                        onUrlChange(urlToLoad)
                                        webView.loadUrl(urlToLoad)
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("nav_go_button")
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

                        Spacer(modifier = Modifier.width(6.dp))

                        // Maximize (Fullscreen) Button
                        IconButton(
                            onClick = { onToggleFullscreen(true) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("maximize_webview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Maximizar WebView (Ecrã Inteiro)",
                                tint = CyanNeon,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Slim expand/collapse arrow strip directly under the tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(Color(0xFF0F172A))
                    .clickable { isAddressBarExpanded = !isAddressBarExpanded }
                    .testTag("toggle_address_bar_arrow"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAddressBarExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isAddressBarExpanded) "Ocultar barra de endereços" else "Expandir barra de endereços",
                    tint = CyanNeon,
                    modifier = Modifier.size(16.dp)
                )
            }
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
                    FrameLayout(ctx).apply {
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

            // In Fullscreen mode: Floating exit button at top-right
            if (isFullscreen) {
                IconButton(
                    onClick = { onToggleFullscreen(false) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xAA0F172A))
                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("exit_fullscreen_floating_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Sair do Ecrã Inteiro",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatUrlInput(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        trimmed
    } else {
        "https://$trimmed"
    }
}
