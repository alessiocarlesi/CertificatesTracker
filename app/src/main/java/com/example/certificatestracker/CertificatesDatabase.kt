package com.example.certificatestracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CertificateEntity::class],  // 👈 deve esserci CertificateEntity
    version = 2,
    exportSchema = false
)
abstract class CertificatesDatabase : RoomDatabase() {
    abstract fun certificatesDao(): CertificatesDao

    companion object {
        @Volatile
        private var INSTANCE: CertificatesDatabase? = null

        fun getDatabase(context: Context): CertificatesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CertificatesDatabase::class.java,
                    "certificates_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
