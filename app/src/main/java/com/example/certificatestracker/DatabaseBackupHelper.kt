package com.example.certificatestracker

import android.content.Context
import androidx.room.Room
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DatabaseBackupHelper {

    private const val DATABASE_NAME = "certificates.db"
    private const val BACKUP_FOLDER = "backups"
    private const val DATE_FORMAT = "yyyyMMdd_HHmmss"

    /**
     * Restituisce un'istanza del database in modo sicuro.
     * Se il database è corrotto, tenta di ripristinare il backup più recente.
     */
    fun getSafeDatabase(context: Context): CertificatesDatabase {
        return try {
            Room.databaseBuilder(
                context.applicationContext,
                CertificatesDatabase::class.java,
                DATABASE_NAME
            ).build()
        } catch (e: Exception) {
            e.printStackTrace()
            println("⚠️ Database corrotto! Ripristino in corso...")
            restoreDatabase(context)
            // Riprova ad aprire il DB dopo il restore
            Room.databaseBuilder(
                context.applicationContext,
                CertificatesDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }

    /**
     * Salva un backup del database corrente.
     * Il file di backup contiene timestamp nel nome.
     */
    fun backupDatabase(context: Context) {
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) return

            val backupDir = File(context.filesDir, BACKUP_FOLDER)
            if (!backupDir.exists()) backupDir.mkdir()

            val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
            val backupFile = File(backupDir, "backup_$timestamp.db")

            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            println("✅ Backup creato: ${backupFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Ripristina il backup più recente.
     * Sovrascrive il database corrotto.
     */
    fun restoreDatabase(context: Context) {
        try {
            val backupDir = File(context.filesDir, BACKUP_FOLDER)
            if (!backupDir.exists()) return

            val backups = backupDir.listFiles { file ->
                file.isFile && file.name.endsWith(".db")
            }?.sortedByDescending { it.lastModified() }

            val latestBackup = backups?.firstOrNull() ?: return

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            FileInputStream(latestBackup).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            println("✅ Database ripristinato dal backup: ${latestBackup.name}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
