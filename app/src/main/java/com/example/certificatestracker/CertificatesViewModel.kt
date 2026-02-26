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

private val globalApiLogs = mutableStateListOf<String>()

class CertificatesViewModel(
    private val dao: CertificatesDao,
    private val apiUsageDao: ApiUsageDao,
    private val insertionDao: CertificateInsertionDao,
    private val underlyingPriceDao: UnderlyingPriceDao
) : ViewModel() {

    val apiLogs: List<String> get() = globalApiLogs

    private val _lastOperationFailed = MutableStateFlow(false)
    val lastOperationFailed = _lastOperationFailed.asStateFlow()

    private val _syncSuccess = MutableStateFlow(false)
    val syncSuccess = _syncSuccess.asStateFlow()

    // 🔹 NUOVO SENSORE: Indica se un aggiornamento è in corso
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    private val lastPricesMap = mutableStateMapOf<String, Double>()
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private fun logApi(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        globalApiLogs.add("[$timestamp] $message")
        Log.d("CERT_TRACKER_LOG", "Nuovo: $message")
        if (globalApiLogs.size > 1000) globalApiLogs.removeFirst()
    }

    val certificates: StateFlow<List<Certificate>> =
        dao.getAllFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val apiUsages: StateFlow<List<ApiUsage>> =
        apiUsageDao.getAllFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _insertionDates = MutableStateFlow<Map<String, String>>(emptyMap())
    val insertionDates: StateFlow<Map<String, String>> = _insertionDates.asStateFlow()

    init {
        refreshInsertionDates()
        caricaPrezziDalDatabase()
    }

    private fun caricaPrezziDalDatabase() {
        viewModelScope.launch {
            val prezziSalvati = underlyingPriceDao.getAll()
            prezziSalvati.forEach {
                lastPricesMap[it.ticker] = it.price
            }
            _lastSyncTime.value = prezziSalvati.maxByOrNull { it.lastUpdate }?.lastUpdate
        }
    }

    fun getLastKnownPrice(ticker: String): Double = lastPricesMap[ticker] ?: 0.0

    fun refreshInsertionDates() {
        viewModelScope.launch {
            val allCerts = certificates.value
            val map = mutableMapOf<String, String>()
            allCerts.forEach { cert ->
                insertionDao.getByIsin(cert.isin)?.let { map[cert.isin] = it.insertionDate }
            }
            _insertionDates.value = map
        }
    }

    fun fetchAndUpdatePrice(isin: String, useBorsaItaliana: Boolean = true) {
        viewModelScope.launch {
            _lastOperationFailed.value = false
            val cert = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = (cert.und1 ?: cert.underlyingName).trim()
            val now = formatter.format(Date())

            if (useBorsaItaliana) {
                val result = BorsaItalianaFetcher.fetchLatestClose(isin)
                handleFetchResult(result, isin, now, ApiProvider.BORSA_ITALIANA)
            } else {
                val yahooPrice = YahooFinanceFetcher.getPrice(symbol)
                if (yahooPrice != null) {
                    handleFetchResult(FetchResult.Success(yahooPrice), isin, now, ApiProvider.YAHOO)
                } else {
                    _lastOperationFailed.value = true
                    logApi("⚠️ Yahoo non trovato per $symbol")
                }
            }
        }
    }

    private fun handleFetchResult(result: FetchResult, isin: String, now: String, provider: ApiProvider) {
        viewModelScope.launch {
            when (result) {
                is FetchResult.Success -> {
                    _lastOperationFailed.value = false
                    _lastSyncTime.value = now
                    val roundedPrice = (kotlin.math.round(result.price * 100) / 100.0)

                    if (provider == ApiProvider.BORSA_ITALIANA) {
                        dao.updateCertificatePrice(isin, roundedPrice, now)
                    } else {
                        dao.updateUnderlyingPrice(isin, roundedPrice, now)
                        val cert = certificates.value.find { it.isin == isin }
                        val ticker = (cert?.und1 ?: cert?.underlyingName)?.trim()
                        ticker?.let {
                            lastPricesMap[it] = roundedPrice
                            underlyingPriceDao.insertOrUpdate(UnderlyingPrice(it, roundedPrice, now))
                        }
                        incrementApiUsage(provider.displayName)
                    }
                    logApi("✅ ${provider.displayName} → $roundedPrice")
                }
                is FetchResult.Error -> {
                    _lastOperationFailed.value = true
                    logApi("❌ ${provider.displayName} → ${result.message}")
                }
            }
        }
    }

    fun updateAllUnderlyings() {
        viewModelScope.launch {
            // 🔄 RESET STATI PER NUOVO CICLO
            _isSyncing.value = true
            _syncSuccess.value = false
            _lastOperationFailed.value = false

            logApi("🚀 START: Sincronizzazione Mercati (v14)")

            val listaCertificati = certificates.value
            val tickerUnici = listaCertificati.flatMap { cert ->
                listOfNotNull(cert.und1, cert.und2, cert.und3, cert.und4, cert.und5, cert.und6)
            }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

            if (tickerUnici.isEmpty()) {
                logApi("⚠️ Nessun sottostante trovato.")
                _isSyncing.value = false
                return@launch
            }

            var hasError = false
            tickerUnici.forEachIndexed { index, symbol ->
                logApi("🔍 Sottostante [${index + 1}/${tickerUnici.size}]: $symbol")
                val prezzoYahoo = YahooFinanceFetcher.getPrice(symbol)
                if (prezzoYahoo != null) {
                    val now = formatter.format(Date())
                    val roundedPrice = (kotlin.math.round(prezzoYahoo * 100) / 100.0)
                    lastPricesMap[symbol] = roundedPrice
                    underlyingPriceDao.insertOrUpdate(UnderlyingPrice(symbol, roundedPrice, now))
                    _lastSyncTime.value = now
                    listaCertificati.filter { (it.und1 ?: it.underlyingName).trim() == symbol }.forEach { cert ->
                        dao.updateUnderlyingPrice(cert.isin, roundedPrice, now)
                    }
                    logApi("✅ Yahoo Finance → $symbol: $roundedPrice")
                } else {
                    hasError = true
                    logApi("❌ Yahoo Finance → $symbol non trovato")
                }
                kotlinx.coroutines.delay(1000)
            }

            // 🏁 FINE CICLO: Imposta risultati finali
            _isSyncing.value = false
            _lastOperationFailed.value = hasError
            _syncSuccess.value = !hasError
            logApi(if (hasError) "⚠️ Fine: Sincronizzazione conclusa con alcuni errori" else "🏁 Fine: Mercati aggiornati con successo")
        }
    }

    fun updateAllCertificates() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncSuccess.value = false
            _lastOperationFailed.value = false

            logApi("🚀 Avvio aggiornamento globale portafoglio...")

            val lista = certificates.value
            if (lista.isEmpty()) {
                logApi("⚠️ Nessun certificato in portafoglio.")
                _isSyncing.value = false
                return@launch
            }

            var erroriRilevati = false
            lista.forEachIndexed { index, cert ->
                try {
                    logApi("🔍 Aggiornamento [${index + 1}/${lista.size}]: ${cert.isin}")
                    fetchAndUpdatePrice(cert.isin, useBorsaItaliana = true)
                    kotlinx.coroutines.delay(2500)
                } catch (e: Exception) {
                    erroriRilevati = true
                    logApi("❌ Errore durante l'aggiornamento di ${cert.isin}")
                }
            }

            _isSyncing.value = false
            _lastOperationFailed.value = erroriRilevati
            _syncSuccess.value = !erroriRilevati
            logApi("🏁 Fine: Aggiornamento globale completato")
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

    fun deleteCertificate(isin: String) = viewModelScope.launch { dao.deleteByIsin(isin) }
    fun updateCertificate(certificate: Certificate) = viewModelScope.launch { dao.update(certificate) }
    fun avviaEsportazione(context: Context) = viewModelScope.launch { logApi(DatabaseManager.exportDatabase(context)) }

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
}