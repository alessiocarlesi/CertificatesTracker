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

    fun updateCertificatePrice(isin: String, price: Double) {
        viewModelScope.launch {
            dao.updatePrice(isin, price)
        }
    }

    fun fetchAndUpdatePrice(isin: String) {
        viewModelScope.launch {
            // Trova il certificato
            val certificate = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = certificate.underlyingName

            // 🔹 Prima prova Marketstack
            val result = MarketstackFetcher.fetchLatestClose(symbol, ApiKeys.MARKETSTACK)

            if (result is FetchResult.Success) {
                updateCertificatePrice(isin, result.price)
                println("CERTIFICATO $isin -> prezzo aggiornato Marketstack: ${result.price} EUR")
            } else {
                println("Marketstack limite superato, provo Alpha Vantage...")
                val altResult = AlphaVantageFetcher.fetchLatestClose(symbol, ApiKeys.ALPHAVANTAGE)

                if (altResult is FetchResult.Success) {
                    updateCertificatePrice(isin, altResult.price)
                    println("CERTIFICATO $isin -> prezzo aggiornato Alpha Vantage: ${altResult.price} EUR")
                } else if (altResult is FetchResult.Error) {
                    println("ERRORE fetch $symbol -> ${altResult.message}")
                }
            }
        }
    }
}
