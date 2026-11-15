package gh.otumfour.o24.lang

enum class KeyboardLanguage(
    val code: String,
    val label: String,
    val shortLabel: String,
    val asset: String,
    val speechTag: String,
    val englishName: String,
) {
    ENGLISH("en", "English", "EN", "en.txt", "en-GH", "English"),
    ASANTE("tw-as", "Asante Twi", "Asante", "tw-as.txt", "en-GH", "Asante Twi"),
    AKUAPEM("tw-ak", "Akuapem Twi", "Akuapem", "tw-ak.txt", "en-GH", "Akuapem Twi"),
    FANTE("fat", "Fante", "Fante", "fat.txt", "en-GH", "Fante"),
    GA("gaa", "Ga", "Ga", "gaa.txt", "en-GH", "Ga"),
    EWE("ee", "Ewe", "Ewe", "ee.txt", "en-GH", "Ewe"),
    DAGBANI("dag", "Dagbani", "Dagbani", "dag.txt", "en-GH", "Dagbani"),
    DAGAARE("dga", "Dagaare", "Dagaare", "dga.txt", "en-GH", "Dagaare"),
    GONJA("gjn", "Gonja", "Gonja", "gjn.txt", "en-GH", "Gonja"),
    NZEMA("nzi", "Nzema", "Nzema", "nzi.txt", "en-GH", "Nzema"),
    KASEM("xsm", "Kasem", "Kasem", "xsm.txt", "en-GH", "Kasem"),
    HAUSA("ha", "Hausa", "Hausa", "ha.txt", "ha-GH", "Hausa"),
    ;

    /** Extra letter row. Capitals follow shift. Hausa uses the hooked Boko letters. */
    fun extraLetters(shifted: Boolean): List<String> {
        val lower = when (this) {
            ENGLISH -> emptyList()
            ASANTE, AKUAPEM, FANTE, NZEMA -> listOf("ɛ", "ɔ")
            GA, GONJA -> listOf("ɛ", "ɔ", "ŋ")
            EWE -> listOf("ɛ", "ɔ", "ɖ", "ƒ", "ɣ", "ŋ", "ʋ")
            DAGBANI -> listOf("ɛ", "ɔ", "ɣ", "ŋ", "ʒ")
            DAGAARE -> listOf("ɛ", "ɔ", "ɩ", "ʋ", "ŋ")
            KASEM -> listOf("ɛ", "ɔ", "ɩ", "ʋ", "ŋ")
            HAUSA -> listOf("ɓ", "ɗ", "ƙ", "ƴ")
        }
        return if (shifted) lower.map { it.uppercase() } else lower
    }

    fun alternate(letter: String): String? {
        val map = mapOf(
            "e" to "ɛ",
            "o" to "ɔ",
            "n" to "ŋ",
            "d" to "ɖ",
            "f" to "ƒ",
            "g" to "ɣ",
            "v" to "ʋ",
            "i" to "ɩ",
            "b" to "ɓ",
            "k" to "ƙ",
            "y" to "ƴ",
            "z" to "ʒ",
        )
        val base = letter.lowercase()
        val alt = map[base] ?: return null
        return if (letter.any { it.isLetter() && it.isUpperCase() }) alt.uppercase() else alt
    }

    companion object {
        fun fromCode(code: String?): KeyboardLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}
