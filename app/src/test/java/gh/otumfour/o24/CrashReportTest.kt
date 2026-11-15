package gh.otumfour.o24

import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {
    @Test
    fun reportIncludesTheStack() {
        val error = IllegalArgumentException("contentTopInsets too large")
        val text = CrashReport.textOf(error)
        assertTrue(text.contains("contentTopInsets too large"))
        assertTrue(text.contains("IllegalArgumentException"))
        assertTrue(text.contains("O24 Keyboard"))
    }
}
