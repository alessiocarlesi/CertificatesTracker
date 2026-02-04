package com.example.certificatestracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import org.json.JSONObject

object YahooFinanceFetcher {

    suspend fun getPrice(ticker: String): Double? {
        return withContext(Dispatchers.IO) {
            try {
                // Usiamo l'endpoint "query2" che è più moderno e meno protetto
                val url = "https://query2.finance.yahoo.com/v8/finance/chart/$ticker?interval=1d&range=1d"
                val connection = URL(url).openConnection()

                // Header necessari per sembrare un client legittimo
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.setRequestProperty("Accept", "application/json")

                val response = connection.getInputStream().bufferedReader().use { it.readText() }

                // Estrazione del prezzo dal JSON di risposta
                val json = JSONObject(response)
                val result = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
                val meta = result.getJSONObject("meta")

                // Ritorna il prezzo corrente
                meta.getDouble("regularMarketPrice")
            } catch (e: Exception) {
                null // In caso di errore, il ViewModel passerà a Marketstack
            }
        }
    }
}