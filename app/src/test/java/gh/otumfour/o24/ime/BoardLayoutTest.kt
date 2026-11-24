package gh.otumfour.o24.ime

import gh.otumfour.o24.lang.KeyboardLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardLayoutTest {
    @Test
    fun topRowShowsDigitHintsAndKeepsSpecialLetters() {
        val rows = KeyboardModel.rows(BoardPage.LETTERS, KeyboardLanguage.ENGLISH, ShiftState.OFF, "Enter")
        val top = rows.first { line -> line.any { it.value == "q" } }
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), top.map { it.hint })
        val e = top.first { it.value == "e" }
        assertTrue(e.extras.contains("3"))
        assertTrue(e.extras.contains("ɛ"))
    }

    @Test
    fun letterBottomRowMatchesTheReferenceOrder() {
        val rows = KeyboardModel.rows(BoardPage.LETTERS, KeyboardLanguage.ENGLISH, ShiftState.OFF, "Search")
        val bottom = rows.last()
        assertEquals("?123", bottom.first().label)
        assertEquals(",", bottom[1].value)
        assertEquals(KeyAction.EMOJI, bottom[2].action)
        assertEquals(KeyAction.SPACE, bottom[3].action)
        assertEquals(".", bottom[4].value)
        assertEquals("_", bottom[4].alternate)
        assertTrue(bottom.last().round)
        assertEquals(KeyIcon.SEARCH, bottom.last().icon)
    }

    @Test
    fun symbolPagesAndNumpad() {
        val first = KeyboardModel.rows(BoardPage.SYMBOLS, KeyboardLanguage.ENGLISH, ShiftState.OFF, "Enter")
        assertEquals("=\\<", first[2].first().label)
        assertEquals(KeyAction.SYMBOL_PAGE, first[2].first().action)
        assertEquals("1234", first.last().first { it.action == KeyAction.NUMPAD }.label)
        val second = KeyboardModel.rows(BoardPage.SYMBOLS_MORE, KeyboardLanguage.ENGLISH, ShiftState.OFF, "Enter")
        assertEquals("~", second[0].first().value)
        assertEquals("\\", second[1].last().value)
        assertEquals("?123", second[2].first().label)
        assertTrue(second.last().any { it.value == "<" })
        assertTrue(second.last().any { it.value == ">" })
        val pad = KeyboardModel.rows(BoardPage.NUMPAD, KeyboardLanguage.ENGLISH, ShiftState.OFF, "Enter")
        assertEquals("1", pad[0].first().value)
        assertEquals("+", pad[1].last().value)
    }

    @Test
    fun gifMissingKeyHidesTheFeature() {
        val missing = GifReply.parse(200, """{"ok":false,"missingKey":"TENOR_API_KEY"}""")
        assertTrue(missing is GifReply.MissingKey)
        assertEquals("TENOR_API_KEY", (missing as GifReply.MissingKey).keyName)
        val ready = GifReply.parse(200, """{"ok":true,"gifs":[{"title":"Hi","preview":"https://example.com/p.gif","gif":"https://example.com/a.gif"}]}""")
        assertTrue(ready is GifReply.Ready)
        assertEquals("Hi", (ready as GifReply.Ready).hits.single().title)
    }

    @Test
    fun toolbarOrderKeepsUnknownSlots() {
        val parsed = ToolbarItem.parse("theme,grid")
        assertEquals(ToolbarItem.THEME, parsed.first())
        assertEquals(ToolbarItem.GRID, parsed[1])
        assertTrue(parsed.contains(ToolbarItem.AI))
    }
}