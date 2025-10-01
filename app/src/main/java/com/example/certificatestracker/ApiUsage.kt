// filename: ApiUsage.kt
package com.example.certificatestracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_usage")
data class ApiUsage(
    @PrimaryKey val providerName: String,
    val dailyCount: Int,
    val monthlyCount: Int,
    val lastUpdated: String
)
