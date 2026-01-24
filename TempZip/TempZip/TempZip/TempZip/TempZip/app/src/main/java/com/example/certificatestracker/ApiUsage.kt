package com.example.certificatestracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_usage")
data class ApiUsage(
    @PrimaryKey val provider: String,
    val dailyUsage: Int = 0,
    val monthlyUsage: Int = 0,
    val lastResetDayMillis: Long = 0L,
    val lastResetMonthMillis: Long = 0L
)
