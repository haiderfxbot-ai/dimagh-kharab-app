package com.dimaghkharab.guardian.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intruder_photos")
data class IntruderPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val filePath: String,
    val timestamp: Long,
    val unlockType: String
)
