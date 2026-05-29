package com.dimaghkharab.guardian.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dimaghkharab.guardian.data.entity.IntruderPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface IntruderPhotoDao {

    @Query("SELECT * FROM intruder_photos ORDER BY timestamp DESC")
    fun getAll(): Flow<List<IntruderPhoto>>

    @Query("SELECT * FROM intruder_photos ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<IntruderPhoto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: IntruderPhoto): Long

    @Delete
    suspend fun delete(photo: IntruderPhoto)

    @Query("DELETE FROM intruder_photos")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM intruder_photos")
    suspend fun getCount(): Int
}
