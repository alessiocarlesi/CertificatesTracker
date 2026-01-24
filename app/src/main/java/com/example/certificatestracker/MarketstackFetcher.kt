// filename: MarketstackFetcher.kt
package com.example.certificatestracker

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import android.util.Log

data class MarketstackEod(val close: String?)
data class MarketstackResponse(val data: List<MarketstackEod>?)

object MarketstackFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetchLatestClose(symbol: String, apiKey: String): FetchResult = withContext(Dispatchers.IO) {
        val url = "https://api.marketstack.com/v2/eod?symbols=$symbol&access_key=$apiKey"
        Log.d("API_QUERY_OUT", "Sending Marketstack request: $url")

        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                Log.d("API_RESPONSE_RAW", "Marketstack raw response for $symbol: $body")

                if (!response.isSuccessful) {
                    return@withContext FetchResult.Error("HTTP ${response.code}: ${response.message}")
                }

                val parsed = gson.fromJson(body, MarketstackResponse::class.java)
                val price = parsed.data?.firstOrNull()?.close?.toDoubleOrNull()
                if (price != null) {
                    Log.d("API_RESPONSE", "Marketstack price for $symbol: $price")
                    return@withContext FetchResult.Success(price)
                }

                return@withContext FetchResult.Error("No data available for $symbol")
            }
        } catch (e: IOException) {
            Log.e("API_ERROR", "Network error fetching Marketstack for $symbol", e)
            return@withContext FetchResult.Error("Network error: ${e.message}")
        } catch (t: Throwable) {
            Log.e("API_ERROR", "Unexpected error fetching Marketstack for $symbol", t)
            return@withContext FetchResult.Error("Unexpected error: ${t.message}")
        }
    }
}
