package gh.otumfour.o24.suggest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeDecoderTest {
    private val keys = qwerty()

    @Test
    fun helloPathWithAPauseOnLBecomesHello() {
        val samples = path(keys.getValue('h'), keys.getValue('e'), linger = keys.getValue('l'), end = keys.getValue('o'))
        val traced = SwipeDecoder.trace(samples, keys.values.toList(), 100f)
        assertEquals("hello", traced)
        val ranked = SwipeDecoder.rank(
            samples,
            keys.values.toList(),
            100f,
            listOf("hello" to 400, "hell" to 80, "help" to 60, "hellos" to 20, "the" to 500),
        )
        assertEquals("hello", ranked.first())
        assertTrue(ranked.size in 1..5)
    }

    @Test
    fun aShortMoveStaysASingleLetter() {
        val h = keys.getValue('h')
        val samples = listOf(
            SwipeSample(h.x, h.y, 0L),
            SwipeSample(h.x + 10f, h.y, 40L),
        )
        assertEquals("h", SwipeDecoder.trace(samples, keys.values.toList(), 100f))
    }

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
