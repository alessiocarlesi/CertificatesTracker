package com.example.certificatestracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CertificateInsertionDao {

    @Query("SELECT * FROM certificate_insertions WHERE isin = :isin")
    suspend fun getByIsin(isin: String): CertificateInsertion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CertificateInsertion)
}
