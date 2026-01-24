package com.example.certificatestracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ApiUsageDao {
    @Query("SELECT * FROM api_usage WHERE provider = :provider LIMIT 1")
    suspend fun getUsage(provider: String): ApiUsage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usage: ApiUsage)
}
