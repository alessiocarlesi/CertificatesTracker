package com.example.certificatestracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.io.IOException

data class MarketstackEodResponse(val data: List<EodData>?)
data class EodData(val date: String?, val close: Double?)

object MarketstackFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetchLatestClose(symbol: String, apiKey: String): FetchResult = withContext(Dispatchers.IO) {
        val url = "http://api.marketstack.com/v1/eod?access_key=$apiKey&symbols=$symbol&limit=1&sort=desc"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext FetchResult.Error("HTTP ${response.code}: ${response.message}")

                val body = response.body?.string().orEmpty()
                val parsed = gson.fromJson(body, MarketstackEodResponse::class.java)
                val eod = parsed.data?.firstOrNull()
                val close = eod?.close
                if (close != null) return@withContext FetchResult.Success(close)

                FetchResult.Error("No data available for $symbol")
            }
        } catch (e: IOException) {
            FetchResult.Error("Network error: ${e.message}")
        } catch (t: Throwable) {
            FetchResult.Error("Unexpected error: ${t.message}")
        }
    }
}
