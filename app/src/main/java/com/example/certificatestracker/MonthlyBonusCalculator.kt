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
        val (months, _, totals, _) = calculateDetailed(certificates)
        return MonthlyBonuses(months, totals)
    }

    fun calculateDetailed(certificates: List<Certificate>):
            Quadruple<List<String>, Map<String, List<Double>>, List<Double>, List<Double>> {

        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val monthNames = mutableListOf<String>()

        // Mese corrente + 2 successivi
        repeat(3) {
            val monthName = monthFormat.format(calendar.time)
            monthNames.add(monthName.replaceFirstChar { it.uppercase() })
            calendar.add(Calendar.MONTH, 1)
        }

        val globalBonuses = MutableList(3) { 0.0 }        // Totali reali
        val virtualBonuses = MutableList(3) { 0.0 }       // Totali simulati
        val perIsinBonuses = mutableMapOf<String, MutableList<Double>>()
        val dateParser = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        for (cert in certificates) {
            val certBonuses = MutableList(3) { 0.0 }

            val prezzo = cert.lastPrice
            val sogliaBonus = cert.bonusLevel
            val sogliaAutocall = cert.autocallLevel
            val qty = cert.quantity
            val premio = cert.premio
            val purchasePrice = cert.purchasePrice ?: 0.0

            val isVirtual = ((purchasePrice * 1000).toInt() % 10 == 1)

            val bonusDate = cert.nextbonus.takeIf { it.isNotBlank() }?.let {
                try { dateParser.parse(it) } catch (_: Exception) { null }
            }
            val autocallDate = cert.valautocall.takeIf { it.isNotBlank() }?.let {
                try { dateParser.parse(it) } catch (_: Exception) { null }
            }

            val autocallMonthName = autocallDate?.let {
                monthFormat.format(it).replaceFirstChar { c -> c.uppercase() }
            }

            val validBonusMonths = mutableSetOf<String>()
            bonusDate?.let {
                val tmp = Calendar.getInstance().apply { time = it }
                repeat(12 / cert.bonusMonths.coerceAtLeast(1) + 1) {
                    validBonusMonths.add(
                        monthFormat.format(tmp.time).replaceFirstChar { c -> c.uppercase() }
                    )
                    tmp.add(Calendar.MONTH, cert.bonusMonths.coerceAtLeast(1))
                }
            }

            var autocallTriggered = false

            for (monthIndex in 0 until 3) {
                val currentMonth = monthNames[monthIndex]
                if (autocallTriggered) continue

                // Autocall
                if (autocallMonthName == currentMonth && prezzo >= sogliaAutocall) {
                    val value = (premio * qty) + ((100.0 - purchasePrice) * qty)
                    certBonuses[monthIndex] += value
                    if (isVirtual) virtualBonuses[monthIndex] += value else globalBonuses[monthIndex] += value
                    autocallTriggered = true
                    continue
                }

                // Bonus regolare
                if (currentMonth in validBonusMonths && prezzo >= sogliaBonus) {
                    val value = premio * qty
                    certBonuses[monthIndex] += value
                    if (isVirtual) virtualBonuses[monthIndex] += value else globalBonuses[monthIndex] += value
                }
            }

            perIsinBonuses[cert.isin] = certBonuses
        }

        return Quadruple(monthNames, perIsinBonuses, globalBonuses, virtualBonuses)
    }

    // Struttura per restituire 4 valori
    data class Quadruple<A, B, C, D>(
        val first: A, val second: B, val third: C, val fourth: D
    )
}
