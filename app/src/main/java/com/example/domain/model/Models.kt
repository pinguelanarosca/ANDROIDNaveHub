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
    val lastActiveTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

fun isAccessedToday(timestamp: Long): Boolean {
    if (timestamp <= 0L) return false
    val calLast = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val calNow = java.util.Calendar.getInstance()
    return calLast.get(java.util.Calendar.YEAR) == calNow.get(java.util.Calendar.YEAR) &&
           calLast.get(java.util.Calendar.DAY_OF_YEAR) == calNow.get(java.util.Calendar.DAY_OF_YEAR)
}

fun Account.isAccessedToday(): Boolean = isAccessedToday(lastActiveTimestamp)

fun isPlatformAllAccessedToday(accounts: List<Account>): Boolean {
    if (accounts.isEmpty()) return true
    return accounts.all { it.isAccessedToday() }
}

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
    val webViewId: String,
    val boundProfileName: String,
    val validationResult: String,
    val isMultiProfileSupported: Boolean,
    val isCustomProfile: Boolean,
    val profileStatus: String
)

data class IsolationCriterionResult(
    val id: Int,
    val category: String,
    val title: String,
    val description: String,
    val passed: Boolean,
    val status: String = if (passed) "PASS" else "FAIL", // "PASS", "FAIL", "UNSUPPORTED", "NOT_TESTED"
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
