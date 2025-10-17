// filename: MonthlyBonusCalculator.kt
package com.example.certificatestracker

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

data class MonthlyBonuses(
    val monthNames: List<String>,
    val bonuses: List<Double>
)

object MonthlyBonusCalculator {

    fun calculate(certificates: List<Certificate>): MonthlyBonuses {
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val monthNames = mutableListOf<String>()

        // ✅ Mesi: corrente + 2 successivi
        for (i in 0 until 3) {
            val monthName = monthFormat.format(calendar.time)
            monthNames.add(monthName.replaceFirstChar { it.uppercase() })
            calendar.add(Calendar.MONTH, 1)
        }

        val globalBonuses = MutableList(3) { 0.0 } // somma totale
        val perIsinBonuses = mutableMapOf<String, MutableList<Double>>() // 🔹 per singolo ISIN
        val dateParser = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        certificates.forEach { cert ->
            val certBonuses = MutableList(3) { 0.0 } // bonus specifici per questo ISIN
            val prezzoSottostante = cert.lastPrice
            val sogliaBonus = cert.bonusLevel
            val sogliaAutocall = cert.autocallLevel
            val quantita = cert.quantity
            val purchasePrice = cert.purchasePrice ?: 0.0
            val premio = cert.premio
            val bonusMonths = cert.bonusMonths.coerceAtLeast(1)

            val bonusDate = cert.nextbonus.takeIf { it.isNotBlank() }?.let {
                try { dateParser.parse(it) } catch (_: Exception) { null }
            }
            val autocallDate = cert.valautocall.takeIf { it.isNotBlank() }?.let {
                try { dateParser.parse(it) } catch (_: Exception) { null }
            }

            val autocallMonthName = autocallDate?.let {
                monthFormat.format(it).replaceFirstChar { c -> c.uppercase() }
            }

            // 🔹 Mesi validi per bonus in base alla frequenza
            val validBonusMonths = mutableSetOf<String>()
            bonusDate?.let {
                val tempCal = Calendar.getInstance().apply { time = it }
                repeat(12 / bonusMonths + 1) {
                    validBonusMonths.add(
                        monthFormat.format(tempCal.time).replaceFirstChar { c -> c.uppercase() }
                    )
                    tempCal.add(Calendar.MONTH, bonusMonths)
                }
            }

            var autocallTriggered = false

            for (monthIndex in 0 until 3) {
                val currentMonthName = monthNames[monthIndex]
                if (autocallTriggered) continue

                // 🔸 AUTOCALL
                if (autocallMonthName == currentMonthName && prezzoSottostante >= sogliaAutocall) {
                    val valore = (premio * quantita) + ((100.0 - purchasePrice) * quantita)
                    certBonuses[monthIndex] += valore
                    globalBonuses[monthIndex] += valore
                    Log.d("BONUS_DEBUG", "🔸 AUTOCALL → ${cert.isin} | $currentMonthName | +${"%.2f".format(valore)} (premio+rimborso)")
                    autocallTriggered = true
                    continue
                }

                // 🔹 BONUS mensile
                if (currentMonthName in validBonusMonths && prezzoSottostante >= sogliaBonus) {
                    val aggiunta = premio * quantita
                    certBonuses[monthIndex] += aggiunta
                    globalBonuses[monthIndex] += aggiunta
                    Log.d("BONUS_DEBUG", "💰 BONUS → ${cert.isin} | $currentMonthName | +${"%.2f".format(aggiunta)} | SogliaBonus=${"%.2f".format(sogliaBonus)} | Prezzo=${"%.2f".format(prezzoSottostante)}")
                }
            }

            // 🔸 salva risultati per l'ISIN
            perIsinBonuses[cert.isin] = certBonuses

            // 🔸 riepilogo singolo
            Log.d(
                "BONUS_DEBUG",
                "✅ Totali ${cert.isin}: " +
                        "${monthNames[0]}=${"%.2f".format(certBonuses[0])} | " +
                        "${monthNames[1]}=${"%.2f".format(certBonuses[1])} | " +
                        "${monthNames[2]}=${"%.2f".format(certBonuses[2])}"
            )
        }

        // 🔹 riepilogo finale per ISIN
        Log.d("BONUS_DEBUG", "===============================")
        Log.d("BONUS_DEBUG", "📊 RIEPILOGO BONUS MENSILI (per ISIN)")
        Log.d("BONUS_DEBUG", "-------------------------------")
        Log.d("BONUS_DEBUG", "ISIN           | ${monthNames[0]} | ${monthNames[1]} | ${monthNames[2]}")
        Log.d("BONUS_DEBUG", "-------------------------------")

        perIsinBonuses.forEach { (isin, list) ->
            Log.d(
                "BONUS_DEBUG",
                "${isin.padEnd(14)} | ${"%.2f".format(list[0]).padStart(6)} | " +
                        "${"%.2f".format(list[1]).padStart(6)} | ${"%.2f".format(list[2]).padStart(6)}"
            )
        }

        // 🔸 Totale portafoglio (somma di tutti i bonus)
        Log.d("BONUS_DEBUG", "-------------------------------")
        Log.d(
            "BONUS_DEBUG",
            "TOTALE PORTAFOGLIO | ${"%.2f".format(globalBonuses[0]).padStart(6)} | " +
                    "${"%.2f".format(globalBonuses[1]).padStart(6)} | ${"%.2f".format(globalBonuses[2]).padStart(6)}"
        )
        Log.d("BONUS_DEBUG", "===============================")

        return MonthlyBonuses(monthNames, globalBonuses)
    }
}
