package com.example.certificatestracker

enum class ApiProvider(
    val displayName: String,
    val dailyLimit: Int,
    val monthlyLimit: Int
) {
    TWELVEDATA("Twelve Data", dailyLimit = 800, monthlyLimit = 24800),
    MARKETSTACK("Marketstack", dailyLimit = 200, monthlyLimit = 200),
    ALPHAVANTAGE("Alpha Vantage", dailyLimit = 25, monthlyLimit = 775),

    // Aggiunto per lo scraping di Borsa Italiana
    // Usiamo limiti simbolici alti perché non è un servizio API a consumo
    BORSA_ITALIANA("Borsa Italiana", dailyLimit = 999999, monthlyLimit = 999999)
}