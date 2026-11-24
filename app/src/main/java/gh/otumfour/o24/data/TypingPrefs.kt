package gh.otumfour.o24.data

enum class KeyHeight(val storage: String) {
    SMALL("small"),
    NORMAL("normal"),
    LARGE("large"),
    EXTRA_LARGE("extra_large"),
    ;

    companion object {
        fun fromStorage(value: String?): KeyHeight =
            entries.firstOrNull { it.storage == value } ?: NORMAL
    }
}

enum class LetterSize(val storage: String) {
    NORMAL("normal"),
    LARGE("large"),
    EXTRA_LARGE("extra_large"),
    ;

    companion object {
        fun fromStorage(value: String?): LetterSize =
            entries.firstOrNull { it.storage == value } ?: NORMAL
    }
}
