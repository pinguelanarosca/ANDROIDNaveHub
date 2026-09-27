package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.domain.model.CookieItem

@Entity(
    tableName = "account_cookies",
    primaryKeys = ["accountId", "domain", "name", "path"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"])]
)
data class AccountCookieEntity(
    val accountId: String,
    val domain: String,
    val name: String,
    val value: String,
    val path: String = "/",
    val isSecure: Boolean = false,
    val isHttpOnly: Boolean = false,
    val expires: Long = 0L
) {
    fun toDomain() = CookieItem(
        accountId = accountId,
        domain = domain,
        name = name,
        value = value,
        path = path,
        isSecure = isSecure,
        isHttpOnly = isHttpOnly,
        expires = expires
    )

    companion object {
        fun fromDomain(cookie: CookieItem) = AccountCookieEntity(
            accountId = cookie.accountId,
            domain = cookie.domain,
            name = cookie.name,
            value = cookie.value,
            path = cookie.path,
            isSecure = cookie.isSecure,
            isHttpOnly = cookie.isHttpOnly,
            expires = cookie.expires
        )
    }
}
