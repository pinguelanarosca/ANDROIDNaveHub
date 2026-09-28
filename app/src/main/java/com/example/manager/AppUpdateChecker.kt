package com.example.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val latestVersion: String,
    val hasUpdate: Boolean,
    val releaseNotes: String,
    val downloadUrl: String,
    val htmlUrl: String,
    val publishedAt: String = ""
)

class AppUpdateChecker(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val directApkUrl = "https://raw.githubusercontent.com/pinguelanarosca/ANDROIDNaveHub/main/release-apk/ANDROIDNaveHub.apk"
    val repoUrl = "https://github.com/pinguelanarosca/ANDROIDNaveHub"

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/pinguelanarosca/ANDROIDNaveHub/releases/latest")
                .header("User-Agent", "NaveHub-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string().orEmpty()
                val json = JSONObject(jsonStr)
                val tagName = json.optString("tag_name", "v1.0")
                val releaseName = json.optString("name", tagName)
                val body = json.optString("body", "Atualização mais recente disponível no GitHub.")
                val htmlUrl = json.optString("html_url", repoUrl)
                val publishedAt = json.optString("published_at", "")

                var apkUrl = directApkUrl
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", directApkUrl)
                            break
                        }
                    }
                }

                val currentVer = BuildConfig.VERSION_NAME
                val cleanTag = tagName.removePrefix("v").removePrefix("V").trim()
                val isNewer = cleanTag != currentVer

                UpdateInfo(
                    currentVersion = currentVer,
                    latestVersion = releaseName,
                    hasUpdate = isNewer,
                    releaseNotes = body,
                    downloadUrl = apkUrl,
                    htmlUrl = htmlUrl,
                    publishedAt = publishedAt
                )
            } else {
                UpdateInfo(
                    currentVersion = BuildConfig.VERSION_NAME,
                    latestVersion = "Última Versão (Main)",
                    hasUpdate = true,
                    releaseNotes = "APK pronto e disponível para download direto do repositório no GitHub.",
                    downloadUrl = directApkUrl,
                    htmlUrl = repoUrl
                )
            }
        } catch (e: Exception) {
            UpdateInfo(
                currentVersion = BuildConfig.VERSION_NAME,
                latestVersion = "Versão Mais Recente",
                hasUpdate = true,
                releaseNotes = "Download do APK mais recente do repositório oficial no GitHub.",
                downloadUrl = directApkUrl,
                htmlUrl = repoUrl
            )
        }
    }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
