// filename: DatabaseBackupHelper.kt
package com.example.certificatestracker

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DatabaseBackupHelper {

    private const val DATABASE_NAME = "certificates_db"
    private const val BACKUP_FOLDER = "backups"
    private const val DATE_FORMAT = "yyyyMMdd_HHmmss"

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