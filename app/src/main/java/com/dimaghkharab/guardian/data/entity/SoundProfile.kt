package com.dimaghkharab.guardian.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sound_profiles")
data class SoundProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String,
    val name: String,
    val filePath: String,
    val volume: Int,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
