// filename: CertificatesDatabase.kt
package com.example.certificatestracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Certificate::class], version = 11, exportSchema = false)
abstract class CertificatesDatabase : RoomDatabase() {
    abstract fun certificatesDao(): CertificatesDao

    companion object {
        @Volatile
        private var INSTANCE: CertificatesDatabase? = null

        // Migrazione da versioni precedenti a 11 (aggiunta campo purchasePrice)
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Aggiunge la colonna purchasePrice senza perdere dati esistenti
                database.execSQL("ALTER TABLE certificates ADD COLUMN purchasePrice REAL")
            }
        }

        fun getDatabase(context: Context): CertificatesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        CertificatesDatabase::class.java,
                        "certificates_db"
                    )
                        .addMigrations(MIGRATION_10_11)
                        .build()
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("⚠️ Errore apertura DB: tentativo di restore da backup...")
                    // Se fallisce, prova a ripristinare backup
                    DatabaseBackupHelper.restoreDatabase(context)
                    Room.databaseBuilder(
                        context.applicationContext,
                        CertificatesDatabase::class.java,
                        "certificates_db"
                    )
                        .addMigrations(MIGRATION_10_11)
                        .build()
                }
                INSTANCE = instance
                instance
            }
        }
    }
}
