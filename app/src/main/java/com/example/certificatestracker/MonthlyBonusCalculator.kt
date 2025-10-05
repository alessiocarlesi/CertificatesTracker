package com.example.certificatestracker

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

data class MonthlyBonuses(
    val monthNames: List<String>,
    val bonuses: List<Double>
)

class MonthlyBonusCalculator {

    companion object {
        fun calculate(certificates: List<Certificate>): MonthlyBonuses {
            val now = Calendar.getInstance()

            val monthNames = (0..2).map { offset ->
                val cal = now.clone() as Calendar
                cal.add(Calendar.MONTH, offset)
                cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "N/A"
            }

            val bonuses = MutableList(3) { 0.0 }

            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            for (cert in certificates) {
                if (cert.nextbonus.isBlank()) continue

                val nextBonusDate = try {
                    formatter.parse(cert.nextbonus)
                } catch (e: Exception) {
                    Log.e("MonthlyBonusCalculator", "Errore parsing data: ${cert.nextbonus}", e)
                    null
                } ?: continue

                val calBonus = Calendar.getInstance()
                calBonus.time = nextBonusDate

                for (i in 0..2) {
                    val calTarget = now.clone() as Calendar
                    calTarget.add(Calendar.MONTH, i)
                    if (calBonus.get(Calendar.MONTH) == calTarget.get(Calendar.MONTH) &&
                        calBonus.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR)
                        && cert.lastPrice >= cert.barrier
                    ) {
                        bonuses[i] += cert.premio * cert.quantity
                    }
                }
            }

            for (i in 0..2) {
                Log.d("MonthlyBonusCalculator", "Bonus ${monthNames[i]}: ${bonuses[i]}")
            }

            return MonthlyBonuses(monthNames, bonuses)
        }
    }
}
