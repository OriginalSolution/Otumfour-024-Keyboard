package gh.otumfour.o24.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeMatchTest {
    private val pkg = "gh.otumfour.o24"
    private val cls = "gh.otumfour.o24.ime.O24InputMethodService"

    @Test
    fun matchesBothComponentForms() {
        assertTrue(ImeMatch.isOurs("gh.otumfour.o24/.ime.O24InputMethodService", pkg, cls))
        assertTrue(ImeMatch.isOurs("gh.otumfour.o24/gh.otumfour.o24.ime.O24InputMethodService", pkg, cls))
    }

    @Test
    fun matchesSubtypeSuffix() {
        assertTrue(ImeMatch.isOurs("gh.otumfour.o24/.ime.O24InputMethodService;en", pkg, cls))
    }

    @Test
    fun ignoresOtherKeyboards() {
        assertFalse(ImeMatch.isOurs("com.google.android.inputmethod.latin/.LatinIME", pkg, cls))
        assertFalse(ImeMatch.isOurs(null, pkg, cls))
        assertFalse(ImeMatch.isOurs("", pkg, cls))
    }

    @Test
    fun findsO24InTheEnabledList() {
        val list = "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME:" +
            "gh.otumfour.o24/.ime.O24InputMethodService"
        assertTrue(ImeMatch.listContains(list, pkg, cls))
        assertFalse(ImeMatch.listContains("com.google.android.inputmethod.latin/.LatinIME", pkg, cls))
    }
}
