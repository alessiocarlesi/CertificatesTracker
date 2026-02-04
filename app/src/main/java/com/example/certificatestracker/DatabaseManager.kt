package com.example.certificatestracker

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DatabaseManager {

    private const val DB_NAME = "certificates_db"

    fun exportDatabase(context: Context): String {
        return try {
            // Lista dei 3 file che compongono il database SQLite moderno
            val dbFiles = listOf(DB_NAME, "$DB_NAME-shm", "$DB_NAME-wal")
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            // Creiamo una sottocartella nei Download per tenere i file uniti
            val backupFolder = File(downloadDir, "Backup_Certificati_${System.currentTimeMillis()}")
            if (!backupFolder.exists()) backupFolder.mkdirs()

            dbFiles.forEach { fileName ->
                val dbFile = context.getDatabasePath(fileName)
                if (dbFile.exists()) {
                    val outputFile = File(backupFolder, fileName)
                    FileInputStream(dbFile).use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            "✅ Backup completo (3 file) salvato in Download nella cartella: ${backupFolder.name}"
        } catch (e: Exception) {
            "❌ Errore Export: ${e.message}"
        }
    }
}