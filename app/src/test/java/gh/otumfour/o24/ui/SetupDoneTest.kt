package gh.otumfour.o24.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
            DoneStep.CLOSE,
            SetupDone.onPrimary(last, SettingsPages.COUNT, ImeReadiness.READY),
        )
    }

    @Test
    fun launcherOpensSettingsHomeAfterSetup() {
        assertEquals(LaunchDestination.SETTINGS_HOME, SetupDone.launchDestination(onboarded = true, runAgain = false))
        assertEquals(LaunchDestination.ONBOARDING, SetupDone.launchDestination(onboarded = false, runAgain = false))
        assertEquals(LaunchDestination.ONBOARDING, SetupDone.launchDestination(onboarded = true, runAgain = true))
    }

    @Test
    fun thePickerIsAllowedOnTheDonePageAndWaitsForFocus() {
        assertEquals(PickerPresent.SHOW, ImePicker.present(hasFocus = true))
        assertEquals(PickerPresent.WAIT, ImePicker.present(hasFocus = false))
    }

    @Test
    fun theAppClosesOnlyWhenDoneIsWaitingAndTheKeyboardIsActive() {
        assertTrue(SetupDone.shouldClose(closeWhenActive = true, ImeReadiness.READY))
        assertFalse(SetupDone.shouldClose(closeWhenActive = true, ImeReadiness.NOT_ENABLED))
        assertFalse(SetupDone.shouldClose(closeWhenActive = true, ImeReadiness.NOT_DEFAULT))
        assertFalse(SetupDone.shouldClose(closeWhenActive = false, ImeReadiness.READY))
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
