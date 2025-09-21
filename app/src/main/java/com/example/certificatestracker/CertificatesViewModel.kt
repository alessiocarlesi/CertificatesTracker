package com.example.certificatestracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CertificatesViewModel(private val dao: CertificatesDao) : ViewModel() {

    val certificates: StateFlow<List<CertificateEntity>> =
        dao.getAllFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCertificate(
        isin: String,
        underlyingName: String,
        strike: Double,
        barrier: Double,
        bonusLevel: Double,
        autocallLevel: Double
    ) {
        viewModelScope.launch {
            val cert = CertificateEntity(
                isin = isin,
                underlyingName = underlyingName,
                strike = strike,
                barrier = barrier,
                bonusLevel = bonusLevel,
                autocallLevel = autocallLevel
            )
            dao.insert(cert)
        }
    }

    fun deleteCertificate(cert: CertificateEntity) {
        viewModelScope.launch {
            dao.delete(cert)
        }
    }
}
