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

        // Inizializza tutti i bonus a 0 €
        val bonuses = MutableList(3) { 0.0 }

        certificates.forEach { cert ->
            val prezzoSottostante = cert.lastPrice
            val sogliaBonus = cert.bonusLevel
            val sogliaAutocall = cert.autocallLevel
            val quantita = cert.quantity
            val purchasePrice = cert.purchasePrice ?: 0.0
            val premio = cert.premio

            // 🔸 Estrai il mese previsto per il pagamento bonus
            val bonusMonthName = cert.nextbonus.takeIf { it.isNotBlank() }?.let { dateString ->
                try {
                    val date = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(dateString)
                    SimpleDateFormat("MMMM", Locale.getDefault())
                        .format(date!!)
                        .replaceFirstChar { it.uppercase() }
                } catch (e: Exception) {
                    null
                }
            }

            // 🔸 Estrai il mese previsto per autocall (se diverso)
            val autocallMonthName = cert.valautocall.takeIf { it.isNotBlank() }?.let { dateString ->
                try {
                    val date = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(dateString)
                    SimpleDateFormat("MMMM", Locale.getDefault())
                        .format(date!!)
                        .replaceFirstChar { it.uppercase() }
                } catch (e: Exception) {
                    null
                }
            }

            var autocallTriggered = false

            for (monthIndex in 0 until 3) {
                val currentMonthName = monthNames[monthIndex]

                if (autocallTriggered) continue

                // 🔹 AUTOCALL — resta invariato
                if (autocallMonthName == currentMonthName && prezzoSottostante >= sogliaAutocall) {
                    bonuses[monthIndex] += (premio * quantita) + ((100.0 - purchasePrice) * quantita)
                    val debugValue = bonuses[monthIndex]

                    if (debugValue < 0) {
                        android.util.Log.d("BONUS_DEBUG", "Valore NEGATIVO: $debugValue per ISIN=${cert.isin}")
                    }
                    autocallTriggered = true
                    continue
                }

                // 🔹 BONUS MENSILE — valido per tutto il mese, anche dopo la data di pagamento
                if (bonusMonthName != null) {
                    val bonusDate = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(cert.nextbonus)
                    if (bonusDate != null) {
                        val calBonus = Calendar.getInstance().apply { time = bonusDate }
                        val calCurrent = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.MONTH, (Calendar.getInstance().get(Calendar.MONTH) + monthIndex) % 12)
                            set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR) +
                                    ((Calendar.getInstance().get(Calendar.MONTH) + monthIndex) / 12)
                            )
                        }

                        val sameMonth = calBonus.get(Calendar.MONTH) == calCurrent.get(Calendar.MONTH) &&
                                calBonus.get(Calendar.YEAR) == calCurrent.get(Calendar.YEAR)

                        if (sameMonth && prezzoSottostante >= sogliaBonus) {
                            bonuses[monthIndex] += premio * quantita
                        }
                    }
                }
            }
        }

        return MonthlyBonuses(monthNames, bonuses)
    }
}
