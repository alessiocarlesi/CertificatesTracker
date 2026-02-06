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
    val underlyingPrice: Double = 0.0,
    val lastUpdate: String? = null,
    val quantity: Int = 0,
    val purchasePrice: Double? = null,

    // 🔹 CAMPI VERSION 13: I 6 SOTTOSTANTI
    val und1: String? = null, val und1Strike: Double = 0.0,
    val und2: String? = null, val und2Strike: Double = 0.0,
    val und3: String? = null, val und3Strike: Double = 0.0,
    val und4: String? = null, val und4Strike: Double = 0.0,
    val und5: String? = null, val und5Strike: Double = 0.0,
    val und6: String? = null, val und6Strike: Double = 0.0,

    // 🔹 CAMPI VERSION 13: PERCENTUALI UNICHE
    val barrierPerc: Double = 0.0,
    val bonusPerc: Double = 0.0,
    val autocallPerc: Double = 0.0
)

// 🔹 NUOVA TABELLA VERSION 14: MEMORIA PREZZI SOTTOSTANTI
@Entity(tableName = "underlying_prices")
data class UnderlyingPrice(
    @PrimaryKey val ticker: String,
    val price: Double,
    val lastUpdate: String
)