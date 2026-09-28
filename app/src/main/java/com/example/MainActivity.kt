package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NaveHubApp
import com.example.ui.NaveHubViewModel
import com.example.ui.theme.NaveHubTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pre-create WebView cache directories to prevent Chromium simple index file enumerator errors
        try {
            val webViewCache = File(cacheDir, "WebView")
            File(webViewCache, "Default/HTTP Cache/Code Cache/wasm").mkdirs()
            File(webViewCache, "Default/HTTP Cache/Code Cache/js").mkdirs()
        } catch (_: Throwable) {
        }

        enableEdgeToEdge()
        setContent {
            NaveHubTheme {
                val viewModel: NaveHubViewModel = viewModel()
                NaveHubApp(viewModel = viewModel)
            }
        }
    }
}
