package com.example.certificatestracker

sealed class FetchResult {
    data class Success(val price: Double) : FetchResult()
    data class Error(val message: String) : FetchResult()
}
