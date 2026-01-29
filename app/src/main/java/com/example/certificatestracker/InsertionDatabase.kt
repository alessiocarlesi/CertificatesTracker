package com.example.certificatestracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CertificateInsertion::class], version = 1)
abstract class InsertionDatabase : RoomDatabase() {
    abstract fun insertionDao(): CertificateInsertionDao

    companion object {
        @Volatile
        private var INSTANCE: InsertionDatabase? = null

        fun getDatabase(context: Context): InsertionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InsertionDatabase::class.java,
                    "insertion_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
