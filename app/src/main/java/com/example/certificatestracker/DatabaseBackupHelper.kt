// filename: DatabaseBackupHelper.kt
package com.example.certificatestracker

import android.content.Context
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DatabaseBackupHelper {

    private const val DATABASE_NAME = "certificates_db"
    private const val BACKUP_FOLDER = "backups"
    private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
    private const val MAX_BACKUPS = 3

    /**
     * Crea un backup .db apribile, mantenendo solo gli ultimi 3.
     */
    fun backupDatabase(context: Context) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) {
            println("⚠️ Nessun database trovato per il backup.")
            return
        }

        val backupDir = File(context.filesDir, BACKUP_FOLDER)
        if (!backupDir.exists()) backupDir.mkdirs()

        // ✅ Esegui il checkpoint WAL per consolidare tutto nel .db
        try {
            context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null)
                .execSQL("PRAGMA wal_checkpoint(FULL);")
            println("✅ Checkpoint WAL completato.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("⚠️ Checkpoint WAL fallito: il backup potrebbe non essere perfetto.")
        }

        // ✅ Copia il file principale .db
        val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
        val backupFile = File(backupDir, "backup_$timestamp.db")

        try {
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            println("✅ Backup creato: ${backupFile.name} (${backupFile.length()} bytes)")
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Errore durante la copia del file di backup.")
            return
        }

        // ✅ Mantieni solo gli ultimi 3 backup
        val backups = backupDir.listFiles { f ->
            f.isFile && f.name.startsWith("backup_") && f.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        if (backups.size > MAX_BACKUPS) {
            backups.drop(MAX_BACKUPS).forEach { old ->
                if (old.delete()) println("🧽 Eliminato backup vecchio: ${old.name}")
            }
        }
    }

    /**
     * Ripristina l’ultimo backup disponibile.
     */
    fun restoreDatabase(context: Context) {
        val backupDir = File(context.filesDir, BACKUP_FOLDER)
        if (!backupDir.exists()) {
            println("⚠️ Nessuna cartella backup trovata.")
            return
        }

        val backups = backupDir.listFiles { f ->
            f.isFile && f.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() }

        val latest = backups?.firstOrNull()
        if (latest == null) {
            println("⚠️ Nessun backup disponibile per il ripristino.")
            return
        }

        val dbFile = context.getDatabasePath(DATABASE_NAME)
        try {
            FileInputStream(latest).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            println("✅ Database ripristinato da ${latest.name}")
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Errore durante il ripristino del backup.")
        }
    }

    /**
     * Opzionale: wrapper per usare il restore automatico con Room.
     */
    fun <T : RoomDatabase> getSafeDatabase(
        context: Context,
        dbClass: Class<T>,
        migrations: Array<Migration>
    ): T {
        return try {
            androidx.room.Room.databaseBuilder(context.applicationContext, dbClass, DATABASE_NAME)
                .addMigrations(*migrations)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            println("⚠️ Errore apertura DB: tentativo di restore da backup...")
            restoreDatabase(context)
            androidx.room.Room.databaseBuilder(context.applicationContext, dbClass, DATABASE_NAME)
                .addMigrations(*migrations)
                .build()
        }
    }
}
