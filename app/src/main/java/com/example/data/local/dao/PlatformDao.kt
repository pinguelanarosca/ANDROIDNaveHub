package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PlatformEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatformDao {
    @Query("SELECT * FROM platforms ORDER BY isCustom ASC, createdAt ASC")
    fun getAllPlatforms(): Flow<List<PlatformEntity>>

    @Query("SELECT * FROM platforms ORDER BY isCustom ASC, createdAt ASC")
    suspend fun getAllPlatformsSync(): List<PlatformEntity>

    @Query("SELECT * FROM platforms WHERE id = :id LIMIT 1")
    suspend fun getPlatformById(id: String): PlatformEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatform(platform: PlatformEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatforms(platforms: List<PlatformEntity>)

    @Update
    suspend fun updatePlatform(platform: PlatformEntity)

    @Query("DELETE FROM platforms WHERE id = :id")
    suspend fun deletePlatformById(id: String)

    @Query("DELETE FROM platforms")
    suspend fun deleteAllPlatforms()
}
