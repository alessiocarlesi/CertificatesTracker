package com.example.certificatestracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 1. Incrementiamo la versione a 14 e aggiungiamo la nuova Entity
@Database(
    entities = [Certificate::class, UnderlyingPrice::class],
    version = 14,
    exportSchema = false
)
abstract class CertificatesDatabase : RoomDatabase() {
    abstract fun certificatesDao(): CertificatesDao
    abstract fun underlyingPriceDao(): UnderlyingPriceDao // 🔹 Nuovo accesso ai prezzi persistenti

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

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                for (i in 1..6) {
                    database.execSQL("ALTER TABLE certificates ADD COLUMN und$i TEXT")
                    database.execSQL("ALTER TABLE certificates ADD COLUMN und${i}Strike REAL NOT NULL DEFAULT 0.0")
                }
                database.execSQL("ALTER TABLE certificates ADD COLUMN barrierPerc REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE certificates ADD COLUMN bonusPerc REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE certificates ADD COLUMN autocallPerc REAL NOT NULL DEFAULT 0.0")
                database.execSQL("UPDATE certificates SET und1 = underlyingName, und1Strike = strike")
            }
        }

        // 2. NUOVA MIGRAZIONE 13 -> 14: Creazione della tabella prezzi
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Creiamo la tabella per i prezzi persistenti dei sottostanti
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `underlying_prices` (
                        `ticker` TEXT NOT NULL, 
                        `price` REAL NOT NULL, 
                        `lastUpdate` TEXT NOT NULL, 
                        PRIMARY KEY(`ticker`)
                    )
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
                    // 3. Aggiungiamo la nuova migrazione alla catena
                    .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}