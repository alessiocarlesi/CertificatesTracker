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
    private val insertionDao: CertificateInsertionDao // Aggiunto Dao Inserzioni
) : ViewModel() {

    private val _apiLogs = mutableStateListOf<String>()
    val apiLogs: List<String> get() = _apiLogs

    private fun logApi(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _apiLogs.add("[$timestamp] $message")
        if (_apiLogs.size > 200) _apiLogs.removeFirst()
    }

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // Flusso Certificati
    val certificates: StateFlow<List<Certificate>> =
        dao.getAllFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Flusso Utilizzo API
    val apiUsages: StateFlow<List<ApiUsage>> =
        apiUsageDao.getAllFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Mappa delle date di inserimento (ISIN -> Data)
    // Usiamo un StateFlow per monitorare i cambiamenti nel database delle inserzioni
    private val _insertionDates = MutableStateFlow<Map<String, String>>(emptyMap())
    val insertionDates: StateFlow<Map<String, String>> = _insertionDates.asStateFlow()

    init {
        // Carica le date di inserimento all'avvio
        refreshInsertionDates()
    }

    fun refreshInsertionDates() {
        viewModelScope.launch {
            // In un progetto reale potresti voler osservare il DB con un Flow,
            // qui carichiamo i dati necessari per il calcolo.
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

    // Aggiungi certificato con gestione della data di inserimento
    fun addCertificate(certificate: Certificate) {
        viewModelScope.launch {
            dao.insert(certificate)
            // Se non esiste già una data di inserimento, usiamo oggi
            if (insertionDao.getByIsin(certificate.isin) == null) {
                val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                insertionDao.insert(CertificateInsertion(certificate.isin, today))
                refreshInsertionDates()
            }
            Log.d("ViewModel", "Inserted certificate: ${certificate.isin}")
        }
    }

    fun updateCertificate(certificate: Certificate) {
        viewModelScope.launch {
            dao.update(certificate)
            Log.d("ViewModel", "Updated certificate: ${certificate.isin}")
        }
    }

    fun deleteCertificate(isin: String) {
        viewModelScope.launch {
            dao.deleteByIsin(isin)
            Log.d("ViewModel", "Deleted certificate: $isin")
        }
    }

    fun fetchAndUpdatePrice(isin: String) {
        viewModelScope.launch {
            val cert = certificates.value.find { it.isin == isin } ?: return@launch
            val symbol = cert.underlyingName.trim()
            val now = formatter.format(Date())

            logApi("───────────────────────────────")
            logApi("🔹 Richiesta aggiornamento per $symbol ($isin)")

            val provider = when {
                symbol.endsWith(".MI", ignoreCase = true) -> ApiProvider.MARKETSTACK
                symbol.contains(".") -> ApiProvider.ALPHAVANTAGE
                else -> ApiProvider.TWELVEDATA
            }

            logApi("⚙️ Provider selezionato: ${provider.displayName}")

            when (provider) {
                ApiProvider.TWELVEDATA -> {
                    val result = TwelveDataFetcher.fetchLatestClose(symbol, ApiKeys.TWELVEDATA)
                    handleFetchResult(result, isin, now, provider)
                }
                ApiProvider.MARKETSTACK -> {
                    val result = MarketstackFetcher.fetchLatestClose(symbol, ApiKeys.MARKETSTACK)
                    handleFetchResult(result, isin, now, provider)
                }
                ApiProvider.ALPHAVANTAGE -> {
                    val result = AlphaVantageFetcher.fetchLatestClose(symbol, ApiKeys.ALPHAVANTAGE)
                    handleFetchResult(result, isin, now, provider)
                }
            }
        }
    }

    private fun handleFetchResult(result: FetchResult, isin: String, now: String, provider: ApiProvider) {
        viewModelScope.launch {
            when (result) {
                is FetchResult.Success -> {
                    logApi("✅ ${provider.displayName} → ${result.price}")
                    updateCertificatePrice(isin, result.price, now)
                    incrementApiUsage(provider.displayName)
                }
                is FetchResult.Error -> logApi("❌ ${provider.displayName} → ${result.message}")
            }
        }
    }

    private fun updateCertificatePrice(isin: String, price: Double, timestamp: String) {
        viewModelScope.launch {
            val roundedPrice = (kotlin.math.round(price * 100) / 100.0)
            dao.updatePriceAndTimestamp(isin, roundedPrice, timestamp)
        }
    }

    private fun incrementApiUsage(providerName: String) {
        viewModelScope.launch {
            val usage = apiUsageDao.get(providerName)

            // 1. Usiamo lo stesso formato che vediamo nel Database Inspector (YYYY-MM-DD)
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // 2. Formato completo per il salvataggio (quello che già usi)
            val currentTimestamp = formatter.format(Date())

            if (usage != null) {
                // Se la stringa lastUpdated NON inizia con la data di oggi (es. 2026-01-29), resetta
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

        if (cal.before(today) &&
            cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        ) return null

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