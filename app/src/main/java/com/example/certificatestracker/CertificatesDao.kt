package com.example.certificatestracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificatesDao {

    @Query("SELECT * FROM certificates")
    fun getAllFlow(): Flow<List<Certificate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(certificate: Certificate)

    @Query("DELETE FROM certificates WHERE isin = :isin")
    suspend fun delete(isin: String)

    @Query("UPDATE certificates SET lastPrice = :price WHERE isin = :isin")
    suspend fun updatePrice(isin: String, price: Double)

    @Query("UPDATE certificates SET lastPrice = :price, lastUpdate = :timestamp WHERE isin = :isin")
    suspend fun updatePriceAndTimestamp(isin: String, price: Double, timestamp: String)

    // 🔹 Aggiorna nextbonus persistente
    @Query("UPDATE certificates SET nextbonus = :newDate WHERE isin = :isin")
    suspend fun updateNextBonus(isin: String, newDate: String)

    // 🔹 Aggiorna valautocall persistente
    @Query("UPDATE certificates SET valautocall = :newDate WHERE isin = :isin")
    suspend fun updateValAutocall(isin: String, newDate: String)

    @Dao
    interface CertificatesDao {
        @Query("SELECT COUNT(*) FROM certificates")
        suspend fun countCertificates(): Int
    }
}
