package gh.otumfour.o24.ime

import gh.otumfour.o24.data.KeyHeight
import gh.otumfour.o24.data.LetterSize

/**
 * Phone letters are about 23sp. Tablets and an unfolded fold step up.
 * The caller applies the system font scale by measuring in sp.
 */
object KeyMetrics {
    fun letterSp(size: LetterSize, widthDp: Int): Float {
        val base = when (size) {
            LetterSize.NORMAL -> 23f
            LetterSize.LARGE -> 27f
            LetterSize.EXTRA_LARGE -> 31f
        }
        val wider = when {
            widthDp >= 840 -> 5f
            widthDp >= 600 -> 3f
            else -> 0f
        }
        return base + wider
    }

    /** Row height in dp. Normal is a little taller than the old 42dp keys. */
    fun rowHeightDp(height: KeyHeight, widthDp: Int, narrow: Boolean): Float {
        val base = when (height) {
            KeyHeight.SMALL -> 44f
            KeyHeight.NORMAL -> 52f
            KeyHeight.LARGE -> 60f
            KeyHeight.EXTRA_LARGE -> 68f
        }
        val wider = when {
            widthDp >= 840 -> 6f
            widthDp >= 600 -> 4f
            else -> 0f
        }
        val raw = base + wider
        return if (narrow) raw - 4f else raw
    }
}
