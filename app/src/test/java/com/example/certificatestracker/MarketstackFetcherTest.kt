import kotlinx.coroutines.runBlocking
import org.junit.Test
import com.example.certificatestracker.MarketstackFetcher

class MarketstackFetcherSuspendTest {
    private val apiKey = "e1e60f41a11968b889595584e0a6c310"

    @Test
    fun testSuspendFetcher() = runBlocking {
        val result = MarketstackFetcher.fetchLatestClose("ISP.MI", apiKey)
        println(">>> SUSPEND RESULT = $result")
    }
}

