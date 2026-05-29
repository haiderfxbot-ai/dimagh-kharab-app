package com.dimaghkharab.guardian.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dimaghkharab.guardian.data.entity.UsageLog
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLogDao {

    @Query("SELECT * FROM usage_logs ORDER BY startTime DESC")
    fun getAll(): Flow<List<UsageLog>>

    @Query("SELECT * FROM usage_logs WHERE date = :date ORDER BY startTime DESC")
    suspend fun getByDate(date: String): List<UsageLog>

    @Query("SELECT SUM(durationSeconds) FROM usage_logs WHERE date = :date")
    suspend fun getTodayTotalDuration(date: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: UsageLog): Long

    @Query("SELECT * FROM usage_logs WHERE packageName = :packageName ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestForPackage(packageName: String): UsageLog?

    @Update
    suspend fun update(log: UsageLog)
}
