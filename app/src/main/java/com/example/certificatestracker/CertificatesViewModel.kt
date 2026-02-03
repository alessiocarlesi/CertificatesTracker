package com.example.certificatestracker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.mutableStateListOf

class CertificatesViewModel(
    private val dao: CertificatesDao,
    private val apiUsageDao: ApiUsageDao,
    private val insertionDao: CertificateInsertionDao
) : ViewModel() {

    private val _apiLogs = mutableStateListOf<String>()
    val apiLogs: List<String> get() = _apiLogs

    private fun logApi(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _apiLogs.add("[$timestamp] $message")
        if (_apiLogs.size > 200) _apiLogs.removeFirst()
    }

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    val certificates: StateFlow<List<Certificate>> =
        dao.getAllFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val apiUsages: StateFlow<List<ApiUsage>> =
        apiUsageDao.getAllFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _insertionDates = MutableStateFlow<Map<String, String>>(emptyMap())
    val insertionDates: StateFlow<Map<String, String>> = _insertionDates.asStateFlow()

    init {
        refreshInsertionDates()
    }

    fun refreshInsertionDates() {
        viewModelScope.launch {
            val allCerts = certificates.value
            val map = mutableMapOf<String, String>()
            allCerts.forEach { cert ->
                insertionDao.getByIsin(cert.isin)?.let {
                    map[cert.isin] = it.insertionDate
                }
            }
            _insertionDates.value = map
        }
    }

    fun addCertificate(certificate: Certificate) {
        viewModelScope.launch {
            dao.insert(certificate)
            if (insertionDao.getByIsin(certificate.isin) == null) {
                val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                insertionDao.insert(CertificateInsertion(certificate.isin, today))
                refreshInsertionDates()
            }
        }
    }

    fun updateCertificate(certificate: Certificate) {
        viewModelScope.launch {
            dao.update(certificate)
        }
    }

    fun deleteCertificate(isin: String) {
        viewModelScope.launch {
            dao.deleteByIsin(isin)
        }
    }

    /**
     * MODIFICATO: Ora smista correttamente tra Scraper (Mercato) e API (Sottostante)
     */
    fun fetchAndUpdatePrice(isin: String, useBorsaItaliana: Boolean = true) {
        viewModelScope.launch {
            val cert = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = cert.underlyingName.trim()
            val now = formatter.format(Date())

            logApi("───────────────────────────────")

            if (useBorsaItaliana) {
                logApi("🔹 Scraper Borsa IT per $isin")
                val provider = ApiProvider.BORSA_ITALIANA
                logApi("⚙️ Metodo: ${provider.displayName}")

                val result = BorsaItalianaFetcher.fetchLatestClose(isin)
                handleFetchResult(result, isin, now, provider)
            } else {
                logApi("🔹 API Sottostante per $symbol ($isin)")

                val provider = when {
                    symbol.endsWith(".MI", ignoreCase = true) -> ApiProvider.MARKETSTACK
                    symbol.contains(".") -> ApiProvider.ALPHAVANTAGE
                    else -> ApiProvider.TWELVEDATA
                }

                logApi("⚙️ Provider: ${provider.displayName}")

                val result = when (provider) {
                    ApiProvider.TWELVEDATA -> TwelveDataFetcher.fetchLatestClose(symbol, ApiKeys.TWELVEDATA)
                    ApiProvider.MARKETSTACK -> MarketstackFetcher.fetchLatestClose(symbol, ApiKeys.MARKETSTACK)
                    ApiProvider.ALPHAVANTAGE -> AlphaVantageFetcher.fetchLatestClose(symbol, ApiKeys.ALPHAVANTAGE)
                    else -> FetchResult.Error("Provider non configurato")
                }
                handleFetchResult(result, isin, now, provider)
            }
        }
    }

    /**
     * MODIFICATO: Smista il salvataggio nelle due colonne differenti del DB
     */
    private fun handleFetchResult(result: FetchResult, isin: String, now: String, provider: ApiProvider) {
        viewModelScope.launch {
            when (result) {
                is FetchResult.Success -> {
                    logApi("✅ ${provider.displayName} → ${result.price}")

                    val roundedPrice = (kotlin.math.round(result.price * 100) / 100.0)

                    if (provider == ApiProvider.BORSA_ITALIANA) {
                        // Aggiorna il valore di mercato del certificato (lastPrice)
                        dao.updateCertificatePrice(isin, roundedPrice, now)
                    } else {
                        // Aggiorna il valore del sottostante (underlyingPrice)
                        dao.updateUnderlyingPrice(isin, roundedPrice, now)
                        incrementApiUsage(provider.displayName)
                    }
                }
                is FetchResult.Error -> logApi("❌ ${provider.displayName} → ${result.message}")
            }
        }
    }

    private fun incrementApiUsage(providerName: String) {
        viewModelScope.launch {
            val usage = apiUsageDao.get(providerName)
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTimestamp = formatter.format(Date())

            if (usage != null) {
                val isNewDay = !usage.lastUpdated.startsWith(todayDate)
                val newDailyCount = if (isNewDay) 1 else usage.dailyCount + 1

                apiUsageDao.insert(
                    usage.copy(
                        dailyCount = newDailyCount,
                        monthlyCount = usage.monthlyCount + 1,
                        lastUpdated = currentTimestamp
                    )
                )
            } else {
                apiUsageDao.insert(ApiUsage(providerName, 1, 1, currentTimestamp))
            }
        }
    }

    // --- Gestione Date Bonus/Autocall ---
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
        if (monthsToAdd == 0 || dateStr.isBlank()) return null
        val parts = dateStr.split("/")
        if (parts.size != 3) return null

        val cal = Calendar.getInstance()
        try {
            cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
            val today = Calendar.getInstance()

            if (cal.before(today)) {
                cal.add(Calendar.MONTH, monthsToAdd)
                val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val year = cal.get(Calendar.YEAR)
                return "$day/$month/$year"
            }
        } catch (e: Exception) { return null }
        return null
    }

    fun updateAllCertificates() {
        viewModelScope.launch { // <--- Questo apre il contesto per le coroutine
            logApi("🚀 Avvio aggiornamento globale portafoglio...")

            // Prendiamo la lista attuale
            val listaCertificati = certificates.value

            listaCertificati.forEachIndexed { index, cert ->
                logApi("🔄 Aggiornamento ${index + 1}/${listaCertificati.size}: ${cert.isin}")

                // fetchAndUpdatePrice è già una funzione che lancia una coroutine
                fetchAndUpdatePrice(cert.isin, useBorsaItaliana = true)

                // Il delay deve stare QUI, dentro il lancio della coroutine principale
                kotlinx.coroutines.delay(2500)
            }
            logApi("✅ Aggiornamento globale terminato.")
        }
    }


}