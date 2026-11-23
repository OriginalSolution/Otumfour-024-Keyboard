package gh.otumfour.o24.suggest

import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max

data class SwipeSample(val x: Float, val y: Float, val time: Long)

data class SwipeKey(val letter: Char, val x: Float, val y: Float)

/**
 * Gesture typing. The finger path is matched to each word's ideal key centres
 * with dynamic time warping, plus a frequency prior and start/end key pruning.
 * This is an original SHARK2-style scorer. It does not include OpenBoard or HeliBoard code.
 */
object SwipeDecoder {
    private const val PAUSE_MS = 170L

    fun trace(samples: List<SwipeSample>, keys: List<SwipeKey>, keyWidth: Float): String {
        if (samples.isEmpty() || keys.isEmpty()) return ""
        val visits = ArrayList<Visit>()
        for (sample in samples) {
            val key = nearest(sample.x, sample.y, keys) ?: continue
            val last = visits.lastOrNull()
            if (last != null && last.letter == key.letter) {
                last.end = sample.time
                last.travel += hypot(sample.x - last.x, sample.y - last.y)
                last.x = sample.x
                last.y = sample.y
            } else {
                visits.add(Visit(key.letter, sample.time, sample.time, sample.x, sample.y, 0f))
            }
        }
        val out = StringBuilder()
        for (visit in visits) {
            out.append(visit.letter)
            val paused = visit.end - visit.start >= PAUSE_MS
            val looped = visit.travel > keyWidth * 0.85f
            if (paused || looped) out.append(visit.letter)
        }
        return out.toString()
    }

    fun rank(
        samples: List<SwipeSample>,
        keys: List<SwipeKey>,
        keyWidth: Float,
        words: List<Pair<String, Int>>,
        limit: Int = 5,
    ): List<String> {
        val traced = trace(samples, keys, keyWidth)
        if (traced.isEmpty() || keys.isEmpty()) return emptyList()
        val byLetter = keys.associateBy { it.letter }
        val start = Qwerty.neighbours(traced.first())
        val end = Qwerty.neighbours(traced.last())
        val minLen = (traced.length - 1).coerceAtLeast(1)
        val maxLen = traced.length + 2
        val pool = words.asSequence()
            .filter { (word, _) ->
                word.length in minLen..maxLen &&
                    word.first() in start &&
                    word.last() in end &&
                    word.all { it in byLetter }
            }
            .sortedByDescending { it.second }
            .take(280)
            .toList()
        if (pool.isEmpty()) return listOf(traced)
        val finger = resample(samples.map { it.x to it.y }, 28)
        val ranked = pool.map { (word, freq) ->
            val ideal = word.map { letter ->
                val key = byLetter.getValue(letter)
                key.x to key.y
            }
            val dtw = dtw(finger, ideal) / keyWidth.coerceAtLeast(1f)
            val exact = if (word == traced) 1.4 else if (word == traced.replace(Regex("(.)\\1"), "$1")) 0.35 else 0.0
            val score = -dtw + ln(freq.coerceAtLeast(1).toDouble()) * 0.18 + exact
            word to score
        }.sortedByDescending { it.second }
        val out = ArrayList<String>()
        for ((word, _) in ranked) {
            if (word !in out) out.add(word)
            if (out.size == limit) break
        }
        return if (out.isEmpty() && traced.length > 1) listOf(traced) else out
    }

    private fun nearest(x: Float, y: Float, keys: List<SwipeKey>): SwipeKey? {
        var best: SwipeKey? = null
        var bestD = Float.MAX_VALUE
        for (key in keys) {
            val d = hypot(x - key.x, y - key.y)
            if (d < bestD) {
                bestD = d
                best = key
            }
        }
        return best
    }

    private fun resample(points: List<Pair<Float, Float>>, count: Int): List<Pair<Float, Float>> {
        if (points.size < 2) return points
        val lengths = FloatArray(points.size)
        var total = 0f
        for (i in 1 until points.size) {
            total += hypot(points[i].first - points[i - 1].first, points[i].second - points[i - 1].second)
            lengths[i] = total
        }
        if (total < 1f) return List(count) { points.first() }
        val out = ArrayList<Pair<Float, Float>>(count)
        var index = 1
        for (step in 0 until count) {
            val target = total * step / (count - 1).toFloat()
            while (index < lengths.size - 1 && lengths[index] < target) index++
            val prev = lengths[index - 1]
            val span = (lengths[index] - prev).coerceAtLeast(0.001f)
            val t = ((target - prev) / span).coerceIn(0f, 1f)
            val a = points[index - 1]
            val b = points[index]
            out.add(a.first + (b.first - a.first) * t to a.second + (b.second - a.second) * t)
        }
        return out
    }

    private fun dtw(a: List<Pair<Float, Float>>, b: List<Pair<Float, Float>>): Float {
        val n = a.size
        val m = b.size
        val col = FloatArray(m + 1) { Float.MAX_VALUE / 4f }
        col[0] = 0f
        for (i in 1..n) {
            var previous = col[0]
            col[0] = Float.MAX_VALUE / 4f
            for (j in 1..m) {
                val cost = hypot(a[i - 1].first - b[j - 1].first, a[i - 1].second - b[j - 1].second)
                val best = minOf(col[j], col[j - 1], previous) + cost
                previous = col[j]
                col[j] = best
            }
        }
        return col[m] / max(n, m).toFloat()
    }

    private class Visit(
        val letter: Char,
        val start: Long,
        var end: Long,
        var x: Float,
        var y: Float,
        var travel: Float,
    )
}
