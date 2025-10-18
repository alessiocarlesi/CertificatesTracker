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

    // 🔹 Funzione base, usata da CertificatesScreen (resta invariata)
    fun calculate(certificates: List<Certificate>): MonthlyBonuses {
        val (months, _, totals) = calculateDetailed(certificates)
        return MonthlyBonuses(months, totals)
    }

    // 🔹 Nuova funzione con dettagli per ISIN (usata dalla nuova schermata)
    fun calculateDetailed(certificates: List<Certificate>):
            Triple<List<String>, Map<String, List<Double>>, List<Double>> {

        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val monthNames = mutableListOf<String>()

        // Mese corrente + 2 successivi
        for (i in 0 until 3) {
            val monthName = monthFormat.format(calendar.time)
            monthNames.add(monthName.replaceFirstChar { it.uppercase() })
            calendar.add(Calendar.MONTH, 1)
        }

        val globalBonuses = MutableList(3) { 0.0 }
        val perIsinBonuses = mutableMapOf<String, MutableList<Double>>()
        val dateParser = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        certificates.forEach { cert ->
            val certBonuses = MutableList(3) { 0.0 }

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

            // Mesi validi per bonus in base alla frequenza
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
                    autocallTriggered = true
                    Log.d("BONUS_DEBUG", "🔸 AUTOCALL → ${cert.isin} | $currentMonthName | +${"%.2f".format(valore)}")
                    continue
                }

                // 🔹 BONUS mensile
                if (currentMonthName in validBonusMonths && prezzoSottostante >= sogliaBonus) {
                    val aggiunta = premio * quantita
                    certBonuses[monthIndex] += aggiunta
                    globalBonuses[monthIndex] += aggiunta
                    Log.d("BONUS_DEBUG", "💰 BONUS → ${cert.isin} | $currentMonthName | +${"%.2f".format(aggiunta)}")
                }
            }

            perIsinBonuses[cert.isin] = certBonuses
        }

        return Triple(monthNames, perIsinBonuses, globalBonuses)
    }
}
