package gh.otumfour.o24.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Key caps stay opaque and meet WCAG AA (4.5:1) against their letter colour.
 * A photo or a bright cloth must not show through the label.
 */
object KeyCap {
    private const val MIN_ALPHA = 242
    const val MIN_CONTRAST = 4.5

    fun solid(color: Int): Int {
        if (color == 0) return 0
        val alpha = (color ushr 24) and 0xFF
        if (alpha >= MIN_ALPHA) return color
        return (0xFF shl 24) or (color and 0x00FFFFFF)
    }

    fun ensure(fill: Int, label: Int): Int {
        var color = solid(fill)
        if (color == 0 || label == 0) return color
        if (contrast(color, label) >= MIN_CONTRAST) return color
        val toward = if (luminance(label) >= luminance(color)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        repeat(12) {
            color = mix(color, toward, 0.4f)
            if (contrast(color, label) >= MIN_CONTRAST) return color
        }
        return toward
    }

    fun contrast(first: Int, second: Int): Double {
        val lighter = max(luminance(first), luminance(second))
        val darker = min(luminance(first), luminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun luminance(color: Int): Double {
        fun channel(value: Int): Double {
            val s = value / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val red = channel((color ushr 16) and 0xFF)
        val green = channel((color ushr 8) and 0xFF)
        val blue = channel(color and 0xFF)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    private fun mix(from: Int, to: Int, amount: Float): Int {
        fun channel(color: Int, shift: Int): Int = (color ushr shift) and 0xFF
        fun blend(start: Int, end: Int): Int = (start + (end - start) * amount).toInt().coerceIn(0, 255)
        val rgb = (blend(channel(from, 16), channel(to, 16)) shl 16) or
            (blend(channel(from, 8), channel(to, 8)) shl 8) or
            blend(channel(from, 0), channel(to, 0))
        return (0xFF shl 24) or rgb
    }
}
