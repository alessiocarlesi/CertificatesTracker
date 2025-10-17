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

    /**
     * Calcola i bonus mensili per ciascun certificato (mappa ISIN → MonthlyBonuses)
     */
    fun calculatePerCertificate(certificates: List<Certificate>): Map<String, MonthlyBonuses> {
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val dateParser = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        // ✅ Mesi: corrente + 2 successivi
        val monthNames = MutableList(3) {
            val name = monthFormat.format(calendar.time)
            calendar.add(Calendar.MONTH, 1)
            name.replaceFirstChar { it.uppercase() }
        }

        val result = mutableMapOf<String, MonthlyBonuses>()

        certificates.forEach { cert ->
            val prezzoSottostante = cert.lastPrice
            val sogliaBonus = cert.bonusLevel
            val sogliaAutocall = cert.autocallLevel
            val quantita = cert.quantity
            val purchasePrice = cert.purchasePrice ?: 0.0
            val premio = cert.premio

            val bonusDate = cert.nextbonus.takeIf { it.isNotBlank() }?.let {
                try { dateParser.parse(it) } catch (_: Exception) { null }
            }
            val autocallDate = cert.valautocall.takeIf { it.isNotBlank() }?.let {
                try { dateParser.parse(it) } catch (_: Exception) { null }
            }

            val bonusMonthName = bonusDate?.let {
                monthFormat.format(it).replaceFirstChar { c -> c.uppercase() }
            }
            val autocallMonthName = autocallDate?.let {
                monthFormat.format(it).replaceFirstChar { c -> c.uppercase() }
            }

            val bonuses = MutableList(3) { 0.0 }
            var autocallTriggered = false

            for (monthIndex in 0 until 3) {
                val currentMonthName = monthNames[monthIndex]
                if (autocallTriggered) break

                // 🔸 AUTOCALL
                if (autocallMonthName == currentMonthName && prezzoSottostante >= sogliaAutocall) {
                    val inc = (premio * quantita) + ((100.0 - purchasePrice) * quantita)
                    bonuses[monthIndex] += inc
                    autocallTriggered = true
                    break
                }

                // 🔹 BONUS mensile
                if (bonusMonthName == currentMonthName && prezzoSottostante >= sogliaBonus) {
                    val inc = premio * quantita
                    bonuses[monthIndex] += inc
                }
            }

            result[cert.isin] = MonthlyBonuses(monthNames, bonuses)
        }

        return result
    }

    /**
     * Calcola la somma totale mese per mese su tutti i certificati
     */
    fun calculateTotal(certificates: List<Certificate>): MonthlyBonuses {
        val perCert = calculatePerCertificate(certificates)
        if (perCert.isEmpty()) return MonthlyBonuses(emptyList(), emptyList())

        val monthNames = perCert.values.first().monthNames
        val totals = MutableList(monthNames.size) { 0.0 }

        perCert.values.forEach { m ->
            m.bonuses.forEachIndexed { i, v -> totals[i] += v }
        }

        return MonthlyBonuses(monthNames, totals)
    }

    /**
     * ✅ Retrocompatibilità per CertificatesScreen.kt
     */
    fun calculate(certificates: List<Certificate>): MonthlyBonuses {
        return calculateTotal(certificates)
    }

    /**
     * 🧾 Stampa tabella leggibile in Logcat
     */
    fun printDebug(certificates: List<Certificate>) {
        val perCert = calculatePerCertificate(certificates)
        if (perCert.isEmpty()) {
            Log.d("BONUS_DEBUG", "Nessun certificato disponibile per il calcolo.")
            return
        }

        val monthNames = perCert.values.first().monthNames
        val totals = MutableList(monthNames.size) { 0.0 }

        Log.d("BONUS_DEBUG", "===============================")
        Log.d("BONUS_DEBUG", "📊 RIEPILOGO BONUS MENSILI")
        Log.d("BONUS_DEBUG", "-------------------------------")
        Log.d("BONUS_DEBUG", "ISIN           | " + monthNames.joinToString(" | "))
        Log.d("BONUS_DEBUG", "-------------------------------")

        perCert.forEach { (isin, data) ->
            val values = data.bonuses.mapIndexed { i, v ->
                totals[i] += v
                "%6.2f".format(v)
            }.joinToString(" | ")
            Log.d("BONUS_DEBUG", "${isin.padEnd(13)} | $values")
        }

        Log.d("BONUS_DEBUG", "-------------------------------")
        val totalLine = totals.joinToString(" | ") { "%6.2f".format(it) }
        Log.d("BONUS_DEBUG", "TOTALE         | $totalLine")
        Log.d("BONUS_DEBUG", "===============================")
    }
}
