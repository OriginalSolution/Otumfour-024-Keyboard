package gh.otumfour.o24.ime

import kotlin.math.max

/**
 * Bottom padding that lifts the keys clear of the system navigation bar.
 *
 * API 36 draws the input method edge-to-edge, so the nav-bar icons sit on top of
 * the bottom row unless the input view is padded. The padding is the larger of
 * [navigationBars] and [mandatorySystemGestures]. It is applied to the input view
 * only: the keyboard background keeps painting through that strip, and the key
 * rows are not grown by the inset a second time.
 */
object ImeWindowLayout {
    fun bottomPaddingPx(navigationBarsBottomPx: Int, mandatorySystemGesturesBottomPx: Int = 0): Int {
        val bars = navigationBarsBottomPx.coerceAtLeast(0)
        val gestures = mandatorySystemGesturesBottomPx.coerceAtLeast(0)
        return max(bars, gestures)
    }
}
