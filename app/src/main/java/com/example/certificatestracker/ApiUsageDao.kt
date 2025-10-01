package com.example.certificatestracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiUsageDao {

    @Query("SELECT * FROM api_usage")
    fun getAllFlow(): Flow<List<ApiUsage>>

    @Query("SELECT * FROM api_usage WHERE providerName = :providerName LIMIT 1")
    suspend fun get(providerName: String): ApiUsage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apiUsage: ApiUsage)
}
