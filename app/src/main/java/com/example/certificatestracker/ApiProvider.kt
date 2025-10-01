package com.example.certificatestracker
enum class ApiProvider    ( val displayName: String, val dailyLimit: Int, val monthlyLimit: Int )
{
    TWELVEDATA("Twelve Data", dailyLimit = 800, monthlyLimit = 24800),
    MARKETSTACK("Marketstack", dailyLimit = 200, monthlyLimit = 200),
    ALPHAVANTAGE("Alpha Vantage", dailyLimit = 25, monthlyLimit = 775)

}
// STOCKDATA("Stockdata", dailyLimit = 100, monthlyLimit = 100) }