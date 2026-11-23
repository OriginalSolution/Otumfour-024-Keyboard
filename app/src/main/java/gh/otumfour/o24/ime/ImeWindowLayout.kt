package gh.otumfour.o24.ime

/**
 * The input method window is already placed above the navigation bar.
 * Adding that inset to the keyboard height makes the keys jump about one
 * gesture-bar height when Android 15, or Samsung and Huawei gesture navigation,
 * reports the inset and then clears it.
 */
object ImeWindowLayout {
    fun bottomPaddingPx(reportedNavigationInsetPx: Int): Int = 0
}
