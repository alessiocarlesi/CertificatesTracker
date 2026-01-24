package com.example.certificatestracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CertificatesViewModelFactory(
    private val certificatesDao: CertificatesDao,
    private val apiUsageRepository: ApiUsageRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CertificatesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CertificatesViewModel(certificatesDao, apiUsageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
