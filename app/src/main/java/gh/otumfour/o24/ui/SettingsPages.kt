package gh.otumfour.o24.ui

import kotlin.math.abs

/**
 * Settings is a pager. A smooth scroll across several unmeasured pages
 * overshoots onto About while the step title already says the page that was tapped.
 */
object SettingsPages {
    const val COUNT = 7
    const val HOME = 0
    const val THEMES = 1
    const val LANGUAGES = 2
    const val TYPING = 3
    const val AI = 4
    const val LEARNED = 5
    const val ABOUT = 6

    fun clamp(index: Int): Int = index.coerceIn(HOME, ABOUT)

    /** Only a neighbouring page is safe to animate. Anything further must jump. */
    fun useSmoothScroll(from: Int, to: Int): Boolean = abs(clamp(to) - clamp(from)) <= 1
}
