package com.dimaghkharab.guardian.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dimaghkharab.guardian.data.dao.BatteryThresholdDao
import com.dimaghkharab.guardian.data.dao.IntruderPhotoDao
import com.dimaghkharab.guardian.data.dao.SoundProfileDao
import com.dimaghkharab.guardian.data.dao.UsageLogDao
import com.dimaghkharab.guardian.data.entity.BatteryThreshold
import com.dimaghkharab.guardian.data.entity.IntruderPhoto
import com.dimaghkharab.guardian.data.entity.SoundProfile
import com.dimaghkharab.guardian.data.entity.UsageLog

@Database(
    entities = [SoundProfile::class, BatteryThreshold::class, IntruderPhoto::class, UsageLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun soundProfileDao(): SoundProfileDao
    abstract fun batteryThresholdDao(): BatteryThresholdDao
    abstract fun intruderPhotoDao(): IntruderPhotoDao
    abstract fun usageLogDao(): UsageLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "guardian_database"
            ).fallbackToDestructiveMigration().build()
        }
    }
}
