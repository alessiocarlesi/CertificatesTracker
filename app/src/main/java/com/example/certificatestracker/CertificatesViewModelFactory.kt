// filename: app/src/main/java/com/example/certificatestracker/CertificatesViewModelFactory.kt
package com.example.certificatestracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CertificatesViewModelFactory(
    private val dao: CertificatesDao,
    private val apiUsageDao: ApiUsageDao,
    private val insertionDao: CertificateInsertionDao // 🔹 Aggiunto per gestire le date di acquisto
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CertificatesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CertificatesViewModel(dao, apiUsageDao, insertionDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}