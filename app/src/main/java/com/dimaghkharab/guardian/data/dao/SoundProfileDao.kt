package com.dimaghkharab.guardian.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dimaghkharab.guardian.data.entity.SoundProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundProfileDao {

    @Query("SELECT * FROM sound_profiles ORDER BY id ASC")
    fun getAll(): Flow<List<SoundProfile>>

    @Query("SELECT * FROM sound_profiles WHERE type = :type ORDER BY id ASC")
    fun getByType(type: String): Flow<List<SoundProfile>>

    @Query("SELECT * FROM sound_profiles WHERE type = :type AND is_active = 1 LIMIT 1")
    suspend fun getActiveByType(type: String): SoundProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: SoundProfile): Long

    @Update
    suspend fun update(profile: SoundProfile)

    @Delete
    suspend fun delete(profile: SoundProfile)
}
