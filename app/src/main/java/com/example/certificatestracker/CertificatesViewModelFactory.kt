package com.example.certificatestracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CertificatesViewModelFactory(
    private val dao: CertificatesDao,
    private val apiUsageDao: ApiUsageDao,
    private val insertionDao: CertificateInsertionDao,
    private val underlyingPriceDao: UnderlyingPriceDao // 🔹 1. Aggiunto il DAO per i prezzi v14
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CertificatesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // 🔹 2. Passiamo il quarto parametro al costruttore del ViewModel
            return CertificatesViewModel(
                dao,
                apiUsageDao,
                insertionDao,
                underlyingPriceDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}