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
        insertionDates: Map<String, String> = emptyMap(),
        viewModel: CertificatesViewModel // 🔹 Necessario per getLastKnownPrice
    ): MonthlyBonuses {
        val (months, _, totals, _) = calculateDetailed(certificates, insertionDates, viewModel)
        return MonthlyBonuses(months, totals)
    }

    fun calculateDetailed(
        certificates: List<Certificate>,
        insertionDates: Map<String, String>,
        viewModel: CertificatesViewModel // 🔹 Passiamo il ViewModel
    ): Quadruple<List<String>, Map<String, List<Double>>, List<Double>, List<Double>> {

        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val monthNames = mutableListOf<String>()
        val analysisMonths = mutableListOf<Triple<Int, Int, Calendar>>()

        val tempCal = Calendar.getInstance()
        repeat(3) {
            val monthName = monthFormat.format(tempCal.time)
            monthNames.add(monthName.replaceFirstChar { it.uppercase() })
            analysisMonths.add(Triple(tempCal.get(Calendar.MONTH), tempCal.get(Calendar.YEAR), tempCal.clone() as Calendar))
            tempCal.add(Calendar.MONTH, 1)
        }

        val globalBonuses = MutableList(3) { 0.0 }
        val virtualBonuses = MutableList(3) { 0.0 }
        val perIsinBonuses = mutableMapOf<String, MutableList<Double>>()

        val dateParser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val bonusDateParser = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        for (cert in certificates) {
            // 🛠️ LOGICA WORST-OF v13
            val sottostanti = listOf(
                cert.und1 to cert.und1Strike,
                cert.und2 to cert.und2Strike,
                cert.und3 to cert.und3Strike,
                cert.und4 to cert.und4Strike,
                cert.und5 to cert.und5Strike,
                cert.und6 to cert.und6Strike
            ).filter { !it.first.isNullOrBlank() && it.second > 0.0 }

            val worstPerf = sottostanti.map { (ticker, strike) ->
                val currentPrice = viewModel.getLastKnownPrice(ticker!!)
                if (strike > 0.0) ((currentPrice - strike) / strike * 100.0) else -100.0
            }.minByOrNull { it } ?: 0.0

            val certBonuses = MutableList(3) { 0.0 }
            val insertionDateStr = insertionDates[cert.isin]
            val purchaseDate = insertionDateStr?.let {
                try { dateParser.parse(it) } catch (_: Exception) { null }
            }

            val nextBonusDate = cert.nextbonus.takeIf { it.isNotBlank() }?.let {
                try { bonusDateParser.parse(it) } catch (_: Exception) { null }
            }
            val autocallDate = cert.valautocall.takeIf { it.isNotBlank() }?.let {
                try { bonusDateParser.parse(it) } catch (_: Exception) { null }
            }

            val qty = cert.quantity
            val premio = cert.premio
            val purchasePrice = cert.purchasePrice ?: 0.0
            val isVirtual = ((purchasePrice * 1000).toInt() % 10 == 1)

            var autocallTriggered = false

            for (monthIndex in 0 until 3) {
                if (autocallTriggered) continue
                val (targetMonth, targetYear, _) = analysisMonths[monthIndex]

                if (purchaseDate != null) {
                    val pCal = Calendar.getInstance().apply { time = purchaseDate }
                    if (targetYear < pCal.get(Calendar.YEAR) ||
                        (targetYear == pCal.get(Calendar.YEAR) && targetMonth < pCal.get(Calendar.MONTH))) {
                        continue
                    }
                }

                // --- 2. LOGICA AUTOCALL BASATA SU % (v13) ---
                if (autocallDate != null) {
                    val aCal = Calendar.getInstance().apply { time = autocallDate }
                    if (aCal.get(Calendar.MONTH) == targetMonth && aCal.get(Calendar.YEAR) == targetYear) {
                        // Verifichiamo se la performance del Worst-Of è sopra la soglia Autocall %
                        if (worstPerf >= (cert.autocallPerc - 100.0)) {
                            val value = (premio * qty) + ((100.0 - purchasePrice) * qty)
                            certBonuses[monthIndex] += value
                            if (isVirtual) virtualBonuses[monthIndex] += value else globalBonuses[monthIndex] += value
                            autocallTriggered = true
                            continue
                        }
                    }
                }

                // --- 3. LOGICA BONUS BASATA SU % (v13) ---
                if (nextBonusDate != null && worstPerf >= (cert.bonusPerc - 100.0)) {
                    val bCal = Calendar.getInstance().apply { time = nextBonusDate }
                    val diffMonths = (targetYear - bCal.get(Calendar.YEAR)) * 12 + (targetMonth - bCal.get(Calendar.MONTH))

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