package com.example.certificatestracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "certificates")
data class Certificate(
    @PrimaryKey val isin: String,
    val underlyingName: String,
    val strike: Double = 0.0,
    val barrier: Double = 0.0,
    val bonusLevel: Double = 0.0,
    val bonusMonths: Int = 0,
    val autocallLevel: Double = 0.0,
    val autocallMonths: Int = 0,
    val premio: Double = 0.0,
    val nextbonus: String = "",
    val valautocall: String = "",
    val lastPrice: Double = 0.0,
    val lastUpdate: String? = null
)
