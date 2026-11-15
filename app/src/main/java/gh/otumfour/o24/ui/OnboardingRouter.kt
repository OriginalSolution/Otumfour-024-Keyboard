package gh.otumfour.o24.ui

/**
 * Decides whether the launcher opens Settings, and which onboarding step to restore.
 * The onboarding flag itself is written with SharedPreferences.commit before navigation.
 */
object OnboardingRouter {
    const val PAGE_COUNT = 5

    fun showSettings(onboarded: Boolean): Boolean = onboarded

    /** Saved instance wins, then an explicit request, then the page stored on disk. */
    fun resumePage(stored: Int, requested: Int? = null, restored: Int? = null): Int {
        val raw = restored ?: requested ?: stored
        return raw.coerceIn(0, PAGE_COUNT - 1)
    }

    /**
     * Move only forward.
     * Enable (1) becomes Choose (2) once the keyboard is on.
     * Choose stays on Choose until Otumfour is the default, then goes to the theme step (3).
     * A failed re-check must not send the user back to Enable.
     */
    fun forwardStep(current: Int, enabled: Boolean, isDefault: Boolean, allowAuto: Boolean): Int {
        val page = current.coerceIn(0, PAGE_COUNT - 1)
        if (!allowAuto) return page
        return when {
            isDefault && page in ENABLE_PAGE..CHOOSE_PAGE -> THEME_PAGE
            enabled && page == ENABLE_PAGE -> CHOOSE_PAGE
            else -> page
        }
    }

    const val ENABLE_PAGE = 1
    const val CHOOSE_PAGE = 2
    const val THEME_PAGE = 3
}
