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

    @Test
    fun readsProxyText() {
        assertEquals(
            "Mema wo akye, wo ho te sɛn?",
            AiClient.jsonStringField("""{"text":"Mema wo akye, wo ho te sɛn?"}""", "text"),
        )
        assertEquals("missing key", AiClient.jsonStringField("""{"error":"missing key"}""", "error"))
    }

    @Test
    fun rejectsTextOverTheLimit() {
        assertEquals(null, AiClient.limit("a".repeat(AiClient.MAX_CHARS + 1)))
        assertEquals("hello", AiClient.limit("hello"))
    }
}
