package com.example.certificatestracker

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class AlphaVantageResponse(
    @SerializedName("Global Quote") val globalQuote: GlobalQuote?
)

data class GlobalQuote(
    @SerializedName("01. symbol") val symbol: String?,
    @SerializedName("05. price") val price: String?
)

object AlphaVantageFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetchLatestClose(symbol: String, apiKey: String): FetchResult = withContext(Dispatchers.IO) {
        val url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=$symbol&apikey=$apiKey&datatype=json"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext FetchResult.Error("HTTP ${response.code}: ${response.message}")

                val body = response.body?.string().orEmpty()
                val parsed = gson.fromJson(body, AlphaVantageResponse::class.java)
                val priceStr = parsed.globalQuote?.price

                if (priceStr != null) {
                    val price = priceStr.toDoubleOrNull()
                    if (price != null) return@withContext FetchResult.Success(price)
                }

                FetchResult.Error("No data available for $symbol")
            }
        } catch (io: IOException) {
            FetchResult.Error("Network error: ${io.message}")
        } catch (t: Throwable) {
            FetchResult.Error("Unexpected error: ${t.message}")
        }
    }

    // 🔹 Per sviluppo senza consumare API
    /*
    suspend fun fetchLatestClose(symbol: String, apiKey: String): FetchResult {
        return FetchResult.Success(10.10)
    }
    */
}
