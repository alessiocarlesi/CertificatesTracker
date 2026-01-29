package com.example.certificatestracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "certificate_insertions")
data class CertificateInsertion(
    @PrimaryKey val isin: String,
    val insertionDate: String // formato "dd/MM/yyyy"
)
