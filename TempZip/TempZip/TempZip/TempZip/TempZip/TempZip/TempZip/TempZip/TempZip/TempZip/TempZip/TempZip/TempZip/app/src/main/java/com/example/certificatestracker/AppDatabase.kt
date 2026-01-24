package com.example.certificatestracker

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Certificate::class, ApiUsage::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun certificatesDao(): CertificatesDao
    abstract fun apiUsageDao(): ApiUsageDao
}
