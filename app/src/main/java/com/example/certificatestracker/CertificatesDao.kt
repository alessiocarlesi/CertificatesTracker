package com.example.certificatestracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificatesDao {
    @Query("SELECT * FROM certificates")
    fun getAllFlow(): Flow<List<Certificate>>

    @Insert
    suspend fun insert(certificate: Certificate)

    @Query("DELETE FROM certificates WHERE isin = :isin")
    suspend fun delete(isin: String)

    @Query("UPDATE certificates SET lastPrice = :price WHERE isin = :isin")
    suspend fun updatePrice(isin: String, price: Double)

}
