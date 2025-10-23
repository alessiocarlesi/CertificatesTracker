// filename: CertificatesViewModel.kt
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
import androidx.compose.runtime.mutableStateListOf


class CertificatesViewModel(
    private val dao: CertificatesDao,
    private val apiUsageDao: ApiUsageDao
) : ViewModel() {

    // 🔹 Aggiungi in cima alla classe, fuori da fetchAndUpdatePrice():
    private val _apiLogs = mutableStateListOf<String>()
    val apiLogs: List<String> get() = _apiLogs

    private fun logApi(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _apiLogs.add("[$timestamp] $message")
        if (_apiLogs.size > 200) _apiLogs.removeFirst() // evita overflow
    }

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // Flussi
    val certificates: StateFlow<List<Certificate>> =
        dao.getAllFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val apiUsages: StateFlow<List<ApiUsage>> =
        apiUsageDao.getAllFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Aggiungi/Inserisci certificato
    fun addCertificate(certificate: Certificate) {
        viewModelScope.launch {
            dao.insert(certificate)
            Log.d("ViewModel", "Inserted certificate: ${certificate.isin}")
        }
    }

    // Aggiorna certificato (oggetto completo)
    fun updateCertificate(certificate: Certificate) {
        viewModelScope.launch {
            dao.update(certificate)
            Log.d("ViewModel", "Updated certificate: ${certificate.isin}")
        }
    }

    // Elimina certificato
    fun deleteCertificate(isin: String) {
        viewModelScope.launch {
            dao.deleteByIsin(isin)
            Log.d("ViewModel", "Deleted certificate: $isin")
        }
    }




    // Aggiorna prezzo e timestamp chiamando i provider in ordine
    fun fetchAndUpdatePrice(isin: String) {
        viewModelScope.launch {
            val cert = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = cert.underlyingName.trim()
            val now = formatter.format(Date())

            logApi("───────────────────────────────")
            logApi("🔹 Richiesta aggiornamento per $symbol ($isin)")

            // Determina provider in base al suffisso
            val provider = when {
                symbol.endsWith(".MI", ignoreCase = true) -> ApiProvider.MARKETSTACK
                symbol.contains(".") -> ApiProvider.ALPHAVANTAGE
                else -> ApiProvider.TWELVEDATA
            }

            logApi("⚙️ Provider selezionato: ${provider.displayName}")

            when (provider) {
                ApiProvider.TWELVEDATA -> {
                    try {
                        logApi("🌐 Twelve Data → richiesta per $symbol")
                        val result = TwelveDataFetcher.fetchLatestClose(symbol, ApiKeys.TWELVEDATA)
                        when (result) {
                            is FetchResult.Success -> {
                                logApi("✅ Twelve Data → ${result.price}")
                                updateCertificatePrice(isin, result.price, now)
                                incrementApiUsage(provider.displayName)
                            }
                            is FetchResult.Error -> logApi("❌ Twelve Data → ${result.message}")
                        }
                    } catch (t: Throwable) {
                        logApi("💥 Twelve Data eccezione: ${t.message}")
                    }
                }

                ApiProvider.MARKETSTACK -> {
                    try {
                        logApi("🌐 Marketstack → richiesta per $symbol")
                        val result = MarketstackFetcher.fetchLatestClose(symbol, ApiKeys.MARKETSTACK)
                        when (result) {
                            is FetchResult.Success -> {
                                logApi("✅ Marketstack → ${result.price}")
                                updateCertificatePrice(isin, result.price, now)
                                incrementApiUsage(provider.displayName)
                            }
                            is FetchResult.Error -> logApi("❌ Marketstack → ${result.message}")
                        }
                    } catch (t: Throwable) {
                        logApi("💥 Marketstack eccezione: ${t.message}")
                    }
                }

                ApiProvider.ALPHAVANTAGE -> {
                    try {
                        logApi("🌐 Alpha Vantage → richiesta per $symbol")
                        val result = AlphaVantageFetcher.fetchLatestClose(symbol, ApiKeys.ALPHAVANTAGE)
                        when (result) {
                            is FetchResult.Success -> {
                                logApi("✅ Alpha Vantage → ${result.price}")
                                updateCertificatePrice(isin, result.price, now)
                                incrementApiUsage(provider.displayName)
                            }
                            is FetchResult.Error -> logApi("❌ Alpha Vantage → ${result.message}")
                        }
                    } catch (t: Throwable) {
                        logApi("💥 Alpha Vantage eccezione: ${t.message}")
                    }
                }
            }

            logApi("✅ Fine aggiornamento per $symbol ($isin)")
        }
    }

    // Aggiorna prezzo direttamente in DB
    private fun updateCertificatePrice(isin: String, price: Double, timestamp: String) {
        viewModelScope.launch {
            val safePrice = price ?: 0.0
            val roundedPrice = (kotlin.math.round(safePrice * 100) / 100.0)
            dao.updatePriceAndTimestamp(isin, roundedPrice, timestamp)
            Log.d("ViewModel", "Updated price for $isin: $price at $timestamp")
        }
    }

    // Incremento contatori API
    private fun incrementApiUsage(providerName: String) {
        viewModelScope.launch {
            val usage = apiUsageDao.get(providerName)
            val now = formatter.format(Date())
            if (usage != null) {
                apiUsageDao.insert(
                    usage.copy(
                        dailyCount = usage.dailyCount + 1,
                        monthlyCount = usage.monthlyCount + 1,
                        lastUpdated = now
                    )
                )
            } else {
                apiUsageDao.insert(ApiUsage(providerName, 1, 1, now))
            }
            Log.d("ViewModel", "Incremented API usage for $providerName")
        }
    }

    fun parseDouble(input: String) = input.replace(',', '.').toDoubleOrNull() ?: 0.0

    // 🔸 Aggiorna nextbonus e valautocall solo se si è entrati nel mese successivo
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

        // ✅ Se la data è passata MA siamo ancora nello stesso mese → non aggiornare
        if (cal.before(today) &&
            cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        ) {
            return null
        }

        // ✅ Se la data è passata ed è iniziato un nuovo mese → aggiorna
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
