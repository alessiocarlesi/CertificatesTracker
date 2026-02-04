package com.example.certificatestracker

// Struttura fissa per i risultati dei calcoli (Registri di Output)
data class PortfolioStats(
    val capitaleInvestito: Double = 0.0,
    val valoreAttuale: Double = 0.0,
    val gainLoss: Double = 0.0,
    val gainLossPerc: Double = 0.0
)

object PortfolioCalculators {

    /**
     * Calcola i totali del portafoglio basandosi sui 19.000€ investiti e i prezzi correnti
     */
    fun compute(certificates: List<Certificate>): PortfolioStats {
        var investito = 0.0
        var attuale = 0.0

        certificates.forEach { cert ->
            // Accumulo quantità * prezzo acquisto (dal DB v12)
            investito += (cert.purchasePrice ?: 0.0) * cert.quantity
            // Accumulo quantità * ultimo prezzo certificato (da Borsa IT)
            attuale += cert.lastPrice * cert.quantity
        }

        val diff = attuale - investito
        val perc = if (investito > 0) (diff / investito) * 100 else 0.0

        return PortfolioStats(investito, attuale, diff, perc)
    }

    /**
     * Calcola la distanza tra il sottostante e la soglia di rimborso anticipato
     */
    fun calcolaDistanzaAutocall(cert: Certificate): Double {
        // Se mancano i dati, restituiamo 0 per non sporcare la tabella
        if (cert.autocallLevel <= 0.0 || cert.underlyingPrice <= 0.0) return 0.0

        // Calcolo percentuale: (Prezzo attuale - Soglia) / Soglia * 100
        return ((cert.underlyingPrice - cert.autocallLevel) / cert.autocallLevel) * 100
    }

    /**
     * Calcola il rendimento percentuale mensile
     */
    fun calcolaYieldMensile(bonusMensile: Double, capitaleInvestito: Double): Double {
        if (capitaleInvestito <= 0.0) return 0.0
        return (bonusMensile / capitaleInvestito) * 100
    }

    /**
     * Calcola il rendimento annualizzato basato su un mese
     */
    fun calcolaYieldAnnuo(yieldMensile: Double): Double {
        return yieldMensile * 12
    }
}