package gh.otumfour.o24.ime

import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.suggest.WordSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnderscoreKeyTest {
    @Test
    fun underscoreSitsBesideHyphenOnTheSymbolPage() {
        val rows = KeyboardModel.rows(BoardPage.SYMBOLS, KeyboardLanguage.ENGLISH, ShiftState.OFF, "Enter")
        val symbolRow = rows.first { line -> line.any { it.value == "-" } }
        assertEquals(listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"), symbolRow.map { it.value })
        assertEquals("_", symbolRow.first { it.value == "-" }.alternate)
        assertTrue(rows.flatten().any { it.value == "_" })
    }

    @Test
    fun periodAndHyphenLongPressInsertUnderscore() {
        val letters = KeyboardModel.rows(BoardPage.LETTERS, KeyboardLanguage.ENGLISH, ShiftState.OFF, "Enter")
        val period = letters.flatten().first { it.value == "." }
        assertEquals("_", period.alternate)
        assertEquals("_", WordSpan.insertion(period.alternate!!))
        assertFalse(WordSpan.insertion("_").contains(' '))
    }

    @Test
    fun usernameAndEmailStayIntact() {
        assertEquals("joe_k", WordSpan.ending("send joe_k"))
        assertEquals("joe_k@gmail.com", WordSpan.ending("joe_k@gmail.com"))
        assertTrue(WordSpan.leaveAsTyped("joe_k"))
        assertTrue(WordSpan.leaveAsTyped("joe_k@gmail.com"))
        assertFalse(WordSpan.capitaliseAfter('.', "joe_k@gmail"))
        assertTrue(WordSpan.capitaliseAfter('.', "hello"))
        assertNull(gh.otumfour.o24.suggest.Autocorrect.choose("joe_k", emptyList(), known = false))
        assertNull(gh.otumfour.o24.suggest.Autocorrect.choose("joe_k@gmail.com", emptyList(), known = false))
    }
}
