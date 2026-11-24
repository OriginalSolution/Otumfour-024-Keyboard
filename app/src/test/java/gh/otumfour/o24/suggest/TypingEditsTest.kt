package gh.otumfour.o24.suggest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypingEditsTest {
    @Test
    fun secondSpaceAfterAWordBecomesAStop() {
        assertTrue(TypingEdits.doubleSpace("hello "))
        assertFalse(TypingEdits.doubleSpace("hello. "))
        assertFalse(TypingEdits.doubleSpace("hello"))
        assertFalse(TypingEdits.doubleSpace("joe_"))
        assertFalse(TypingEdits.doubleSpace("_"))
    }

    @Test
    fun aCapitalMidSentenceIsAName() {
        assertTrue(TypingEdits.capitalisedName("Kojo", "See you "))
        assertFalse(TypingEdits.capitalisedName("Teh", ""))
        assertFalse(TypingEdits.capitalisedName("Teh", "Done. "))
        assertTrue(TypingEdits.capitalisedName("McKay", "Hello "))
    }
}
