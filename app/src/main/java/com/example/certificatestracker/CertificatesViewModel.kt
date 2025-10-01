package com.example.certificatestracker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CertificatesViewModel(
    private val dao: CertificatesDao,
    private val apiUsageDao: ApiUsageDao
) : ViewModel() {

    // Lista certificati
    val certificates: StateFlow<List<Certificate>> =
        dao.getAllFlow()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Lista utilizzo API
    val apiUsages: StateFlow<List<ApiUsage>> =
        apiUsageDao.getAllFlow()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // 🔹 Aggiungi nuovo certificato
    fun addCertificate(
        isin: String,
        underlyingName: String,
        strike: Double,
        barrier: Double,
        bonusLevel: Double,
        autocallLevel: Double,
        premio: Double,
        nextbonus: String,
        valautocall: String
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
            Log.d("CERT_VIEWMODEL", "Inserted certificate: $isin")
        }
    }

    // 🔹 Elimina certificato
    fun deleteCertificate(isin: String) {
        viewModelScope.launch {
            dao.delete(isin)
            Log.d("CERT_VIEWMODEL", "Deleted certificate: $isin")
        }
    }

    // 🔹 Aggiorna prezzo certificato con timestamp
    fun updateCertificatePrice(isin: String, price: Double, timestamp: String) {
        viewModelScope.launch {
            dao.updatePriceAndTimestamp(isin, price, timestamp)
            Log.d("CERT_VIEWMODEL", "Updated price for $isin: $price at $timestamp")
        }
    }

    // 🔹 Recupera prezzo più recente dalle API e aggiorna contatori
    fun fetchAndUpdatePrice(isin: String) {
        viewModelScope.launch {
            val certificate = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = certificate.underlyingName
            val now = formatter.format(Date())

            Log.d("API_QUERY", "Fetching price for $symbol ($isin)")

            // 🔹 TwelveData
            val resultTwelve = TwelveDataFetcher.fetchLatestClose(symbol, ApiKeys.TWELVEDATA)
            if (resultTwelve is FetchResult.Success) {
                Log.d("API_RESPONSE", "TwelveData result for $symbol: ${resultTwelve.price}")
                updateCertificatePrice(isin, resultTwelve.price, now)
                incrementApiUsage("Twelve Data")
                return@launch
            }

            // 🔹 Marketstack
            val resultMarket = MarketstackFetcher.fetchLatestClose(symbol, ApiKeys.MARKETSTACK)
            if (resultMarket is FetchResult.Success) {
                Log.d("API_RESPONSE", "Marketstack result for $symbol: ${resultMarket.price}")
                updateCertificatePrice(isin, resultMarket.price, now)
                incrementApiUsage("Marketstack")
                return@launch
            }

            // 🔹 AlphaVantage
            val resultAlpha = AlphaVantageFetcher.fetchLatestClose(symbol, ApiKeys.ALPHAVANTAGE)
            if (resultAlpha is FetchResult.Success) {
                Log.d("API_RESPONSE", "AlphaVantage result for $symbol: ${resultAlpha.price}")
                updateCertificatePrice(isin, resultAlpha.price, now)
                incrementApiUsage("Alpha Vantage")
            }
        }
    }

    // 🔹 Incrementa contatore query API con log
    private fun incrementApiUsage(providerName: String) {
        viewModelScope.launch {
            val usage = apiUsageDao.get(providerName)
            val now = formatter.format(Date())
            if (usage != null) {
                val updated = usage.copy(
                    dailyCount = usage.dailyCount + 1,
                    monthlyCount = usage.monthlyCount + 1,
                    lastUpdated = now
                )
                Log.d("API_USAGE", "Updating usage for $providerName: $updated")
                apiUsageDao.insert(updated)
            } else {
                val newUsage = ApiUsage(providerName, 1, 1, now)
                Log.d("API_USAGE", "Inserting new usage for $providerName: $newUsage")
                apiUsageDao.insert(newUsage)
            }
        }
    }

    // 🔹 Funzione helper per conversione sicura String -> Double
    fun parseDouble(input: String): Double {
        return input.replace(',', '.').toDoubleOrNull() ?: 0.0
    }
}
