package com.example.certificatestracker

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class MarketstackEodResponse(val data: List<EodData>?)
data class EodData(val date: String?, val close: Double?)

sealed class FetchResult {
    data class Success(val date: String, val close: Double) : FetchResult()
    data class Error(val message: String, val code: Int? = null) : FetchResult()
}

object MarketstackFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun fetchLatestCloseBlocking(
        symbol: String,
        apiKey: String,
        baseUrl: String = "https://api.marketstack.com/v1/eod"
    ): FetchResult {
        val url = "$baseUrl?access_key=$apiKey&symbols=$symbol&limit=1&sort=desc"
        val request = Request.Builder().url(url).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return FetchResult.Error("HTTP ${response.code}: ${response.message}", response.code)
                }
                val body = response.body?.string().orEmpty()
                val parsed = gson.fromJson(body, MarketstackEodResponse::class.java)
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

    // 🚀 Versione sospesa per uso in ViewModel/Compose
    suspend fun fetchLatestClose(
        symbol: String,
        apiKey: String,
        baseUrl: String = "https://api.marketstack.com/v1/eod"
    ): FetchResult = withContext(Dispatchers.IO) {
        fetchLatestCloseBlocking(symbol, apiKey, baseUrl)
    }
}
