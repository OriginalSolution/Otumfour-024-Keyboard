package gh.otumfour.o24.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeWindowLayoutTest {
    @Test
    fun navigationInsetDoesNotChangeKeyboardHeight() {
        val hidden = ImeWindowLayout.bottomPaddingPx(0)
        val gestureBar = ImeWindowLayout.bottomPaddingPx(126)
        assertEquals(hidden, gestureBar)
        assertEquals(0, gestureBar)
    }
}
