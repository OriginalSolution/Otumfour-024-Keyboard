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
    /** Dark veil over a photo. Soft is about two thirds, so the picture stays quiet. */
    fun scrimAlpha(strength: BackgroundStrength): Int = when (strength) {
        BackgroundStrength.OFF -> 255
        BackgroundStrength.SOFT -> 170
        BackgroundStrength.FULL -> 72
    }

    fun blur(strength: BackgroundStrength): Boolean = strength == BackgroundStrength.SOFT
}
