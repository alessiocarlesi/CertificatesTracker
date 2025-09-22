package com.example.certificatestracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CertificatesViewModelFactory(private val dao: CertificatesDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CertificatesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CertificatesViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
