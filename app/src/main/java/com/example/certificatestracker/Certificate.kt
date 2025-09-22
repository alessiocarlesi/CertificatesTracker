package com.example.certificatestracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "certificates")
data class Certificate(
    @PrimaryKey val isin: String,
    val underlyingName: String = "",
    val strike: Double = 0.0,
    val barrier: Double = 0.0,
    val bonusLevel: Double = 0.0,
    val autocallLevel: Double = 0.0,
    val lastPrice: Double = 0.0 // prezzo sottostante da Yahoo
)
