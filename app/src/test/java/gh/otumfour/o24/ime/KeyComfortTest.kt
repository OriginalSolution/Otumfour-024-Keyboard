package gh.otumfour.o24.ime

import gh.otumfour.o24.data.KeyHeight
import gh.otumfour.o24.data.LetterSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyComfortTest {
    @Test
    fun phoneLettersAreAbout23spAndGrowOnFoldAndTablet() {
        assertEquals(23f, KeyMetrics.letterSp(LetterSize.NORMAL, widthDp = 392))
        assertEquals(23f, KeyMetrics.letterSp(LetterSize.NORMAL, widthDp = 320))
        assertTrue(KeyMetrics.letterSp(LetterSize.NORMAL, widthDp = 673) >= 26f)
        assertTrue(KeyMetrics.letterSp(LetterSize.NORMAL, widthDp = 840) >= 28f)
        assertTrue(KeyMetrics.letterSp(LetterSize.EXTRA_LARGE, 840) > KeyMetrics.letterSp(LetterSize.NORMAL, 840))
    }

    @Test
    fun normalKeysAreTallerThanTheOldPhoneRow() {
        val phone = KeyMetrics.rowHeightDp(KeyHeight.NORMAL, widthDp = 392, narrow = false)
        assertTrue(phone > 48f)
        assertTrue(KeyMetrics.rowHeightDp(KeyHeight.SMALL, 392, false) < phone)
        assertTrue(KeyMetrics.rowHeightDp(KeyHeight.EXTRA_LARGE, 392, false) > phone)
        assertTrue(KeyMetrics.rowHeightDp(KeyHeight.NORMAL, 840, false) > phone)
        assertTrue(KeyMetrics.rowHeightDp(KeyHeight.NORMAL, 320, narrow = true) < phone)
    }

    @Test
    fun aTapInTheGapHitsTheNearestKey() {
        val left = KeyHits.Box(0, 0f, 0f, 40f, 50f)
        val right = KeyHits.Box(1, 48f, 0f, 88f, 50f)
        assertEquals(0, KeyHits.nearest(20f, 25f, listOf(left, right)))
        assertEquals(0, KeyHits.nearest(42f, 25f, listOf(left, right)))
        assertEquals(1, KeyHits.nearest(46f, 25f, listOf(left, right)))
        assertEquals(1, KeyHits.nearest(70f, 10f, listOf(left, right)))
    }

    @Test
    fun aShortDriftStaysATap() {
        val travel = SwipeGesture.engagePx(averageKeyWidthPx = 80f, density = 2.5f)
        assertTrue(travel >= 36f * 2.5f)
        assertTrue(12f * 2.5f < travel)
    }
}
