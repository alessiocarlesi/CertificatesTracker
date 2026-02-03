package com.example.certificatestracker

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object BorsaItalianaFetcher {

    suspend fun fetchLatestClose(isin: String): FetchResult = withContext(Dispatchers.IO) {
        if (isin.length < 10) return@withContext FetchResult.Error("ISIN non valido")

        // Gli URL più probabili per i dati testuali che abbiamo visto nelle tue foto
        val urlsToTry = listOf(
            "https://www.borsaitaliana.it/borsa/obbligazioni/eurotlx/dati-completi/$isin.html?lang=it",
            "https://www.borsaitaliana.it/borsa/cw-e-certificates/dati-completi/$isin.html?lang=it",
            "https://www.borsaitaliana.it/borsa/obbligazioni/eurotlx/scheda/$isin.html?lang=it",
            "https://www.borsaitaliana.it/borsa/cw-e-certificates/scheda/$isin.html?lang=it"
        )

        for (url in urlsToTry) {
            try {
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get()

                // 🔹 SELETTORI MIRATI DALL'ISPEZIONE
                val prezzoTesto =
                    // 1. Cerca lo span con la classe vista nell'ispezione (image_d2e509)
                    doc.select("td:contains(Prezzo di riferimento) ~ td span.t-text.-right").first()?.text()
                    // 2. Cerca il valore nella cella accanto all'etichetta
                        ?: doc.select("td:contains(Prezzo di riferimento) + td").first()?.text()
                        // 3. Cerca l'etichetta "Prezzo ultimo contratto" (spesso usata in EuroTLX)
                        ?: doc.select("td:contains(Prezzo ultimo contratto) + td").first()?.text()
                        // 4. Selettore generico per la classe vista
                        ?: doc.select("span.t-text.-right").firstOrNull { it.text().contains(",") }?.text()

                if (!prezzoTesto.isNullOrBlank()) {
                    // Pulizia: rimuove punti delle migliaia e converte virgola in punto
                    val cleanPrice = prezzoTesto.replace(".", "").replace(",", ".")
                        .replace(Regex("[^0-9.]"), "")

                    val price = cleanPrice.toDoubleOrNull()
                    if (price != null && price > 0) {
                        Log.d("SCRAPER_SUCCESS", "Trovato su $url: $price")
                        return@withContext FetchResult.Success(price)
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        FetchResult.Error("Dato non trovato per $isin (provate 4 varianti)")
    }
}