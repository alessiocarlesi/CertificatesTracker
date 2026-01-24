package com.example.certificatestracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.certificatestracker.api.ApiProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CertificatesViewModel(
    private val dao: CertificatesDao,
    private val usageRepository: ApiUsageRepository
) : ViewModel() {

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

    fun fetchAndUpdatePriceWithLimit(isin: String, provider: ApiProvider) {
        viewModelScope.launch {
            val usage = usageRepository.getUsage(provider) ?: return@launch
            if (usage.dailyUsage >= provider.dailyLimit) {
                println("Limite giornaliero superato per ${provider.displayName}, salto fetch $isin")
                return@launch
            }
            if (usage.monthlyUsage >= provider.monthlyLimit) {
                println("Limite mensile superato per ${provider.displayName}, salto fetch $isin")
                return@launch
            }

            val certificate = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = certificate.underlyingName
            val now = formatter.format(Date())

            val result = when (provider) {
                ApiProvider.TWELVEDATA -> TwelveDataFetcher.fetchLatestClose(symbol, ApiKeys.TWELVEDATA)
                ApiProvider.MARKETSTACK -> MarketstackFetcher.fetchLatestClose(symbol, ApiKeys.MARKETSTACK)
                ApiProvider.ALPHAVANTAGE -> AlphaVantageFetcher.fetchLatestClose(symbol, ApiKeys.ALPHAVANTAGE)
            }

            if (result is FetchResult.Success) {
                updateCertificatePrice(isin, result.price, now)
                usageRepository.recordCall(provider)
                println("CERTIFICATO $isin -> prezzo aggiornato ${provider.displayName}: ${result.price} EUR, ora: $now")
            } else if (result is FetchResult.Error) {
                println("Errore fetch $symbol con ${provider.displayName}: ${result.message}")
            }
        }
    }
}
