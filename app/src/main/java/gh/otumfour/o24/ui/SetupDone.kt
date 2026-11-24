package gh.otumfour.o24.ui

enum class ImeReadiness { NOT_ENABLED, NOT_DEFAULT, READY }

enum class DoneStep { ADVANCE, OPEN_INPUT_SETTINGS, SHOW_IME_PICKER, LAND_HOME }

/**
 * Done on the last settings page never sends the pager back to page 0.
 * It enables the keyboard, asks for the default, or lands on Home when both are done.
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

    private fun done(readiness: ImeReadiness): DoneStep = when (readiness) {
        ImeReadiness.NOT_ENABLED -> DoneStep.OPEN_INPUT_SETTINGS
        ImeReadiness.NOT_DEFAULT -> DoneStep.SHOW_IME_PICKER
        ImeReadiness.READY -> DoneStep.LAND_HOME
    }
}
