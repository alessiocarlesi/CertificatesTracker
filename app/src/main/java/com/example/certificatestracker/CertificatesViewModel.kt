package com.example.certificatestracker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CertificatesViewModel(
    private val dao: CertificatesDao,
    private val apiUsageDao: ApiUsageDao
) : ViewModel() {

    val certificates: StateFlow<List<Certificate>> =
        dao.getAllFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val apiUsages: StateFlow<List<ApiUsage>> =
        apiUsageDao.getAllFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // 🔹 Aggiungi nuovo certificato
    fun addCertificate(
        isin: String,
        underlyingName: String,
        strike: Double,
        bonusLevel: Double,
        bonusMonths: Int,
        autocallLevel: Double,
        autocallMonths: Int,
        barrier: Double,
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
                    bonusMonths = bonusMonths,
                    autocallLevel = autocallLevel,
                    autocallMonths = autocallMonths,
                    premio = premio,
                    nextbonus = nextbonus,
                    valautocall = valautocall
                )
            )
            Log.d("ViewModel", "Inserted certificate: $isin")
        }
    }

    fun deleteCertificate(isin: String) {
        viewModelScope.launch { dao.delete(isin); Log.d("ViewModel", "Deleted certificate: $isin") }
    }

    fun updateCertificatePrice(isin: String, price: Double, timestamp: String) {
        viewModelScope.launch {
            dao.updatePriceAndTimestamp(isin, price, timestamp)
            Log.d("ViewModel", "Updated price for $isin: $price at $timestamp")
        }
    }

    fun fetchAndUpdatePrice(isin: String) {
        viewModelScope.launch {
            val cert = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = cert.underlyingName
            val now = formatter.format(Date())

            // TwelveData
            Log.d("ViewModel", "Fetching price from TwelveData for $symbol")
            val resultTwelve = TwelveDataFetcher.fetchLatestClose(symbol, ApiKeys.TWELVEDATA)
            if (resultTwelve is FetchResult.Success) {
                updateCertificatePrice(isin, resultTwelve.price, now)
                incrementApiUsage("Twelve Data")
                return@launch
            }

            // Marketstack
            Log.d("ViewModel", "Fetching price from Marketstack for $symbol")
            val resultMarket = MarketstackFetcher.fetchLatestClose(symbol, ApiKeys.MARKETSTACK)
            if (resultMarket is FetchResult.Success) {
                updateCertificatePrice(isin, resultMarket.price, now)
                incrementApiUsage("Marketstack")
                return@launch
            }

            // AlphaVantage
            Log.d("ViewModel", "Fetching price from AlphaVantage for $symbol")
            val resultAlpha = AlphaVantageFetcher.fetchLatestClose(symbol, ApiKeys.ALPHAVANTAGE)
            if (resultAlpha is FetchResult.Success) {
                updateCertificatePrice(isin, resultAlpha.price, now)
                incrementApiUsage("Alpha Vantage")
            }
        }
    }

    private fun incrementApiUsage(providerName: String) {
        viewModelScope.launch {
            val usage = apiUsageDao.get(providerName)
            val now = formatter.format(Date())
            if (usage != null) {
                apiUsageDao.insert(usage.copy(
                    dailyCount = usage.dailyCount + 1,
                    monthlyCount = usage.monthlyCount + 1,
                    lastUpdated = now
                ))
            } else {
                apiUsageDao.insert(ApiUsage(providerName, 1, 1, now))
            }
            Log.d("ViewModel", "Incremented API usage for $providerName")
        }
    }

    fun parseDouble(input: String) = input.replace(',', '.').toDoubleOrNull() ?: 0.0

    // 🔹 Incremento mesi su nextbonus e valautocall
    fun updateDatesIfNeeded(cert: Certificate): Certificate {
        var updatedNextbonus = cert.nextbonus
        var updatedValautocall = cert.valautocall

        formatDateIfPast(cert.nextbonus, cert.bonusMonths)?.let {
            updatedNextbonus = it
            viewModelScope.launch { dao.updateNextBonus(cert.isin, it) }
        }

        formatDateIfPast(cert.valautocall, cert.autocallMonths)?.let {
            updatedValautocall = it
            viewModelScope.launch { dao.updateValAutocall(cert.isin, it) }
        }

        return cert.copy(nextbonus = updatedNextbonus, valautocall = updatedValautocall)
    }

    private fun formatDateIfPast(dateStr: String, monthsToAdd: Int): String? {
        if (monthsToAdd == 0) return null
        val parts = dateStr.split("/")
        if (parts.size != 3) return null
        val cal = Calendar.getInstance()
        cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
        val today = Calendar.getInstance()
        if (cal.before(today)) {
            cal.add(Calendar.MONTH, monthsToAdd)
            val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
            val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            val year = cal.get(Calendar.YEAR)
            return "$day/$month/$year"
        }
        return null
    }
}
