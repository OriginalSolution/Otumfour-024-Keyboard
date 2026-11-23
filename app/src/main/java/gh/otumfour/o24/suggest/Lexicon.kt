package gh.otumfour.o24.suggest

data class Suggestion(
    val word: String,
    val distance: Int,
    val frequency: Int,
    val completion: Boolean,
)

/**
 * In-memory word list. Lines are `word` or `word<tab>frequency`. `#` starts a comment.
 * Learned words are merged at query time so a bigger asset file is the only change needed
 * to extend a language.
 */
class Lexicon(entries: List<Pair<String, Int>>) {
    private val words: List<Entry> = entries
        .map { (w, f) -> Entry(w.lowercase(), f.coerceAtLeast(1)) }
        .groupBy { it.word }
        .map { (w, group) -> Entry(w, group.sumOf { it.frequency }) }
        .sortedByDescending { it.frequency }

    fun isEmpty(): Boolean = words.isEmpty()

    fun contains(raw: String): Boolean = words.any { it.word == raw.lowercase() }

    fun entries(): List<Pair<String, Int>> = words.map { it.word to it.frequency }

    fun suggest(raw: String, learned: Map<String, Int> = emptyMap(), limit: Int = 8): List<Suggestion> {
        val typed = raw.lowercase()
        if (typed.isBlank()) return emptyList()
        val pool = HashMap<String, Int>()
        for (entry in words) pool[entry.word] = entry.frequency
        for ((word, freq) in learned) {
            val key = word.lowercase()
            if (key.isBlank()) continue
            pool[key] = (pool[key] ?: 0) + freq.coerceAtLeast(1)
        }
        val scored = ArrayList<Suggestion>(32)
        for ((word, freq) in pool) {
            val completion = word.startsWith(typed) && word != typed
            val distance = when {
                word == typed -> 0
                completion -> 0
                else -> damerau(typed, word, MAX_DISTANCE) ?: continue
            }
            if (!completion && word != typed && distance > MAX_DISTANCE) continue
            scored.add(Suggestion(word, distance, freq, completion))
        }
        return scored
            .sortedWith(
                compareByDescending<Suggestion> { score(typed, it) }
                    .thenByDescending { it.frequency }
                    .thenBy { it.word },
            )
            .take(limit)
    }

    companion object {
        const val MAX_DISTANCE = 2

        fun parse(text: String): Lexicon {
            val entries = ArrayList<Pair<String, Int>>()
            for (line in text.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val parts = trimmed.split('\t', limit = 2)
                val word = parts[0].trim()
                if (word.isEmpty()) continue
                val freq = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
                entries.add(word to freq)
            }
            return Lexicon(entries)
        }

        fun score(typed: String, suggestion: Suggestion): Double {
            val base = suggestion.frequency.toDouble() / (1.0 + suggestion.distance * suggestion.distance)
            val exact = if (suggestion.word == typed) 4.0 else 1.0
            val completion = if (suggestion.completion) suggestion.frequency * 0.2 else 0.0
            return base * exact + completion
        }

        /** Word to commit when space is pressed, or null to keep what the user typed. */
        fun autocorrect(typed: String, suggestions: List<Suggestion>, known: Boolean = false, rejected: Set<String> = emptySet()): String? =
            Autocorrect.choose(typed, suggestions, known, rejected)

        fun matchCase(typed: String, suggestion: String): String {
            val letters = typed.filter { it.isLetter() }
            if (letters.isNotEmpty() && letters.all { it.isUpperCase() }) return suggestion.uppercase()
            val first = typed.firstOrNull { it.isLetter() }
            if (first != null && first.isUpperCase()) {
                return suggestion.replaceFirstChar { ch -> ch.titlecase() }
            }
            return suggestion
        }

        fun damerau(a: String, b: String, max: Int): Int? {
            if (kotlin.math.abs(a.length - b.length) > max) return null
            val m = a.length
            val n = b.length
            val grid = Array(m + 1) { IntArray(n + 1) }
            for (i in 0..m) grid[i][0] = i
            for (j in 0..n) grid[0][j] = j
            for (i in 1..m) {
                var rowMin = max + 1
                for (j in 1..n) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    var value = minOf(grid[i - 1][j] + 1, grid[i][j - 1] + 1, grid[i - 1][j - 1] + cost)
                    if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                        value = minOf(value, grid[i - 2][j - 2] + 1)
                    }
                    grid[i][j] = value
                    if (value < rowMin) rowMin = value
                }
                if (rowMin > max) return null
            }
            return grid[m][n].takeIf { it <= max }
        }
    }

    private data class Entry(val word: String, val frequency: Int)
}
