package gh.otumfour.o24.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPagesTest {
    @Test
    fun everyCardOpensItsOwnPage() {
        val cards = listOf(
            SettingsPages.THEMES,
            SettingsPages.LANGUAGES,
            SettingsPages.TYPING,
            SettingsPages.AI,
            SettingsPages.LEARNED,
            SettingsPages.ABOUT,
        )
        cards.forEach { card ->
            assertEquals(card, SettingsPages.clamp(card))
        }
    }

    @Test
    fun typingCardDoesNotLandOnAbout() {
        val opened = SettingsPages.clamp(SettingsPages.TYPING)
        assertEquals(SettingsPages.TYPING, opened)
        assertNotEquals(SettingsPages.ABOUT, opened)
        assertFalse(SettingsPages.useSmoothScroll(SettingsPages.HOME, opened))
    }

    @Test
    fun aSingleStepMayAnimate() {
        assertTrue(SettingsPages.useSmoothScroll(SettingsPages.HOME, SettingsPages.THEMES))
        assertTrue(SettingsPages.useSmoothScroll(SettingsPages.TYPING, SettingsPages.LANGUAGES))
    }
}
