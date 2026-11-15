package gh.otumfour.o24.suggest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LexiconTest {
    private val lexicon = Lexicon.parse(
        """
        # comment
        the	100
        then	40
        ten	20
        they	30
        ghana	15
        kumasi	12
        ɛyɛ	8
        """.trimIndent(),
    )

    @Test
    fun transpositionBeatsAWeakerNeighbour() {
        assertEquals(1, Lexicon.damerau("teh", "the", 2))
        val top = lexicon.suggest("teh").first()
        assertEquals("the", top.word)
        assertEquals("the", Lexicon.autocorrect("teh", lexicon.suggest("teh")))
    }

    @Test
    fun exactWordIsNotReplaced() {
        assertNull(Lexicon.autocorrect("the", lexicon.suggest("the")))
    }

    @Test
    fun prefixCompletionsStayAvailable() {
        val words = lexicon.suggest("th").map { it.word }
        assertTrue(words.contains("the"))
        assertTrue(words.contains("then"))
        assertNull(Lexicon.autocorrect("th", lexicon.suggest("th")))
    }

    @Test
    fun ghanaianLetterSurvivesParsingAndCase() {
        assertTrue(lexicon.contains("ɛyɛ"))
        assertEquals("Ɛyɛ", Lexicon.matchCase("Ɛyɛ", "ɛyɛ"))
        assertEquals("GHANA", Lexicon.matchCase("GHANA", "ghana"))
    }

    @Test
    fun learnedWordsJoinTheList() {
        val words = lexicon.suggest("otum", mapOf("otumfuo" to 50)).map { it.word }
        assertTrue(words.contains("otumfuo"))
    }

    @Test
    fun distantWordsAreDropped() {
        assertNull(Lexicon.damerau("abcd", "wxyz", 2))
    }
}
