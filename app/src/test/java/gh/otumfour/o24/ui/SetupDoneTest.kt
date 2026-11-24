package gh.otumfour.o24.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SetupDoneTest {
    @Test
    fun doneOnTheLastPageNeverRestartsThePager() {
        val last = SettingsPages.ABOUT
        ImeReadiness.entries.forEach { readiness ->
            val step = SetupDone.onPrimary(last, SettingsPages.COUNT, readiness)
            assertNotEquals(DoneStep.ADVANCE, step)
        }
        assertEquals(
            DoneStep.OPEN_INPUT_SETTINGS,
            SetupDone.onPrimary(last, SettingsPages.COUNT, ImeReadiness.NOT_ENABLED),
        )
        assertEquals(
            DoneStep.SHOW_IME_PICKER,
            SetupDone.onPrimary(last, SettingsPages.COUNT, ImeReadiness.NOT_DEFAULT),
        )
        assertEquals(
            DoneStep.LAND_HOME,
            SetupDone.onPrimary(last, SettingsPages.COUNT, ImeReadiness.READY),
        )
    }

    @Test
    fun earlierPagesStillAdvance() {
        assertEquals(
            DoneStep.ADVANCE,
            SetupDone.onPrimary(SettingsPages.HOME, SettingsPages.COUNT, ImeReadiness.NOT_ENABLED),
        )
        assertEquals(
            DoneStep.ADVANCE,
            SetupDone.onPrimary(SettingsPages.TYPING, SettingsPages.COUNT, ImeReadiness.READY),
        )
    }
}
