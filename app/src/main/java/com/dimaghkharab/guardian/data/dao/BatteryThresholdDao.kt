package com.dimaghkharab.guardian.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dimaghkharab.guardian.data.entity.BatteryThreshold
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryThresholdDao {

    @Query("SELECT * FROM battery_thresholds ORDER BY percentage ASC")
    fun getAll(): Flow<List<BatteryThreshold>>

    @Query("SELECT * FROM battery_thresholds WHERE percentage = :percentage LIMIT 1")
    suspend fun getByPercentage(percentage: Int): BatteryThreshold?

    @Query("SELECT * FROM battery_thresholds WHERE is_active = 1")
    suspend fun getActiveThresholds(): List<BatteryThreshold>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(threshold: BatteryThreshold): Long

    @Update
    suspend fun update(threshold: BatteryThreshold)

    @Delete
    suspend fun delete(threshold: BatteryThreshold)

    @Query("UPDATE battery_thresholds SET triggered_today = 0")
    suspend fun resetTriggeredToday()
}
