package gh.otumfour.o24.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiClientTest {
    @Test
    fun endpointGainsCompletionsPath() {
        assertEquals(
            "https://example.com/v1/chat/completions",
            AiClient.chatUrl("https://example.com/v1"),
        )
        assertEquals(
            "https://example.com/v1/chat/completions",
            AiClient.chatUrl("https://example.com/v1/chat/completions/"),
        )
    }

    @Test
    fun stripsAWrappingQuote() {
        assertEquals("Akwaaba", AiClient.stripWrappingQuotes("\"Akwaaba\""))
        assertEquals("Akwaaba", AiClient.stripWrappingQuotes("Akwaaba"))
    }
}
