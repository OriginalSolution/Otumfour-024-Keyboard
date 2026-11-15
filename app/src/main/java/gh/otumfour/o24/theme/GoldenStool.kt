package gh.otumfour.o24.theme

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * The Golden Stool as an Asante stool: a curved crescent seat, a carved central pillar,
 * a rectangular base, and bells hanging from the seat. Not a four-legged chair.
 */
object GoldenStool {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()

    fun draw(
        canvas: Canvas,
        cx: Float,
        top: Float,
        width: Float,
        color: Int,
        alpha: Int = 255,
        glow: Boolean = false,
        crown: Boolean = false,
        swords: Boolean = false,
    ) {
        if (width <= 0f) return
        if (swords) swords(canvas, cx, top, width, color, alpha)
        if (glow) {
            fill.color = color
            fill.alpha = (alpha * 0.22f).toInt().coerceIn(0, 255)
            canvas.drawOval(cx - width * 0.72f, top - width * 0.08f, cx + width * 0.72f, top + width * 0.95f, fill)
            fill.alpha = (alpha * 0.12f).toInt().coerceIn(0, 255)
            canvas.drawOval(cx - width * 0.95f, top - width * 0.2f, cx + width * 0.95f, top + width * 1.15f, fill)
        }
        val seatH = width * 0.26f
        path.reset()
        path.moveTo(cx - width * 0.5f, top + seatH * 0.62f)
        path.quadTo(cx, top - seatH * 0.05f, cx + width * 0.5f, top + seatH * 0.62f)
        path.quadTo(cx, top + seatH * 1.05f, cx - width * 0.5f, top + seatH * 0.62f)
        path.close()
        fill.color = color
        fill.alpha = alpha
        canvas.drawPath(path, fill)
        stroke.color = color
        stroke.alpha = alpha
        stroke.strokeWidth = width * 0.015f
        path.reset()
        path.moveTo(cx - width * 0.36f, top + seatH * 0.58f)
        path.quadTo(cx, top + seatH * 0.22f, cx + width * 0.36f, top + seatH * 0.58f)
        canvas.drawPath(path, stroke)

        val pillarTop = top + seatH * 0.78f
        val pillarW = width * 0.2f
        val pillarH = width * 0.4f
        val pillar = RectF(cx - pillarW / 2f, pillarTop, cx + pillarW / 2f, pillarTop + pillarH)
        canvas.drawRect(pillar, fill)
        stroke.strokeWidth = width * 0.012f
        canvas.drawLine(pillar.left, pillar.top + pillarH * 0.28f, pillar.right, pillar.top + pillarH * 0.28f, stroke)
        canvas.drawLine(pillar.left, pillar.top + pillarH * 0.72f, pillar.right, pillar.top + pillarH * 0.72f, stroke)
        path.reset()
        path.moveTo(cx, pillar.top + pillarH * 0.38f)
        path.lineTo(cx + pillarW * 0.28f, pillar.centerY())
        path.lineTo(cx, pillar.top + pillarH * 0.62f)
        path.lineTo(cx - pillarW * 0.28f, pillar.centerY())
        path.close()
        canvas.drawPath(path, stroke)

        val baseTop = pillar.bottom
        val base = RectF(cx - width * 0.34f, baseTop, cx + width * 0.34f, baseTop + width * 0.09f)
        canvas.drawRect(base, fill)
        val step = RectF(cx - width * 0.42f, base.bottom, cx + width * 0.42f, base.bottom + width * 0.07f)
        canvas.drawRect(step, fill)

        bells(canvas, cx - width * 0.34f, top + seatH * 0.7f, width, color, alpha, left = true)
        bells(canvas, cx + width * 0.34f, top + seatH * 0.7f, width, color, alpha, left = false)

        if (crown) crown(canvas, cx, top - width * 0.02f, width, color, alpha)
        fill.alpha = 255
        stroke.alpha = 255
    }

    private fun bells(canvas: Canvas, x: Float, y: Float, width: Float, color: Int, alpha: Int, left: Boolean) {
        fill.color = color
        fill.alpha = alpha
        stroke.color = color
        stroke.alpha = alpha
        stroke.strokeWidth = width * 0.012f
        val dir = if (left) -1f else 1f
        for (i in 0 until 3) {
            val bx = x + dir * width * (0.02f + i * 0.075f)
            val by = y + width * (0.08f + i * 0.055f)
            canvas.drawLine(x, y, bx, by - width * 0.03f, stroke)
            canvas.drawCircle(bx, by, width * 0.035f, fill)
            canvas.drawLine(bx, by + width * 0.02f, bx, by + width * 0.055f, stroke)
        }
    }

    private fun crown(canvas: Canvas, cx: Float, base: Float, width: Float, color: Int, alpha: Int) {
        fill.color = color
        fill.alpha = alpha
        val band = RectF(cx - width * 0.16f, base - width * 0.08f, cx + width * 0.16f, base - width * 0.03f)
        canvas.drawRect(band, fill)
        path.reset()
        path.moveTo(band.left, band.top)
        path.lineTo(band.left + width * 0.06f, band.top - width * 0.1f)
        path.lineTo(cx, band.top)
        path.lineTo(cx, band.top - width * 0.14f)
        path.lineTo(band.right - width * 0.06f, band.top)
        path.lineTo(band.right, band.top - width * 0.1f)
        path.lineTo(band.right, band.top)
        path.close()
        canvas.drawPath(path, fill)
        canvas.drawCircle(cx, band.top - width * 0.15f, width * 0.02f, fill)
    }

    /** Two swords crossed behind the stool. Blades and hilts only. */
    private fun swords(canvas: Canvas, cx: Float, top: Float, width: Float, color: Int, alpha: Int) {
        stroke.color = color
        stroke.alpha = (alpha * 0.9f).toInt().coerceIn(0, 255)
        stroke.strokeWidth = width * 0.035f
        stroke.strokeCap = Paint.Cap.ROUND
        val y = top + width * 0.28f
        canvas.drawLine(cx - width * 0.62f, y + width * 0.34f, cx + width * 0.62f, y - width * 0.34f, stroke)
        canvas.drawLine(cx - width * 0.62f, y - width * 0.34f, cx + width * 0.62f, y + width * 0.34f, stroke)
        stroke.strokeWidth = width * 0.07f
        canvas.drawLine(cx - width * 0.48f, y + width * 0.22f, cx - width * 0.34f, y + width * 0.12f, stroke)
        canvas.drawLine(cx + width * 0.48f, y + width * 0.22f, cx + width * 0.34f, y + width * 0.12f, stroke)
        fill.color = color
        fill.alpha = alpha
        canvas.drawCircle(cx - width * 0.58f, y + width * 0.32f, width * 0.04f, fill)
        canvas.drawCircle(cx + width * 0.58f, y + width * 0.32f, width * 0.04f, fill)
        stroke.strokeCap = Paint.Cap.BUTT
        stroke.alpha = 255
    }
}
