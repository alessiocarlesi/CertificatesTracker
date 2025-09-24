package com.example.certificatestracker

import org.junit.Test

class MarketstackFetcherTest {

    private val apiKey = "e1e60f41a11968b889595584e0a6c310" // metti qui la tua key

    @Test
    fun testFetchBlocking() {
        val result = MarketstackFetcher.fetchLatestCloseBlocking("ISP.MI", apiKey)
        when (result) {
            is FetchResult.Success -> println("SUCCESS -> ${result.date} : ${result.close}")
            is FetchResult.Error -> println("ERROR -> ${result.message} (code=${result.code})")
        }
    }
}
