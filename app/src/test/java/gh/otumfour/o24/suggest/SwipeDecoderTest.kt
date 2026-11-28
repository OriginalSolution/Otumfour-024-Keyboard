package gh.otumfour.o24.suggest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class SwipeDecoderTest {
    private val keys = qwerty()
    private val keyList = keys.values.toList()
    private val words = listOf(
        "the" to 50000,
        "to" to 20000,
        "and" to 15000,
        "you" to 12000,
        "that" to 11000,
        "until" to 8000,
        "this" to 8000,
        "beautiful" to 7000,
        "with" to 6000,
        "have" to 5000,
        "there" to 4000,
        "they" to 4000,
        "then" to 3000,
        "from" to 3000,
        "about" to 2500,
        "good" to 2000,
        "would" to 2000,
        "too" to 1500,
        "going" to 900,
        "morning" to 800,
        "home" to 700,
        "help" to 600,
        "hello" to 400,
        "thanks" to 350,
        "ghana" to 200,
        "hell" to 100,
        "akwaaba" to 80,
    )
    private val known = words.map { it.first }.toSet()

    @Test
    fun aContinuousHelloPathBeatsFrequentDistractors() {
        assertTop("hello", glide("helo"))
    }

    @Test
    fun helloBeatsHelpEvenWhenHelpIsMuchMoreFrequent() {
        val lexicon = words.map { (word, freq) ->
            when (word) {
                "help" -> word to 666286
                "hello" -> word to 405534
                else -> word to freq
            }
        }
        val ranked = SwipeDecoder.rank(glide("helo"), keyList, 100f, lexicon)
        assertEquals("hello", ranked.firstOrNull())
    }

    @Test
    fun commonWordsWinTheirOwnPath() {
        assertTop("the", glide("the"))
        assertTop("and", glide("and"))
        assertTop("good", glide("god"))
        assertTop("thanks", glide("thanks"))
        assertTop("hello", glide("helo"))
    }

    @Test
    fun aContinuousThanksPathIsNotTheCrossedKeys() {
        assertTop("thanks", glide("thanks"))
    }

    @Test
    fun aContinuousThePathStaysThe() {
        assertTop("the", glide("the"))
    }

    @Test
    fun aContinuousGoodPathAllowsTheRepeatedO() {
        assertTop("good", glide("god"))
    }

    @Test
    fun aContinuousMorningPathMatchesMorning() {
        assertTop("morning", glide("morning"))
    }

    @Test
    fun aContinuousGhanaPathMatchesGhana() {
        assertTop("ghana", glide("ghana"))
    }

    @Test
    fun aContinuousAkwaabaPathMatchesAkwaaba() {
        assertTop("akwaaba", glide("akwaba"))
    }

    @Test
    fun aShortWordSurvivesADensePath() {
        assertTop("to", glide("to"))
    }

    @Test
    fun rankNeverReturnsRawTracedLetters() {
        val samples = glide("hello", seed = 7)
        val ranked = SwipeDecoder.rank(samples, keyList, 100f, words)
        assertTrue(ranked.isNotEmpty())
        assertTrue(ranked.all { it in known })
        val traced = SwipeDecoder.trace(samples, keyList, 100f)
        assertTrue(traced.length > "hello".length)
        assertTrue(traced !in ranked)
    }

    @Test
    fun helloPathWithAPauseOnLBecomesHello() {
        val samples = path(keys.getValue('h'), keys.getValue('e'), linger = keys.getValue('l'), end = keys.getValue('o'))
        assertEquals("hello", SwipeDecoder.trace(samples, keyList, 100f))
    }

    @Test
    fun aShortMoveStaysASingleLetter() {
        val h = keys.getValue('h')
        val samples = listOf(
            SwipeSample(h.x, h.y, 0L),
            SwipeSample(h.x + 10f, h.y, 40L),
        )
        assertEquals("h", SwipeDecoder.trace(samples, keyList, 100f))
    }

    private fun assertTop(expected: String, samples: List<SwipeSample>) {
        val ranked = SwipeDecoder.rank(samples, keyList, 100f, words)
        assertEquals(expected, ranked.firstOrNull())
        assertTrue(ranked.all { it in known })
    }

    /** Straight segments between key centres, with a sample every few pixels and fixed jitter. */
    private fun glide(letters: String, seed: Int = 3): List<SwipeSample> {
        val random = Random(seed.toLong())
        val centres = letters.map { keys.getValue(it) }
        val out = ArrayList<SwipeSample>()
        var time = 0L
        val step = 8f
        for (index in 0 until centres.size - 1) {
            val from = centres[index]
            val to = centres[index + 1]
            val distance = hypot(to.x - from.x, to.y - from.y).coerceAtLeast(1f)
            val count = (distance / step).toInt().coerceAtLeast(6)
            for (sample in 0 until count) {
                val t = sample / count.toFloat()
                val jitter = 7f
                out.add(
                    SwipeSample(
                        from.x + (to.x - from.x) * t + (random.nextFloat() - 0.5f) * jitter,
                        from.y + (to.y - from.y) * t + (random.nextFloat() - 0.5f) * jitter,
                        time,
                    ),
                )
                time += 12L
            }
        }
        val end = centres.last()
        out.add(SwipeSample(end.x, end.y, time))
        return out
    }

    private fun hypot(x: Float, y: Float) = kotlin.math.hypot(x, y)

    private fun path(start: SwipeKey, next: SwipeKey, linger: SwipeKey, end: SwipeKey): List<SwipeSample> {
        val out = ArrayList<SwipeSample>()
        var time = 0L
        fun dwell(key: SwipeKey, ms: Long) {
            var left = ms
            while (left > 0) {
                out.add(SwipeSample(key.x, key.y, time))
                time += 20L
                left -= 20L
            }
        }
        dwell(start, 40)
        dwell(next, 40)
        dwell(linger, 220)
        dwell(end, 40)
        return out
    }

    private fun qwerty(): Map<Char, SwipeKey> {
        val map = HashMap<Char, SwipeKey>()
        fun row(letters: String, y: Float, x0: Float) {
            letters.forEachIndexed { index, ch ->
                map[ch] = SwipeKey(ch, x0 + index * 100f, y)
            }
        }
        row("qwertyuiop", 0f, 0f)
        row("asdfghjkl", 100f, 30f)
        row("zxcvbnm", 200f, 80f)
        return map
    }
}
