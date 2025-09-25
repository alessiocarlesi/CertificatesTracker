package com.example.certificatestracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun updateCertificatePrice(isin: String, price: Double, timestamp: String) {
        viewModelScope.launch {
            dao.updatePriceAndTimestamp(isin, price, timestamp)
        }
    }

    fun fetchAndUpdatePrice(isin: String, apiKey: String) {
        viewModelScope.launch {
            // Trova certificato in lista
            val certificate = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = certificate.underlyingName

            // 🔹 Simulazione risposta API (usata ora in debug)
            val simulatedPrice = 10.10
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            updateCertificatePrice(isin, simulatedPrice, now)
            println("SIMULAZIONE CERTIFICATO $isin -> prezzo chiusura: $simulatedPrice EUR @ $now")

            /*
            // 🔹 CODICE REALE (da riattivare dopo 1 ottobre)
            val result = MarketstackFetcher.fetchLatestClose(symbol, apiKey)

            if (result is FetchResult.Success) {
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                updateCertificatePrice(isin, result.close, now)
                println("CERTIFICATO $isin -> prezzo chiusura aggiornato: ${result.close} EUR @ $now")
            } else if (result is FetchResult.Error) {
                println("ERRORE fetch $symbol -> ${result.message}")
            }
            */
        }
    }
}
