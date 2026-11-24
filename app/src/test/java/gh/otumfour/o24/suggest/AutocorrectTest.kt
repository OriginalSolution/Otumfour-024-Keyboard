package gh.otumfour.o24.suggest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutocorrectTest {
    private val lexicon = Lexicon.parse(
        """
        the	100
        then	40
        ten	20
        they	30
        ghana	15
        ɛyɛ	12
        akwaaba	20
        """.trimIndent(),
    )

    @Test
    fun replacesACloseFrequentTypo() {
        assertEquals("the", Autocorrect.choose("teh", lexicon.suggest("teh"), known = false))
    }

    @Test
    fun aTypoAutocorrectWouldReplaceIsNotLearned() {
        val suggestions = lexicon.suggest("teh")
        assertEquals("the", Autocorrect.choose("teh", suggestions, known = false))
        assertFalse(Autocorrect.shouldLearn("teh", suggestions))
        assertTrue(Autocorrect.shouldLearn("akwaaba", lexicon.suggest("akwaaba")))
    }

    @Test
    fun keepsAKnownWordInAnyDictionary() {
        assertNull(Autocorrect.choose("the", lexicon.suggest("the"), known = true))
        assertNull(Autocorrect.choose("akwaaba", lexicon.suggest("akwaaba"), known = true))
    }

    @Test
    fun keepsLearnedWordsByMarkingThemKnown() {
        assertNull(Autocorrect.choose("otumfuo", emptyList(), known = true))
    }

    @Test
    fun doesNotCorrectAgainAfterARevert() {
        assertNull(Autocorrect.choose("teh", lexicon.suggest("teh"), known = false, rejected = setOf("teh")))
    }

    @Test
    fun skipsUrlsEmailsNumbersHandlesAllCapsAndSpecialLetters() {
        assertTrue(Autocorrect.blocked("https://o24.app"))
        assertTrue(Autocorrect.blocked("a@b.com"))
        assertTrue(Autocorrect.blocked("room24"))
        assertTrue(Autocorrect.blocked("@otumfuo"))
        assertTrue(Autocorrect.blocked("NASA"))
        assertTrue(Autocorrect.blocked("joe_k"))
        assertTrue(Autocorrect.blocked("joe_k@gmail.com"))
        assertNull(Autocorrect.choose("joe_k", lexicon.suggest("joe_k"), known = false))
        assertNull(Autocorrect.choose("joe_k@gmail.com", emptyList(), known = false))
        assertTrue(Autocorrect.blocked("ɛyɛ"))
        assertNull(Autocorrect.choose("ɛyɛ", lexicon.suggest("ɛyɛ"), known = false))
    }

    @Test
    fun needsASmallEditAndEnoughFrequency() {
        val rare = listOf(Suggestion("then", 1, 2, false))
        assertNull(Autocorrect.choose("thn", rare, known = false))
        assertNull(Autocorrect.choose("th", lexicon.suggest("th"), known = false))
    }

    @Test
    fun typedWordIsTheFirstSuggestion() {
        val shown = Autocorrect.display("teh", lexicon.suggest("teh"))
        assertEquals("teh", shown[0]?.word)
        assertEquals("the", shown[1]?.word)
    }
}
