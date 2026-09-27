package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Platform

@Entity(tableName = "platforms")
data class PlatformEntity(
    @PrimaryKey val id: String,
    val name: String,
    val defaultUrl: String,
    val accentColorHex: String,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Platform(
        id = id,
        name = name,
        defaultUrl = defaultUrl,
        accentColorHex = accentColorHex,
        isCustom = isCustom,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(platform: Platform) = PlatformEntity(
            id = platform.id,
            name = platform.name,
            defaultUrl = platform.defaultUrl,
            accentColorHex = platform.accentColorHex,
            isCustom = platform.isCustom,
            createdAt = platform.createdAt
        )
    }
}
