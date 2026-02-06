package com.example.certificatestracker

// Struttura fissa per i risultati dei calcoli
data class PortfolioStats(
    val capitaleInvestito: Double = 0.0,
    val valoreAttuale: Double = 0.0,
    val gainLoss: Double = 0.0,
    val gainLossPerc: Double = 0.0
)

object PortfolioCalculators {

    /**
     * Calcola i totali del portafoglio (Invariato, usa lastPrice del certificato)
     */
    fun compute(certificates: List<Certificate>): PortfolioStats {
        var investito = 0.0
        var attuale = 0.0

        certificates.forEach { cert ->
            investito += (cert.purchasePrice ?: 0.0) * cert.quantity
            attuale += cert.lastPrice * cert.quantity
        }

        val diff = attuale - investito
        val perc = if (investito > 0) (diff / investito) * 100 else 0.0

        return PortfolioStats(investito, attuale, diff, perc)
    }

    /**
     * 🚀 NUOVA LOGICA v13: Calcola la distanza del Worst-Of dalla soglia % Autocall
     */
    fun calcolaDistanzaAutocall(cert: Certificate, viewModel: CertificatesViewModel): Double {
        // 1. Raccogliamo i sottostanti attivi
        val sottostanti = listOf(
            cert.und1 to cert.und1Strike,
            cert.und2 to cert.und2Strike,
            cert.und3 to cert.und3Strike,
            cert.und4 to cert.und4Strike,
            cert.und5 to cert.und5Strike,
            cert.und6 to cert.und6Strike
        ).filter { !it.first.isNullOrBlank() && it.second > 0.0 }

        if (sottostanti.isEmpty()) return 0.0

        // 2. Troviamo la performance del peggiore nel paniere
        val worstPerf = sottostanti.map { (ticker, strike) ->
            val currentPrice = viewModel.getLastKnownPrice(ticker!!)
            if (strike > 0.0) ((currentPrice - strike) / strike * 100.0) else -100.0
        }.minByOrNull { it } ?: 0.0

        // 3. Distanza = (Perf. Attuale) - (Soglia Autocall % - 100)
        // Esempio: Perf -5%, Soglia 100% (0%). Distanza = -5 - 0 = -5%
        return worstPerf - (cert.autocallPerc - 100.0)
    }

    fun calcolaYieldMensile(bonusMensile: Double, capitaleInvestito: Double): Double {
        if (capitaleInvestito <= 0.0) return 0.0
        return (bonusMensile / capitaleInvestito) * 100
    }

    fun calcolaYieldAnnuo(yieldMensile: Double): Double {
        return yieldMensile * 12
    }
}