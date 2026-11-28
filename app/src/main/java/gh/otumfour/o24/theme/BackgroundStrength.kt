package gh.otumfour.o24.theme

/** How strongly a photograph shows behind the keys. Soft is the default. */
enum class BackgroundStrength(val storage: String) {
    OFF("off"),
    SOFT("soft"),
    FULL("full"),
    ;

    companion object {
        fun fromStorage(value: String?): BackgroundStrength =
            entries.firstOrNull { it.storage == value } ?: SOFT
    }
}

object BackdropPaint {
    /**
     * How strongly the picture itself is drawn, before a thin dim.
     * Soft stays recognisable and light. Full is clearer. Off draws no picture.
     */
    fun pictureAlpha(strength: BackgroundStrength): Int = when (strength) {
        BackgroundStrength.OFF -> 0
        BackgroundStrength.SOFT -> 132
        BackgroundStrength.FULL -> 210
    }

    /** A light dim so the picture never competes with the letters. */
    fun scrimAlpha(strength: BackgroundStrength): Int = when (strength) {
        BackgroundStrength.OFF -> 255
        BackgroundStrength.SOFT -> 56
        BackgroundStrength.FULL -> 28
    }

    fun blur(strength: BackgroundStrength): Boolean = strength == BackgroundStrength.SOFT
}
