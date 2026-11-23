package gh.otumfour.o24.theme

/**
 * v1.0.8 shipped Night as the default, so the photographs never appeared.
 * The first read after 1.0.9 moves that default, and an unset theme, to Otumfuo.
 * A theme the person chose themselves is left alone, including Night if they pick it later.
 */
object ThemeMigration {
    const val NIGHT = "night"
    const val OTUMFUO = "otumfuo"

    fun storedAfterUpdate(stored: String?, alreadyMigrated: Boolean): String {
        if (alreadyMigrated) return stored?.takeIf { it.isNotBlank() } ?: OTUMFUO
        if (stored.isNullOrBlank() || stored == NIGHT) return OTUMFUO
        return stored
    }
}
