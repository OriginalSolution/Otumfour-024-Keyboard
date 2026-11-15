package gh.otumfour.o24.serwaa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerwaaPersonaTest {
    @Test
    fun greetingUsesTheTraffiAlertVoiceName() {
        assertEquals("Hi, I'm Sirwaah. Ama, O24 Keyboard is ready.", SerwaaPersona.greeting("Ama", ""))
        assertEquals("Hello Ama", SerwaaPersona.greeting("Ama", "Hello {name}"))
        assertTrue(SerwaaPersona.systemPrompt("Ama").contains("Sirwaah"))
        assertTrue(SerwaaPersona.systemPrompt("").contains(SerwaaPersona.INTRO))
    }

    @Test
    fun voiceFamilyMatchesTraffiAlert() {
        assertEquals("en-gb-x-gba", SerwaaPersona.voiceFamily("en-gb-x-gba-local"))
        assertEquals("en-gb-x-gba", SerwaaPersona.voiceFamily("en-gb-x-gba-network"))
    }
}
