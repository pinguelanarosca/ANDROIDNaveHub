package com.example.domain.model

enum class StorageType {
    LOCAL,
    SESSION
}

data class Platform(
    val id: String,
    val name: String,
    val defaultUrl: String,
    val accentColorHex: String,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Account(
    val id: String,
    val platformId: String,
    val name: String,
    val currentUrl: String,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

data class CookieItem(
    val accountId: String,
    val domain: String,
    val name: String,
    val value: String,
    val path: String = "/",
    val isSecure: Boolean = false,
    val isHttpOnly: Boolean = false,
    val expires: Long = 0L
)

data class StorageItem(
    val accountId: String,
    val storageType: StorageType,
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProfileDiagnostics(
    val accountId: String,
    val profileName: String,
    val isMultiProfileSupported: Boolean,
    val isCustomProfile: Boolean,
    val profileStatus: String,
    val webViewId: String
)

data class IsolationCriterionResult(
    val id: Int,
    val category: String, // "ISOLAMENTO NATIVO DO WEBVIEW", "ISOLAMENTO DO ROOM", "PERSISTÊNCIA", "AUTENTICAÇÃO", "SERVICE WORKER", "CACHE", "BRIDGE", "NAVEGAÇÃO", "RECUPERAÇÃO APÓS REINICIALIZAÇÃO"
    val title: String,
    val description: String,
    val passed: Boolean,
    val details: String,
    val evidence: String = ""
)

data class IsolationAuditReport(
    val timestamp: Long,
    val allPassed: Boolean,
    val passedCount: Int,
    val totalCount: Int,
    val isMultiProfileSupported: Boolean,
    val criteriaResults: List<IsolationCriterionResult>
)
