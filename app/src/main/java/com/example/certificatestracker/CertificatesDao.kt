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

    // 🔹 Aggiorna prezzo e timestamp
    @Query("UPDATE certificates SET lastPrice = :price, lastUpdate = :timestamp WHERE isin = :isin")
    suspend fun updatePriceAndTimestamp(isin: String, price: Double, timestamp: String)
}
