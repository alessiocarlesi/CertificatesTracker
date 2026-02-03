package com.example.certificatestracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificatesDao {

    @Query("SELECT * FROM certificates ORDER BY isin")
    fun getAllFlow(): Flow<List<Certificate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(certificate: Certificate)

    @Update
    suspend fun update(certificate: Certificate)

    @Query("DELETE FROM certificates WHERE isin = :isin")
    suspend fun deleteByIsin(isin: String)

    @Query("SELECT * FROM certificates WHERE isin = :isin LIMIT 1")
    suspend fun getByIsin(isin: String): Certificate?

    @Query("UPDATE certificates SET nextbonus = :nextbonus WHERE isin = :isin")
    suspend fun updateNextBonus(isin: String, nextbonus: String)

    @Query("UPDATE certificates SET valautocall = :valautocall WHERE isin = :isin")
    suspend fun updateValAutocall(isin: String, valautocall: String)

    /**
     * 🔹 AGGIORNAMENTO VALORE CERTIFICATO (Da Scraper Borsa Italiana)
     */
    @Query("UPDATE certificates SET lastPrice = :price, lastUpdate = :timestamp WHERE isin = :isin")
    suspend fun updateCertificatePrice(isin: String, price: Double, timestamp: String)

    /**
     * 🔹 AGGIORNAMENTO VALORE SOTTOSTANTE (Da API: ENI, ENEL, ecc.)
     */
    @Query("UPDATE certificates SET underlyingPrice = :price, lastUpdate = :timestamp WHERE isin = :isin")
    suspend fun updateUnderlyingPrice(isin: String, price: Double, timestamp: String)
}