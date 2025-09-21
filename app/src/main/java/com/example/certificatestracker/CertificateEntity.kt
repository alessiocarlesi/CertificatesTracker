package com.example.certificatestracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey val isin: String,
    val underlyingName: String = "", // nuovo campo
    val strike: Double = 0.0,
    val barrier: Double = 0.0,
    val bonusLevel: Double = 0.0,
    val autocallLevel: Double = 0.0 // nuovo campo
)

