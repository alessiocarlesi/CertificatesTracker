package com.example.certificatestracker

import java.text.SimpleDateFormat
import java.util.*

data class MonthlyBonuses(
    val monthNames: List<String>,
    val bonuses: List<Double>
)

object MonthlyBonusCalculator {

    fun calculate(
        certificates: List<Certificate>,
        insertionDates: Map<String, String> = emptyMap()
    ): MonthlyBonuses {
        val (months, _, totals, _) = calculateDetailed(certificates, insertionDates)
        return MonthlyBonuses(months, totals)
    }

    fun calculateDetailed(
        certificates: List<Certificate>,
        insertionDates: Map<String, String>
    ): Quadruple<List<String>, Map<String, List<Double>>, List<Double>, List<Double>> {

        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val monthNames = mutableListOf<String>()
        val analysisMonths = mutableListOf<Triple<Int, Int, Calendar>>()

        val tempCal = Calendar.getInstance()
        repeat(3) {
            val monthName = monthFormat.format(tempCal.time)
            monthNames.add(monthName.replaceFirstChar { it.uppercase() })
            // Salviamo Mese, Anno e un'istanza del Calendario per i confronti
            analysisMonths.add(Triple(tempCal.get(Calendar.MONTH), tempCal.get(Calendar.YEAR), tempCal.clone() as Calendar))
            tempCal.add(Calendar.MONTH, 1)
        }

        val globalBonuses = MutableList(3) { 0.0 }
        val virtualBonuses = MutableList(3) { 0.0 }
        val perIsinBonuses = mutableMapOf<String, MutableList<Double>>()

        val dateParser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val bonusDateParser = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        for (cert in certificates) {
            val certBonuses = MutableList(3) { 0.0 }
            val insertionDateStr = insertionDates[cert.isin]
            val purchaseDate = insertionDateStr?.let {
                try { dateParser.parse(it) } catch (_: Exception) { null }
            }

            // Date del certificato
            val nextBonusDate = cert.nextbonus.takeIf { it.isNotBlank() }?.let {
                try { bonusDateParser.parse(it) } catch (_: Exception) { null }
            }
            val autocallDate = cert.valautocall.takeIf { it.isNotBlank() }?.let {
                try { bonusDateParser.parse(it) } catch (_: Exception) { null }
            }

            val prezzo = cert.lastPrice
            val qty = cert.quantity
            val premio = cert.premio
            val purchasePrice = cert.purchasePrice ?: 0.0
            val isVirtual = ((purchasePrice * 1000).toInt() % 10 == 1)

            var autocallTriggered = false

            for (monthIndex in 0 until 3) {
                if (autocallTriggered) continue
                val (targetMonth, targetYear, targetCal) = analysisMonths[monthIndex]

                // --- 1. FILTRO DATA ACQUISTO ---
                if (purchaseDate != null) {
                    val pCal = Calendar.getInstance().apply { time = purchaseDate }
                    if (targetYear < pCal.get(Calendar.YEAR) ||
                        (targetYear == pCal.get(Calendar.YEAR) && targetMonth < pCal.get(Calendar.MONTH))) {
                        continue
                    }
                }

                // --- 2. LOGICA AUTOCALL ---
                if (autocallDate != null) {
                    val aCal = Calendar.getInstance().apply { time = autocallDate }
                    if (aCal.get(Calendar.MONTH) == targetMonth && aCal.get(Calendar.YEAR) == targetYear) {
                        if (prezzo >= cert.autocallLevel) {
                            val value = (premio * qty) + ((100.0 - purchasePrice) * qty)
                            certBonuses[monthIndex] += value
                            if (isVirtual) virtualBonuses[monthIndex] += value else globalBonuses[monthIndex] += value
                            autocallTriggered = true
                            continue
                        }
                    }
                }

                // --- 3. LOGICA BONUS (SOLO SE NON AUTOCALLATO) ---
                if (nextBonusDate != null && prezzo >= cert.bonusLevel) {
                    val bCal = Calendar.getInstance().apply { time = nextBonusDate }

                    // Verifichiamo se il mese target è un mese di stacco valido
                    // partendo dal nextBonusDate e aggiungendo i mesi di frequenza
                    val diffMonths = (targetYear - bCal.get(Calendar.YEAR)) * 12 + (targetMonth - bCal.get(Calendar.MONTH))

                    // Il mese è valido se:
                    // a) Non è nel passato rispetto al prossimo bonus
                    // b) La differenza di mesi è un multiplo della frequenza (bonusMonths)
                    if (diffMonths >= 0 && diffMonths % cert.bonusMonths.coerceAtLeast(1) == 0) {
                        val value = premio * qty
                        certBonuses[monthIndex] += value
                        if (isVirtual) virtualBonuses[monthIndex] += value else globalBonuses[monthIndex] += value
                    }
                }
            }
            perIsinBonuses[cert.isin] = certBonuses
        }

        return Quadruple(monthNames, perIsinBonuses, globalBonuses, virtualBonuses)
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}