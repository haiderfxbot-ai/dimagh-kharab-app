package com.dimaghkharab.guardian.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_thresholds")
data class BatteryThreshold(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val percentage: Int,
    val name: String,
    val filePath: String,
    val volume: Int,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "triggered_today")
    val triggeredToday: Boolean = false
)
