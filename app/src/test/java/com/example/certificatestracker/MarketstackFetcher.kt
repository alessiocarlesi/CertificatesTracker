package com.example.certificatestracker

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.io.IOException

// Modello semplice per Gson
data class MarketstackEodResponse(val data: List<EodData>?)
data class EodData(val date: String?, val close: Double?)

// Risultato esplicito per facilitare i test
sealed class FetchResult {
    data class Success(val date: String, val close: Double) : FetchResult()
    data class Error(val message: String, val code: Int? = null) : FetchResult()
}

object MarketstackFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * Funzione sincrona minimale che effettua la chiamata HTTP e restituisce un FetchResult.
     * Puoi usarla direttamente dai test JVM (no coroutines necessarie).
     */
    fun fetchLatestCloseBlocking(symbol: String, apiKey: String, baseUrl: String = "http://api.marketstack.com/v1/eod"): FetchResult {
        val url = "$baseUrl?access_key=$apiKey&symbols=$symbol&limit=1&sort=desc"
        val request = Request.Builder().url(url).build()

        return try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    return FetchResult.Error("HTTP $code: ${response.message} ${if (body.isBlank()) "" else "/ $body"}", code)
                }

                val body = response.body?.string().orEmpty()
                val parsed = try {
                    gson.fromJson(body, MarketstackEodResponse::class.java)
                } catch (e: Exception) {
                    return FetchResult.Error("JSON parse error: ${e.message}")
                }

                val eod = parsed.data?.firstOrNull()
                if (eod?.date != null && eod.close != null) {
                    FetchResult.Success(eod.date, eod.close)
                } else {
                    FetchResult.Error("No data available for $symbol")
                }
            }
        } catch (io: IOException) {
            FetchResult.Error("Network error: ${io.message}")
        } catch (t: Throwable) {
            FetchResult.Error("Unexpected error: ${t.message}")
        }
    }

    // NOTE: se più avanti vuoi la versione suspend, aggiungo volentieri la wrapper con coroutines.
}
