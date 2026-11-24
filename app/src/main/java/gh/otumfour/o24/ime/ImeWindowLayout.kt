package gh.otumfour.o24.ime

/**
 * Do not add the navigation-bar inset to the keyboard height.
 *
 * API 36 forces edge-to-edge and ignores [androidx.core.view.WindowCompat.setDecorFitsSystemWindows].
 * The inset still flickers on gesture navigation, including Samsung and Huawei, and on a fold.
 * The input-method window is positioned by the system. Growing the keyboard by that inset
 * moves the keys by about one gesture bar. [bottomPaddingPx] stays zero so the height is stable
 * on phones, tablets, and the inner Fold screen.
 */
object ImeWindowLayout {
    fun bottomPaddingPx(reportedNavigationInsetPx: Int): Int = 0
}
