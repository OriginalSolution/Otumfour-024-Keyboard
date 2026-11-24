package gh.otumfour.o24.ime

import kotlin.math.hypot

/** A tap in the gap between caps belongs to the nearest key. */
object KeyHits {
    data class Box(
        val index: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    )

    fun nearest(x: Float, y: Float, boxes: List<Box>): Int? {
        var best = -1
        var bestDistance = Float.POSITIVE_INFINITY
        for (box in boxes) {
            val dx = when {
                x < box.left -> box.left - x
                x > box.right -> x - box.right
                else -> 0f
            }
            val dy = when {
                y < box.top -> box.top - y
                y > box.bottom -> y - box.bottom
                else -> 0f
            }
            val distance = hypot(dx, dy)
            if (distance < bestDistance) {
                bestDistance = distance
                best = box.index
            }
        }
        return best.takeIf { it >= 0 }
    }
}
