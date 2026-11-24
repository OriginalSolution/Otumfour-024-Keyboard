package gh.otumfour.o24.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeWindowLayoutTest {
    @Test
    fun paddingIsTheLargerOfNavBarAndGestureInset() {
        assertEquals(0, ImeWindowLayout.bottomPaddingPx(0, 0))
        assertEquals(48, ImeWindowLayout.bottomPaddingPx(48, 24))
        assertEquals(126, ImeWindowLayout.bottomPaddingPx(84, 126))
        assertEquals(140, ImeWindowLayout.bottomPaddingPx(140, 140))
    }

    @Test
    fun aNegativeInsetDoesNotPullTheKeysDown() {
        assertEquals(0, ImeWindowLayout.bottomPaddingPx(-8, 0))
        assertEquals(20, ImeWindowLayout.bottomPaddingPx(-8, 20))
    }

    @Test
    fun threeButtonAndGestureBarsBothLiftTheKeys() {
        val threeButton = ImeWindowLayout.bottomPaddingPx(navigationBarsBottomPx = 144, mandatorySystemGesturesBottomPx = 48)
        val gesture = ImeWindowLayout.bottomPaddingPx(navigationBarsBottomPx = 63, mandatorySystemGesturesBottomPx = 84)
        assertTrue(threeButton >= 144)
        assertTrue(gesture >= 84)
    }
}
