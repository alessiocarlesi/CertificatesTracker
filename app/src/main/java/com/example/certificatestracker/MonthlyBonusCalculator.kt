// filename: MonthlyBonusCalculator.kt
package com.example.certificatestracker

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

        // ✅ Solo 3 mesi: corrente + 2 successivi
        for (i in 0 until 3) {
            val monthName = monthFormat.format(calendar.time)
            monthNames.add(monthName.replaceFirstChar { it.uppercase() })
            calendar.add(Calendar.MONTH, 1)
        }

        val bonuses = MutableList(3) { 0.0 }

        certificates.forEach { cert ->
            val prezzoSottostante = cert.lastPrice
            val sogliaBonus = cert.bonusLevel
            val sogliaAutocall = cert.autocallLevel
            val quantita = cert.quantity
            val purchasePrice = cert.purchasePrice ?: 0.0
            val premio = cert.premio

            // 🔹 Estrai il mese di valutazione autocall
            val autocallMonthName = cert.valautocall?.let { dateString ->
                try {
                    val date = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(dateString)
                    SimpleDateFormat("MMMM", Locale.getDefault())
                        .format(date!!)
                        .replaceFirstChar { it.uppercase() }
                } catch (e: Exception) {
                    null
                }
            }

            // ✅ Variabile che indica se il certificato è già estinto da autocall
            var autocallTriggered = false

            for (monthIndex in 0 until 3) {
                val currentMonthName = monthNames[monthIndex]

                // Se autocall già scattata in un mese precedente → skip
                if (autocallTriggered) continue

                if (autocallMonthName == currentMonthName) {
                    // 🔸 Mese di valutazione autocall
                    if (prezzoSottostante >= sogliaAutocall) {
                        bonuses[monthIndex] += (premio * quantita) + (100.0 - purchasePrice) * quantita
                        autocallTriggered = true  // certificato estinto da questo mese in poi
                    } else if (prezzoSottostante >= sogliaBonus) {
                        bonuses[monthIndex] += premio * quantita
                    }
                } else {
                    // 🔸 Altri mesi → solo bonus (se autocall non ancora scattata)
                    if (prezzoSottostante >= sogliaBonus) {
                        bonuses[monthIndex] += premio * quantita
                    }
                }
            }
        }

        return MonthlyBonuses(monthNames, bonuses)
    }
}
