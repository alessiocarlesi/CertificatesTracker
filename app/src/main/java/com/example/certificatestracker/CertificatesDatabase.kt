package com.example.certificatestracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 1. Incrementiamo la versione a 13
@Database(entities = [Certificate::class], version = 13, exportSchema = false)
abstract class CertificatesDatabase : RoomDatabase() {
    abstract fun certificatesDao(): CertificatesDao

    companion object {
        @Volatile
        private var INSTANCE: CertificatesDatabase? = null

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE certificates ADD COLUMN purchasePrice REAL")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE certificates ADD COLUMN underlyingPrice REAL NOT NULL DEFAULT 0.0")
            }
        }

        // 2. NUOVA MIGRAZIONE 12 -> 13: La struttura Worst-Of
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Slot per i 6 sottostanti e i loro strike
                for (i in 1..6) {
                    database.execSQL("ALTER TABLE certificates ADD COLUMN und$i TEXT")
                    database.execSQL("ALTER TABLE certificates ADD COLUMN und${i}Strike REAL NOT NULL DEFAULT 0.0")
                }

                // Percentuali strategiche uniche
                database.execSQL("ALTER TABLE certificates ADD COLUMN barrierPerc REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE certificates ADD COLUMN bonusPerc REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE certificates ADD COLUMN autocallPerc REAL NOT NULL DEFAULT 0.0")

                // TRASLOCO DATI: Salviamo quello che avevi nella v12
                database.execSQL("""
                    UPDATE certificates 
                    SET und1 = underlyingName, 
                        und1Strike = strike
                """)
            }
        }

        fun getDatabase(context: Context): CertificatesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CertificatesDatabase::class.java,
                    "certificates_db"
                )
                    // 3. Aggiungiamo la migrazione 12_13 alla catena
                    .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}