package com.example

import android.app.Application
import java.io.File

class NaveHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureAllWebViewCacheDirectories()
    }

    fun ensureProfileCacheDir(profileName: String) {
        try {
            val cacheBase = File(cacheDir, "WebView/$profileName/HTTP Cache/Code Cache")
            File(cacheBase, "js").mkdirs()
            File(cacheBase, "wasm").mkdirs()
        } catch (e: Throwable) {
            // Non-critical
        }
    }

    private fun ensureAllWebViewCacheDirectories() {
        try {
            val webViewCacheDir = File(cacheDir, "WebView")
            if (webViewCacheDir.exists() && webViewCacheDir.isDirectory) {
                webViewCacheDir.listFiles()?.forEach { profileFolder ->
                    if (profileFolder.isDirectory) {
                        ensureProfileCacheDir(profileFolder.name)
                    }
                }
            }
            ensureProfileCacheDir("Default")
            ensureProfileCacheDir("Profile 1")
        } catch (e: Throwable) {
            // Non-critical
        }
    }
}
