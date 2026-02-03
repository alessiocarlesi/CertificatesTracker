package com.example.certificatestracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Certificate::class], version = 12, exportSchema = false)
abstract class CertificatesDatabase : RoomDatabase() {
    abstract fun certificatesDao(): CertificatesDao

    companion object {
        @Volatile
        private var INSTANCE: CertificatesDatabase? = null

        // Migrazione precedente (versione 10 -> 11)
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE certificates ADD COLUMN purchasePrice REAL")
            }
        }

        // NUOVA MIGRAZIONE (versione 11 -> 12) per il prezzo del sottostante
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Aggiungiamo la colonna underlyingPrice con valore predefinito 0.0
                database.execSQL("ALTER TABLE certificates ADD COLUMN underlyingPrice REAL NOT NULL DEFAULT 0.0")
            }
        }

        fun getDatabase(context: Context): CertificatesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CertificatesDatabase::class.java,
                    "certificates_db"
                )
                    .addMigrations(MIGRATION_10_11, MIGRATION_11_12) // Entrambe le migrazioni attive
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}