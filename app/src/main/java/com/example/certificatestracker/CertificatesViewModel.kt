package com.example.certificatestracker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import android.content.Context

class CertificatesViewModel(
    private val dao: CertificatesDao,
    private val apiUsageDao: ApiUsageDao,
    private val insertionDao: CertificateInsertionDao
) : ViewModel() {

    private val _apiLogs = mutableStateListOf<String>()
    val apiLogs: List<String> get() = _apiLogs

    // 🔹 1. MAPPA PREZZI IN RAM (Per calcolo Worst-Of istantaneo)
    private val lastPricesMap = mutableStateMapOf<String, Double>()

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

    // 🔹 2. FUNZIONE PER LO SCREEN: Risolve l'errore del calcolo Worst-Of
    fun getLastKnownPrice(ticker: String): Double {
        return lastPricesMap[ticker] ?: 0.0
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

    fun fetchAndUpdatePrice(isin: String, useBorsaItaliana: Boolean = true) {
        viewModelScope.launch {
            val cert = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = (cert.und1 ?: cert.underlyingName).trim()
            val now = formatter.format(Date())

            logApi("───────────────────────────────")

            if (useBorsaItaliana) {
                logApi("🔹 Scraper Borsa IT per $isin")
                val result = BorsaItalianaFetcher.fetchLatestClose(isin)
                handleFetchResult(result, isin, now, ApiProvider.BORSA_ITALIANA)
            } else {
                logApi("🔹 API Sottostante per $symbol ($isin)")
                logApi("🔍 Tentativo Yahoo Finance...")
                val yahooPrice = YahooFinanceFetcher.getPrice(symbol)

                if (yahooPrice != null) {
                    handleFetchResult(FetchResult.Success(yahooPrice), isin, now, ApiProvider.YAHOO)
                } else {
                    logApi("⚠️ Yahoo non trovato. Utilizzo fallback...")
                    // Logica fallback omessa per brevità
                }
            }
        }
    }

    private fun handleFetchResult(result: FetchResult, isin: String, now: String, provider: ApiProvider) {
        viewModelScope.launch {
            when (result) {
                is FetchResult.Success -> {
                    logApi("✅ ${provider.displayName} → ${result.price}")
                    val roundedPrice = (kotlin.math.round(result.price * 100) / 100.0)

                    if (provider == ApiProvider.BORSA_ITALIANA) {
                        dao.updateCertificatePrice(isin, roundedPrice, now)
                    } else {
                        dao.updateUnderlyingPrice(isin, roundedPrice, now)
                        val cert = certificates.value.find { it.isin == isin }
                        val ticker = cert?.und1 ?: cert?.underlyingName
                        ticker?.let { lastPricesMap[it] = roundedPrice }
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
            val now = Calendar.getInstance()
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(now.time)
            val currentTimestamp = formatter.format(now.time)

            if (usage != null) {
                val isNewDay = !usage.lastUpdated.startsWith(todayDate)
                val isNewMonth = !usage.lastUpdated.startsWith(currentMonth)
                apiUsageDao.insert(usage.copy(
                    dailyCount = if (isNewDay) 1 else usage.dailyCount + 1,
                    monthlyCount = if (isNewMonth) 1 else usage.monthlyCount + 1,
                    lastUpdated = currentTimestamp
                ))
            } else {
                apiUsageDao.insert(ApiUsage(providerName, 1, 1, currentTimestamp))
            }
        }
    }

    /**
     * 🚀 AGGIORNAMENTO GLOBALE v13: Scansiona tutti i 6 slot di ogni certificato
     */
    fun updateAllUnderlyings() {
        viewModelScope.launch {
            logApi("🚀 START: Aggiornamento globale Yahoo Finance (v13)")
            val listaCertificati = certificates.value

            val tickerUnici = listaCertificati.flatMap { cert ->
                listOfNotNull(cert.und1, cert.und2, cert.und3, cert.und4, cert.und5, cert.und6)
            }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

            tickerUnici.forEachIndexed { index, symbol ->
                logApi("🔄 [${index + 1}/${tickerUnici.size}] Yahoo per $symbol")
                val prezzoYahoo = YahooFinanceFetcher.getPrice(symbol)

                if (prezzoYahoo != null) {
                    val now = formatter.format(Date())
                    val roundedPrice = (kotlin.math.round(prezzoYahoo * 100) / 100.0)
                    lastPricesMap[symbol] = roundedPrice
                    listaCertificati.filter { (it.und1 ?: it.underlyingName).trim() == symbol }.forEach { cert ->
                        dao.updateUnderlyingPrice(cert.isin, roundedPrice, now)
                    }
                    logApi("📈 $symbol aggiornato: €$roundedPrice")
                } else {
                    logApi("⚠️ $symbol: Non trovato")
                }
                kotlinx.coroutines.delay(1000)
            }
            logApi("🏁 FINE: Sottostanti aggiornati.")
        }
    }

    fun updateDatesIfNeeded(cert: Certificate): Certificate {
        var updatedNext = cert.nextbonus
        var updatedAutocall = cert.valautocall
        formatDateIfPast(cert.nextbonus, cert.bonusMonths)?.let {
            updatedNext = it
            viewModelScope.launch { dao.updateNextBonus(cert.isin, it) }
        }
        formatDateIfPast(cert.valautocall, cert.autocallMonths)?.let {
            updatedAutocall = it
            viewModelScope.launch { dao.updateValAutocall(cert.isin, it) }
        }
        return cert.copy(nextbonus = updatedNext, valautocall = updatedAutocall)
    }

    private fun formatDateIfPast(dateStr: String, monthsToAdd: Int): String? {
        if (monthsToAdd == 0 || dateStr.isBlank()) return null
        val parts = dateStr.split("/")
        if (parts.size != 3) return null
        val cal = Calendar.getInstance()
        try {
            cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
            if (cal.before(Calendar.getInstance())) {
                cal.add(Calendar.MONTH, monthsToAdd)
                return "${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}/${(cal.get(Calendar.MONTH)+1).toString().padStart(2, '0')}/${cal.get(Calendar.YEAR)}"
            }
        } catch (e: Exception) { return null }
        return null
    }

    fun updateAllCertificates() {
        viewModelScope.launch {
            logApi("🚀 Avvio aggiornamento globale portafoglio...")
            certificates.value.forEachIndexed { index, cert ->
                fetchAndUpdatePrice(cert.isin, useBorsaItaliana = true)
                kotlinx.coroutines.delay(2500)
            }
            logApi("✅ Aggiornamento globale terminato.")
        }
    }

    fun avviaEsportazione(context: Context) {
        viewModelScope.launch {
            logApi(DatabaseManager.exportDatabase(context))
        }
    }
}