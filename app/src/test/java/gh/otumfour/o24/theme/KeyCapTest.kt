package gh.otumfour.o24.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyCapTest {
    @Test
    fun translucentCapsBecomeOpaque() {
        val photoCap = argb(150, 26, 31, 42)
        val solid = KeyCap.solid(photoCap)
        assertEquals(255, (solid ushr 24) and 0xFF)
        assertTrue(KeyCap.contrast(solid, 0xFFF4F6FB.toInt()) >= KeyCap.MIN_CONTRAST)
    }

    @Test
    fun everySampleCapMeetsAa() {
        val pairs = listOf(
            0xFF1A1F2A.toInt() to 0xFFF4F6FB.toInt(),
            0xFFFFFFFF.toInt() to 0xFF12151C.toInt(),
            0xFF111111.toInt() to 0xFFF3D37A.toInt(),
            0xFF8E1A2B.toInt() to 0xFFFFFFFF.toInt(),
            argb(168, 0x1A, 0x14, 0x0C) to 0xFFFFFFFF.toInt(),
        )
        pairs.forEach { (fill, label) ->
            val cap = KeyCap.ensure(fill, label)
            assertEquals(255, (cap ushr 24) and 0xFF)
            assertTrue(KeyCap.contrast(cap, label) >= KeyCap.MIN_CONTRAST)
        }
    }

    @Test
    fun softScrimCoversMostOfThePhoto() {
        val soft = BackdropPaint.scrimAlpha(BackgroundStrength.SOFT)
        assertTrue(soft in 153..191)
        assertTrue(BackdropPaint.scrimAlpha(BackgroundStrength.FULL) < soft)
        assertEquals(255, BackdropPaint.scrimAlpha(BackgroundStrength.OFF))
        assertTrue(BackdropPaint.blur(BackgroundStrength.SOFT))
        assertEquals(BackgroundStrength.SOFT, BackgroundStrength.fromStorage(null))
    }

    private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
