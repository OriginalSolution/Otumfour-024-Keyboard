package gh.otumfour.o24.ime

import kotlin.math.max

/**
 * A short slip of the finger is still a tap.
 * Swipe typing starts only after the finger has travelled past this distance.
 */
object SwipeGesture {
    fun engagePx(averageKeyWidthPx: Float, density: Float): Float {
        val floor = 36f * density.coerceAtLeast(1f)
        val acrossKeys = averageKeyWidthPx * 0.9f
        return max(floor, acrossKeys)
    }
}
