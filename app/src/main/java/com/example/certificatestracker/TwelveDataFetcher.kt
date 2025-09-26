package com.example.certificatestracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.io.IOException

data class TwelveDataResponse(val close: String?, val datetime: String?)

object TwelveDataFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetchLatestClose(symbol: String, apiKey: String): FetchResult = withContext(Dispatchers.IO) {
        val url = "https://api.twelvedata.com/eod?symbol=$symbol&apikey=$apiKey"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext FetchResult.Error("HTTP ${response.code}: ${response.message}")

                val body = response.body?.string().orEmpty()
                val parsed = gson.fromJson(body, TwelveDataResponse::class.java)
                val price = parsed.close?.toDoubleOrNull()
                if (price != null) return@withContext FetchResult.Success(price)

                FetchResult.Error("No data available for $symbol")
            }
        } catch (e: IOException) {
            FetchResult.Error("Network error: ${e.message}")
        } catch (t: Throwable) {
            FetchResult.Error("Unexpected error: ${t.message}")
        }
    }
}
