package gh.otumfour.o24.suggest

import kotlin.math.abs
import kotlin.math.hypot

/**
 * QWERTY geometry for proximity-weighted edits.
 * Coordinates are in key steps: adjacent letters are about 1 apart.
 * The weighted edit is an original implementation of the same idea AOSP LatinIME
 * uses for nearby-key corrections (Apache-2.0). No AOSP source is copied.
 */
object Qwerty {
    private val pos: Map<Char, Pair<Float, Float>> = buildMap {
        fun row(letters: String, y: Float, x0: Float) {
            letters.forEachIndexed { index, ch -> put(ch, x0 + index to y) }
        }
        row("qwertyuiop", 0f, 0f)
        row("asdfghjkl", 1f, 0.5f)
        row("zxcvbnm", 2f, 1.5f)
    }

    fun distance(a: Char, b: Char): Float {
        val left = pos[a.lowercaseChar()] ?: return 4f
        val right = pos[b.lowercaseChar()] ?: return 4f
        return hypot(left.first - right.first, left.second - right.second)
    }

    fun neighbours(letter: Char): Set<Char> {
        val origin = pos[letter.lowercaseChar()] ?: return setOf(letter.lowercaseChar())
        val near = HashSet<Char>()
        near.add(letter.lowercaseChar())
        for ((ch, at) in pos) {
            if (hypot(origin.first - at.first, origin.second - at.second) <= 1.2f) near.add(ch)
        }
        return near
    }

    /** Weighted Damerau distance. Null when the edit is too large to autocorrect. */
    fun editCost(typed: String, candidate: String, max: Float = 1.05f): Float? {
        if (typed.isEmpty() || candidate.isEmpty()) return null
        if (abs(typed.length - candidate.length) > 2) return null
        val m = typed.length
        val n = candidate.length
        val grid = Array(m + 1) { FloatArray(n + 1) { max + 1f } }
        grid[0][0] = 0f
        for (i in 1..m) grid[i][0] = i * 0.8f
        for (j in 1..n) grid[0][j] = j * 0.8f
        for (i in 1..m) {
            var rowMin = max + 1f
            for (j in 1..n) {
                val sub = if (typed[i - 1] == candidate[j - 1]) 0f else substitution(typed[i - 1], candidate[j - 1])
                var value = minOf(grid[i - 1][j] + 0.8f, grid[i][j - 1] + 0.8f, grid[i - 1][j - 1] + sub)
                if (i > 1 && j > 1 && typed[i - 1] == candidate[j - 2] && typed[i - 2] == candidate[j - 1]) {
                    value = minOf(value, grid[i - 2][j - 2] + 0.55f)
                }
                grid[i][j] = value
                if (value < rowMin) rowMin = value
            }
            if (rowMin > max) return null
        }
        return grid[m][n].takeIf { it <= max }
    }

    private fun substitution(a: Char, b: Char): Float {
        val steps = distance(a, b)
        return when {
            steps <= 1.15f -> 0.45f
            steps <= 2.05f -> 0.85f
            else -> 1.3f
        }
    }
}
