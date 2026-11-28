package gh.otumfour.o24.suggest

import kotlin.math.hypot
import kotlin.math.ln

data class SwipeSample(val x: Float, val y: Float, val time: Long)

data class SwipeKey(val letter: Char, val x: Float, val y: Float)

/**
 * Gesture typing. The finger path and each word's ideal key-centre path are
 * resampled to the same number of points. Location distance and shape distance
 * (after the two paths are centred) are combined with a frequency prior.
 * The word must start and end near the finger. A raw trace of crossed keys is
 * never returned. This is an original SHARK2-style scorer.
 */
object SwipeDecoder {
    private const val PAUSE_MS = 170L
    private const val SAMPLES = 32
    private const val START_END_RADIUS = 1.5f
    private const val FREQUENCY_WEIGHT = 0.05
    private const val LOCATION_WEIGHT = 0.45
    private const val SHAPE_WEIGHT = 0.25
    private const val END_WEIGHT = 0.7
    private const val START_WEIGHT = 0.2

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
        if (samples.size < 2 || keys.isEmpty() || words.isEmpty()) return emptyList()
        val width = keyWidth.coerceAtLeast(1f)
        val byLetter = HashMap<Char, SwipeKey>(keys.size)
        for (key in keys) byLetter[key.letter] = key
        val origin = samples.first()
        val finish = samples.last()
        val finger = resample(samples.map { it.x to it.y }, SAMPLES)
        val radius = width * START_END_RADIUS
        val pool = words.asSequence()
            .filter { (word, _) ->
                word.length in 1..24 && word.all { it in byLetter }
            }
            .mapNotNull { (word, freq) ->
                val first = byLetter.getValue(word.first())
                val last = byLetter.getValue(word.last())
                val startGap = hypot(first.x - origin.x, first.y - origin.y)
                val endGap = hypot(last.x - finish.x, last.y - finish.y)
                if (startGap > radius || endGap > radius) return@mapNotNull null
                Candidate(word, freq, idealPath(word, byLetter), startGap, endGap)
            }
            .toList()
        if (pool.isEmpty()) return emptyList()
        val ranked = pool.map { candidate ->
            val template = resample(candidate.ideal, SAMPLES)
            val location = pointDistance(finger, template) / width
            val shape = pointDistance(centred(finger), centred(template)) / width
            val endCost = candidate.endGap / width
            val startCost = candidate.startGap / width
            val prior = ln(candidate.freq.coerceAtLeast(1).toDouble()) * FREQUENCY_WEIGHT
            // The last key is not averaged away. Ending on o must beat a more frequent word that ends on p.
            val score = -(LOCATION_WEIGHT * location + SHAPE_WEIGHT * shape + END_WEIGHT * endCost + START_WEIGHT * startCost) + prior
            candidate.word to score
        }.sortedByDescending { it.second }
        val out = ArrayList<String>(limit)
        for ((word, _) in ranked) {
            if (word !in out) out.add(word)
            if (out.size == limit) break
        }
        return out
    }

    private fun idealPath(word: String, byLetter: Map<Char, SwipeKey>): List<Pair<Float, Float>> =
        word.map { letter ->
            val key = byLetter.getValue(letter)
            key.x to key.y
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

    private fun centred(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        if (points.isEmpty()) return points
        var sx = 0.0
        var sy = 0.0
        for (point in points) {
            sx += point.first
            sy += point.second
        }
        val cx = (sx / points.size).toFloat()
        val cy = (sy / points.size).toFloat()
        return points.map { it.first - cx to it.second - cy }
    }

    private fun pointDistance(a: List<Pair<Float, Float>>, b: List<Pair<Float, Float>>): Float {
        val n = minOf(a.size, b.size)
        if (n == 0) return Float.MAX_VALUE
        var sum = 0.0
        for (i in 0 until n) {
            sum += hypot(a[i].first - b[i].first, a[i].second - b[i].second)
        }
        return (sum / n).toFloat()
    }

    private fun resample(points: List<Pair<Float, Float>>, count: Int): List<Pair<Float, Float>> {
        if (points.isEmpty()) return points
        if (points.size == 1 || count < 2) return List(count.coerceAtLeast(1)) { points.first() }
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

    private data class Candidate(
        val word: String,
        val freq: Int,
        val ideal: List<Pair<Float, Float>>,
        val startGap: Float,
        val endGap: Float,
    )

    private class Visit(
        val letter: Char,
        val start: Long,
        var end: Long,
        var x: Float,
        var y: Float,
        var travel: Float,
    )
}
