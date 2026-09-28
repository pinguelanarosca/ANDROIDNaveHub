package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Account

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = PlatformEntity::class,
            parentColumns = ["id"],
            childColumns = ["platformId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["platformId"])]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val platformId: String,
    val name: String,
    val currentUrl: String,
    val lastActiveTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Account(
        id = id,
        platformId = platformId,
        name = name,
        currentUrl = currentUrl,
        lastActiveTimestamp = lastActiveTimestamp,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(account: Account) = AccountEntity(
            id = account.id,
            platformId = account.platformId,
            name = account.name,
            currentUrl = account.currentUrl,
            lastActiveTimestamp = account.lastActiveTimestamp,
            createdAt = account.createdAt
        )
    }
}
