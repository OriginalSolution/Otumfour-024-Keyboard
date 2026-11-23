package gh.otumfour.o24.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeMigrationTest {
    @Test
    fun nightAndAMissingThemeBecomeOtumfuoOnce() {
        assertEquals("otumfuo", ThemeMigration.storedAfterUpdate(null, alreadyMigrated = false))
        assertEquals("otumfuo", ThemeMigration.storedAfterUpdate("night", alreadyMigrated = false))
        assertEquals("kente", ThemeMigration.storedAfterUpdate("kente", alreadyMigrated = false))
    }

    @Test
    fun aLaterChoiceOfNightStays() {
        assertEquals("night", ThemeMigration.storedAfterUpdate("night", alreadyMigrated = true))
        assertEquals("otumfuo", ThemeMigration.storedAfterUpdate(null, alreadyMigrated = true))
    }
}
