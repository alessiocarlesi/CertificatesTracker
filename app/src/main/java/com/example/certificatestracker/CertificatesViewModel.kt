package com.example.certificatestracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CertificatesViewModel(private val dao: CertificatesDao) : ViewModel() {

    val certificates: StateFlow<List<Certificate>> =
        dao.getAllFlow()
            .map { it }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addCertificate(
        isin: String,
        underlyingName: String,
        strike: Double,
        barrier: Double,
        bonusLevel: Double,
        autocallLevel: Double
    ) {
        viewModelScope.launch {
            dao.insert(
                Certificate(
                    isin = isin,
                    underlyingName = underlyingName,
                    strike = strike,
                    barrier = barrier,
                    bonusLevel = bonusLevel,
                    autocallLevel = autocallLevel
                )
            )
        }
    }

    fun deleteCertificate(isin: String) {
        viewModelScope.launch {
            dao.delete(isin)
        }
    }

    fun fetchLatestCloseForCertificate(symbol: String, apiKey: String) {
        viewModelScope.launch {
            // Chiamata sospesa
            val result = MarketstackFetcher.fetchLatestClose(symbol, apiKey)
            when (result) {
                is FetchResult.Success -> {
                    println("CERTIFICATO $symbol -> prezzo chiusura: ${result.close} EUR")
                    // Qui potrai fare update in DB in futuro
                }
                is FetchResult.Error -> {
                    println("ERRORE fetch $symbol -> ${result.message}")
                }
            }
        }
    }

    fun updateCertificatePrice(isin: String, price: Double) {
        viewModelScope.launch {
            dao.updatePrice(isin, price)
        }
    }

    fun fetchAndUpdatePrice(isin: String, apiKey: String) {
        viewModelScope.launch {
            // Trova il sottostante corrispondente
            val certificate = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = certificate.underlyingName

            // Fetch prezzo
            val result = MarketstackFetcher.fetchLatestClose(symbol, apiKey)

            if (result is FetchResult.Success) {
                updateCertificatePrice(isin, result.close)
                println("CERTIFICATO $isin -> prezzo chiusura aggiornato: ${result.close} EUR")
            } else if (result is FetchResult.Error) {
                println("ERRORE fetch $symbol -> ${result.message}")
            }
        }
    }

}

