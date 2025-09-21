package com.example.certificatestracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificatesDao {

    @Query("SELECT * FROM certificates ORDER BY isin ASC")
    fun getAllFlow(): Flow<List<CertificateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(certificate: CertificateEntity)

    @Delete
    suspend fun delete(certificate: CertificateEntity)
}
