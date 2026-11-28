package gh.otumfour.o24.ui

enum class ImeReadiness { NOT_ENABLED, NOT_DEFAULT, READY }

enum class DoneStep { ADVANCE, OPEN_INPUT_SETTINGS, SHOW_IME_PICKER, CLOSE }

enum class LaunchDestination { ONBOARDING, SETTINGS_HOME }

enum class PickerPresent { SHOW, WAIT }

/**
 * The keyboard list can be requested from any onboarding page, including Done.
 * Without window focus Android ignores the picker, so the request waits.
 */
object ImePicker {
    fun present(hasFocus: Boolean): PickerPresent =
        if (hasFocus) PickerPresent.SHOW else PickerPresent.WAIT
}

/**
 * Done never sends the pager back to page 0.
 * It enables the keyboard, asks for the default, or closes the app when Otumfour is already active.
 * A later launcher open goes to Settings Home, not the welcome steps, unless setup is run again.
 */
object SetupDone {
    fun readiness(enabled: Boolean, isDefault: Boolean): ImeReadiness = when {
        !enabled -> ImeReadiness.NOT_ENABLED
        !isDefault -> ImeReadiness.NOT_DEFAULT
        else -> ImeReadiness.READY
    }

    fun onPrimary(page: Int, pageCount: Int, readiness: ImeReadiness): DoneStep {
        if (pageCount <= 1) return done(readiness)
        if (page < pageCount - 1) return DoneStep.ADVANCE
        return done(readiness)
    }

    fun launchDestination(onboarded: Boolean, runAgain: Boolean): LaunchDestination = when {
        runAgain -> LaunchDestination.ONBOARDING
        onboarded -> LaunchDestination.SETTINGS_HOME
        else -> LaunchDestination.ONBOARDING
    }

    /** Close only after Done, and only once the keyboard is enabled and the default. */
    fun shouldClose(closeWhenActive: Boolean, readiness: ImeReadiness): Boolean =
        closeWhenActive && readiness == ImeReadiness.READY

    private fun done(readiness: ImeReadiness): DoneStep = when (readiness) {
        ImeReadiness.NOT_ENABLED -> DoneStep.OPEN_INPUT_SETTINGS
        ImeReadiness.NOT_DEFAULT -> DoneStep.SHOW_IME_PICKER
        ImeReadiness.READY -> DoneStep.CLOSE
    }
}
