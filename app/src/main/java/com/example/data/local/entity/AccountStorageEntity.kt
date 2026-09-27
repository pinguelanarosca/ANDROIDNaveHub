package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.domain.model.StorageItem
import com.example.domain.model.StorageType

@Entity(
    tableName = "account_storage",
    primaryKeys = ["accountId", "storageType", "key"],
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
data class AccountStorageEntity(
    val accountId: String,
    val storageType: String, // "LOCAL" or "SESSION"
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = StorageItem(
        accountId = accountId,
        storageType = if (storageType == "SESSION") StorageType.SESSION else StorageType.LOCAL,
        key = key,
        value = value,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(item: StorageItem) = AccountStorageEntity(
            accountId = item.accountId,
            storageType = item.storageType.name,
            key = item.key,
            value = item.value,
            updatedAt = item.updatedAt
        )
    }
}
