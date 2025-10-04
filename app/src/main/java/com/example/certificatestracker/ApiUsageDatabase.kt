package com.example.certificatestracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ApiUsage::class], version =2, exportSchema = false)
abstract class ApiUsageDatabase : RoomDatabase() {

    abstract fun apiUsageDao(): ApiUsageDao

    companion object {
        @Volatile
        private var INSTANCE: ApiUsageDatabase? = null

        fun getDatabase(context: Context): ApiUsageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApiUsageDatabase::class.java,
                    "api_usage_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
