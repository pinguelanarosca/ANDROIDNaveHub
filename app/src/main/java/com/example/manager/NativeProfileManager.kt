package com.example.manager

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.domain.model.ProfileDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages native Multi-Profile instances via AndroidX WebKit.
 * Verifies `WebViewFeature.MULTI_PROFILE` support in runtime.
 * Ensures each Account is assigned a dedicated persistent Profile:
 * "navehub_profile_<accountId>" and never uses the Default Profile.
 */
class NativeProfileManager(private val context: Context) {

    val isMultiProfileSupported: Boolean = try {
        WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
    } catch (e: Throwable) {
        false
    }

    private val profileStore: ProfileStore? = if (isMultiProfileSupported) {
        try {
            ProfileStore.getInstance()
        } catch (e: Throwable) {
            null
        }
    } else {
        null
    }

    fun getProfileNameForAccount(accountId: String): String {
        return "navehub_profile_$accountId"
    }

    /**
     * Retrieves or creates a custom persistent profile for the given account.
     * Guaranteed to never return Profile.DEFAULT_PROFILE for NaveHub accounts.
     */
    fun getOrCreateProfile(accountId: String): Profile? {
        val store = profileStore ?: return null
        val profileName = getProfileNameForAccount(accountId)
        try {
            (context.applicationContext as? com.example.NaveHubApplication)?.ensureProfileCacheDir(profileName)
        } catch (e: Throwable) {
            // Ignore
        }
        return try {
            store.getOrCreateProfile(profileName)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Associates a WebView with the Account's native Profile before any navigation.
     * CRITICAL: Must be called before loadUrl, evaluateJavascript, or page loads.
     */
    fun bindWebViewToAccountProfile(webView: WebView, accountId: String): Boolean {
        if (!isMultiProfileSupported) return false
        val profile = getOrCreateProfile(accountId) ?: return false
        return try {
            WebViewCompat.setProfile(webView, profile.name)
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Returns the CookieManager specific to this account's profile if supported.
     */
    fun getCookieManagerForAccount(accountId: String): CookieManager? {
        val profile = getOrCreateProfile(accountId)
        return if (profile != null) {
            try {
                profile.cookieManager
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Returns the WebStorage specific to this account's profile if supported.
     */
    fun getWebStorageForAccount(accountId: String): WebStorage? {
        val profile = getOrCreateProfile(accountId)
        return if (profile != null) {
            try {
                profile.webStorage
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Deletes the profile and all its persistent data from disk when an account is removed.
     */
    suspend fun deleteProfile(accountId: String): Boolean = withContext(Dispatchers.IO) {
        val store = profileStore ?: return@withContext false
        val profileName = getProfileNameForAccount(accountId)
        return@withContext try {
            store.deleteProfile(profileName)
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Retrieves diagnostics about the profile binding state.
     */
    fun getDiagnostics(accountId: String, webView: WebView?): ProfileDiagnostics {
        val profileName = getProfileNameForAccount(accountId)
        val profile = if (isMultiProfileSupported) getOrCreateProfile(accountId) else null

        val currentBoundProfileName = if (webView != null && isMultiProfileSupported) {
            try {
                WebViewCompat.getProfile(webView).name
            } catch (e: Throwable) {
                "error: ${e.message}"
            }
        } else {
            if (isMultiProfileSupported) "unbound" else "MULTI_PROFILE_UNSUPPORTED"
        }

        val defaultProfileName = "Default"
        val isCustom = profile != null && profile.name != defaultProfileName && currentBoundProfileName == profileName

        val status = when {
            !isMultiProfileSupported -> "FALLBACK_LEGACY_ISOLATION"
            profile == null -> "PROFILE_CREATION_FAILED"
            currentBoundProfileName == defaultProfileName -> "FAIL_DEFAULT_PROFILE_USED"
            currentBoundProfileName == profileName -> "NATIVE_PROFILE_ACTIVE"
            else -> "MISMATCH ($currentBoundProfileName != $profileName)"
        }

        return ProfileDiagnostics(
            accountId = accountId,
            profileName = profileName,
            isMultiProfileSupported = isMultiProfileSupported,
            isCustomProfile = isCustom,
            profileStatus = status,
            webViewId = webView?.let { "WebView@${Integer.toHexString(it.hashCode())}" } ?: "none"
        )
    }
}
