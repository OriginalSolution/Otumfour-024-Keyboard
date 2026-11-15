package gh.otumfour.o24.suggest

/**
 * Conservative correction. A typed word that is already known is kept.
 * A replacement happens only for a short edit of one step, with a frequent candidate.
 */
object Autocorrect {
    private const val MIN_FREQUENCY = 8
    private val special = "ɛɔŋɖƒɣʋɩʒɓɗƙƴƐƆŊƉƑƔƲƖƷƁƊƘƳ".toSet()

    fun blocked(typed: String): Boolean {
        if (typed.length < 3) return true
        if (typed.any { it.isDigit() || it in "@/:_" }) return true
        if (typed.contains('.') || typed.contains('-')) return true
        val letters = typed.filter { it.isLetter() }
        if (letters.length >= 2 && letters.all { it.isUpperCase() }) return true
        if (typed.any { it in special }) return true
        return false
    }

    fun choose(
        typed: String,
        suggestions: List<Suggestion>,
        known: Boolean,
        rejected: Set<String> = emptySet(),
    ): String? {
        val lower = typed.lowercase()
        if (known || lower in rejected || blocked(typed)) return null
        if (suggestions.any { it.distance == 0 && !it.completion && it.word == lower }) return null
        val best = suggestions.firstOrNull { suggestion ->
            !suggestion.completion && suggestion.distance == 1 && suggestion.word != lower && suggestion.frequency >= MIN_FREQUENCY
        } ?: return null
        return best.word
    }

    /** The typed word is always the first candidate so it can be kept. */
    fun display(typed: String, suggestions: List<Suggestion>): List<Suggestion?> {
        val lower = typed.lowercase()
        val typedSuggestion = Suggestion(lower, 0, 0, false)
        val rest = suggestions.filter { it.word != lower }
        return listOf(typedSuggestion, rest.getOrNull(0), rest.getOrNull(1))
    }
}
