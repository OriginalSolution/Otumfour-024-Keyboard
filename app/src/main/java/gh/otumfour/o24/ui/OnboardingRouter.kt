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
}
