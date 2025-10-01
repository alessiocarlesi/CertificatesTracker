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

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun addCertificate(
        isin: String,
        underlyingName: String,
        strike: Double,
        barrier: Double,
        bonusLevel: Double,
        autocallLevel: Double,
        premio: Double,          // nuovo
        nextbonus: String,        // nuovo
        valautocall: String       // nuovo
    ) {
        viewModelScope.launch {
            dao.insert(
                Certificate(
                    isin = isin,
                    underlyingName = underlyingName,
                    strike = strike,
                    barrier = barrier,
                    bonusLevel = bonusLevel,
                    autocallLevel = autocallLevel,
                    premio = premio,
                    nextbonus = nextbonus,
                    valautocall = valautocall
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

    fun fetchAndUpdatePrice(isin: String) {
        viewModelScope.launch {
            val certificate = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = certificate.underlyingName
            val now = formatter.format(Date())

            // 🔹 Prima prova TwelveData
            val resultTwelve = TwelveDataFetcher.fetchLatestClose(symbol, ApiKeys.TWELVEDATA)
            if (resultTwelve is FetchResult.Success) {
                updateCertificatePrice(isin, resultTwelve.price, now)
                println("CERTIFICATO $isin -> prezzo aggiornato TwelveData: ${resultTwelve.price} EUR, ora: $now")
                return@launch
            }

            println("TwelveData limite superato o errore, provo Marketstack...")
            val resultMarket = MarketstackFetcher.fetchLatestClose(symbol, ApiKeys.MARKETSTACK)
            if (resultMarket is FetchResult.Success) {
                updateCertificatePrice(isin, resultMarket.price, now)
                println("CERTIFICATO $isin -> prezzo aggiornato Marketstack: ${resultMarket.price} EUR, ora: $now")
                return@launch
            }

            println("Marketstack limite superato o errore, provo AlphaVantage...")
            val resultAlpha = AlphaVantageFetcher.fetchLatestClose(symbol, ApiKeys.ALPHAVANTAGE)
            if (resultAlpha is FetchResult.Success) {
                updateCertificatePrice(isin, resultAlpha.price, now)
                println("CERTIFICATO $isin -> prezzo aggiornato AlphaVantage: ${resultAlpha.price} EUR, ora: $now")
            } else if (resultAlpha is FetchResult.Error) {
                println("ERRORE fetch $symbol -> ${resultAlpha.message}")
            }
        }
    }
}

